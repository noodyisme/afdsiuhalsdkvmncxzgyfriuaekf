package com.capitalone.identity.identitybuilder.policycore.camel.external.dynamic;

import com.capitalone.identity.identitybuilder.ClientEnvironment;
import com.capitalone.identity.identitybuilder.client.ConfigStoreClient;
import com.capitalone.identity.identitybuilder.model.Entity;
import com.capitalone.identity.identitybuilder.model.EntityInfo;
import com.capitalone.identity.identitybuilder.model.EntityState;
import com.capitalone.identity.identitybuilder.model.EntityType;
import com.capitalone.identity.identitybuilder.policycore.externalization.events.EntityLoadEventVisitor;
import com.capitalone.identity.identitybuilder.policycore.externalization.events.EntityLoadEvents;
import com.capitalone.identity.identitybuilder.policycore.externalization.events.EntityLoadOperationResult;
import com.capitalone.identity.identitybuilder.policycore.service.logging.EnrichBootstrapAuditEvent;
import com.newrelic.api.agent.Trace;
import org.apache.camel.CamelContext;
import org.apache.camel.Service;
import org.apache.camel.StaticService;
import org.apache.camel.spring.boot.CamelContextConfiguration;
import org.apache.camel.support.service.ServiceSupport;
import org.apache.commons.lang3.time.FastDateFormat;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.util.retry.Retry;

import javax.inject.Inject;
import java.io.IOError;
import java.time.Duration;
import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static com.capitalone.identity.identitybuilder.policycore.camel.external.dynamic.DynamicUpdateUtil.findDurationByEnv;

/**
 * Coordinates lifecycle, applies policy updates, and responds to errors in {@link DynamicPolicyLoader}. Also,
 * responsible for audit logging of dynamic updates.
 */
@Component
public class DynamicPolicyManager extends ServiceSupport implements Service, StaticService, CamelContextConfiguration {
    private static final FastDateFormat SIMPLE_LOGGING_DATE_FORMAT = FastDateFormat.getInstance("MM/dd/yyyy HH:mm:ss.SSS", TimeZone.getTimeZone("UTC"));
    private static final Logger logger = LogManager.getLogger(DynamicPolicyManager.class.getName());
    private final ConfigStoreClient client;
    private final DynamicPolicyUpdater policyUpdater;
    private final PolicyValidator policyValidator;
    private final EntityLoadEventVisitor dynamicLoadingEventLogger;
    private final boolean strictStartupMode;
    private final ClientEnvironment clientEnvironment;
    private Disposable deltaSubscription;
    private boolean isBootstrapInProgress = true;
    private boolean isDynamicPolicyLoadComplete = false;

    @Inject
    public DynamicPolicyManager(ConfigStoreClient client,
                                DynamicPolicyUpdater updater,
                                PolicyValidator validator,
                                @Value("${identitybuilder.policycore.feature.strict-policy-start-mode:false}")
                                boolean strictStartupMode,
                                ClientEnvironment clientEnvironment,
                                @Autowired(required = false) EntityLoadEventVisitor dynamicLoadingEventLogger) {
        this.client = client;
        this.policyUpdater = updater;
        this.policyValidator = validator;
        this.strictStartupMode = strictStartupMode;
        this.clientEnvironment = clientEnvironment;
        this.dynamicLoadingEventLogger = Optional.ofNullable(dynamicLoadingEventLogger)
                .orElse(EntityLoadEventVisitor.EMPTY);
    }

    @Override
    @Trace
    public void beforeApplicationStart(CamelContext camelContext) {
        long start = System.currentTimeMillis();
        try {
            camelContext.addService(this, true, true);
        } catch (Exception e) {
            throw new IOError(e);
        }

        policyValidator.populateInternalModel(camelContext);

        long end = System.currentTimeMillis();
        long elapsed = end - start;
        String startDate = SIMPLE_LOGGING_DATE_FORMAT.format(new java.util.Date(start));
        String endDate = SIMPLE_LOGGING_DATE_FORMAT.format(new java.util.Date(end));
        logger.info("Before Application Start. Start time: {} End time: {} Elapsed Time: {} ms", startDate, endDate, elapsed);
    }

    @Override
    protected void doStart() {
        // noop
    }

    /**
     * Subscribe to policy updates.
     */
    @Override
    @Trace
    @EnrichBootstrapAuditEvent
    public void afterApplicationStart(CamelContext camelContext) {
        long start = System.currentTimeMillis();

        List<EntityLoadOperationResult> results = client.getEntityInfo(EntityType.PIP, EntityType.POLICY)
                .toStream()
                .map(info -> tryLoadEntities(EntityState.Delta.ChangeType.ADD, info))
                .flatMap(List::stream)
                .collect(Collectors.toList());

        AggregatedLoadError loadError = processLoadResults(results);

        if (strictStartupMode && loadError != null) {
            throw loadError;
        }

        isBootstrapInProgress = false;

        // listen and apply changes to externalized policies
        deltaSubscription = Flux.defer(() -> client.getEntityUpdatesBatch(
                        policyUpdater.getLoadedEntities().stream()
                                .sorted(Comparator.comparing(EntityInfo::getLocationPrefix))
                                .collect(Collectors.toList()),
                        EntityType.PIP, EntityType.POLICY))
                .map(updates -> {
                    List<AggregatedLoadError> errors = new ArrayList<>();
                    for (EntityState.Delta<EntityInfo> update : updates) {
                        List<EntityLoadOperationResult> updateResults = applyUpdate(update);
                        AggregatedLoadError updateError = processLoadResults(updateResults);
                        if (updateError != null) {
                            errors.add(updateError);
                        }
                    }
                    if (!errors.isEmpty()) {
                        throw new AggregatedLoadError(
                                String.format("Error in Batch Load with following messages: [%s]",
                                        errors.stream()
                                                .map(Throwable::getMessage)
                                                .collect(Collectors.joining(", "))));
                    } else {
                        return updates;
                    }
                })
                .doOnError(
                        err -> !(err instanceof AggregatedLoadError),
                        error -> new EntityLoadEvents.NonLoadingError(error).visit(dynamicLoadingEventLogger)
                )
                .retryWhen(
                        Retry.backoff(10000, Duration.ofSeconds(1)).maxBackoff(findDurationByEnv(clientEnvironment)).jitter(0.4).transientErrors(true)
                )
                .doOnTerminate(() -> new EntityLoadEvents.NonLoadingError(null).visit(dynamicLoadingEventLogger))
                .subscribe();

        isDynamicPolicyLoadComplete = true;
        long end = System.currentTimeMillis();
        long elapsed = end - start;
        String startDate = SIMPLE_LOGGING_DATE_FORMAT.format(new java.util.Date(start));
        String endDate = SIMPLE_LOGGING_DATE_FORMAT.format(new java.util.Date(end));
        logger.info("After Application Start. Start time: {} End time: {} Elapsed Time: {} ms", startDate, endDate, elapsed);
    }

    private AggregatedLoadError processLoadResults(List<EntityLoadOperationResult> results) {
        results.forEach(result -> result.visitCatching(dynamicLoadingEventLogger, logger));
        Map<String, Throwable> loadErrorMap = results.stream()
                .filter(entityLoadOperationResult -> entityLoadOperationResult instanceof EntityLoadEvents.Failed)
                .collect(Collectors.toMap(entityLoadOperationResult -> entityLoadOperationResult.getInfo().getLocationPrefix(),
                        entityLoadOperationResult -> ((EntityLoadEvents.Failed) entityLoadOperationResult).getError()));

        if (loadErrorMap.isEmpty()) {
            return null;
        } else {
            String ids = String.join(",", loadErrorMap.keySet());
            AggregatedLoadError err = new AggregatedLoadError("Entity Locations: " + ids);
            loadErrorMap.values().forEach(err::addSuppressed);
            return err;
        }
    }

    private List<EntityLoadOperationResult> applyUpdate(EntityState.Delta<EntityInfo> delta) {
        switch (delta.getType()) {
            case ADD:
            case UPDATE:
                return tryLoadEntities(delta.getType(), delta.getEntityInfo());
            case DELETE:
                return Collections.singletonList(tryDeleteEntity(delta.getEntityInfo()));
            default:
                throw new UnsupportedOperationException(delta.getType().toString());
        }
    }

    private List<EntityLoadOperationResult> tryLoadEntities(EntityState.Delta.ChangeType changeType, EntityInfo info) {
        EntityInfo publishedEntityInfo = info;
        List<EntityLoadOperationResult> loadResultList = new ArrayList<>();
        int entityLoadRetries = 6;
        while (entityLoadRetries >= 0 && publishedEntityInfo != null) {
            EntityLoadOperationResult entityLoadOperationResult = tryLoadEntity(changeType, publishedEntityInfo, client::getEntity);
            loadResultList.add(entityLoadOperationResult);
            if (!(entityLoadOperationResult instanceof EntityLoadEvents.Failed))
                return loadResultList;
            else {
                publishedEntityInfo = publishedEntityInfo.getPriorVersion();
                entityLoadRetries--;
            }
        }
        return loadResultList;
    }

    private EntityLoadOperationResult tryLoadEntity(EntityState.Delta.ChangeType changeType, EntityInfo entityInfo,
                                                    Function<EntityInfo, Entity> factory) {
        final Long startTime = System.currentTimeMillis();
        return tryLoadOperation(entityInfo, changeType, () -> {
            final Entity entity = factory.apply(entityInfo);
            policyUpdater.set(entity);
            return new EntityLoadEvents.Loaded(entity, changeType, startTime, System.currentTimeMillis(),
                    isBootstrapInProgress);
        });
    }

    private EntityLoadOperationResult tryDeleteEntity(EntityInfo entityInfo) {
        final Long startTime = System.currentTimeMillis();
        return tryLoadOperation(entityInfo, EntityState.Delta.ChangeType.DELETE, () -> {
            policyUpdater.delete(entityInfo);
            return new EntityLoadEvents.Unloaded(entityInfo, startTime, System.currentTimeMillis());
        });
    }

    private EntityLoadOperationResult tryLoadOperation(
            EntityInfo info,
            EntityState.Delta.ChangeType type,
            Supplier<EntityLoadOperationResult> loader) {
        final Long start = System.currentTimeMillis();
        try {
            return loader.get();
        } catch (Exception error) {
            return new EntityLoadEvents.Failed(info, type, error, start, System.currentTimeMillis(),
                    isBootstrapInProgress);
        }
    }

    public boolean isExternalLoadComplete() {
        return isDynamicPolicyLoadComplete;
    }

    @Override
    protected void doStop() {
        logger.info("DO STOP");
        if (deltaSubscription != null) {
            deltaSubscription.dispose();
        }
    }

    private static class AggregatedLoadError extends IllegalStateException {
        AggregatedLoadError(String message) {
            super(message);
        }
    }
}
