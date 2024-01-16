package com.capitalone.identity.identitybuilder.policycore.externalization;

import com.capitalone.identity.identitybuilder.ClientEnvironment;
import com.capitalone.identity.identitybuilder.client.ConfigStoreClient;
import com.capitalone.identity.identitybuilder.client.dynamic.DynamicUpdateConfigurationProperties;
import com.capitalone.identity.identitybuilder.model.EntityActivationStatus;
import com.capitalone.identity.identitybuilder.model.EntityInfo;
import com.capitalone.identity.identitybuilder.model.EntityState;
import com.capitalone.identity.identitybuilder.model.EntityType;
import com.capitalone.identity.identitybuilder.policycore.camel.external.dynamic.UpdateTransactionResult;
import com.capitalone.identity.identitybuilder.policycore.camel.external.logging.RuntimeUpdateEventLogger;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.OperationalAuditor;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.mapper.EntityUpdateEventMapper;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.model.configstore.EntityUpdateEventOccurred;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.model.type.OperationalEventType;
import com.capitalone.identity.identitybuilder.polling.PollingConfigurationStreamProvider;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import reactor.core.Disposable;
import reactor.core.Exceptions;
import reactor.core.publisher.Flux;
import reactor.util.retry.Retry;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

import static com.capitalone.identity.identitybuilder.policycore.camel.external.dynamic.DynamicUpdateUtil.findDurationByEnv;

@Service
@ConditionalOnProperty("identitybuilder.policycore.feature.abac-enforcement.enabled")
@Log4j2
public class DynamicEntityManager {

    private final ConfigStoreClient client;
    private final RuntimeUpdateEventLogger eventLogger;
    private final OperationalAuditor operationalAuditor;
    private final PollingConfigurationStreamProvider pollingConfigurationStreamProvider;
    private final boolean strictStartupMode;
    private final EntityUpdateService entityUpdateService;
    private final ClientEnvironment clientEnvironment;
    private boolean isAccessControlLoadComplete = false;
    private boolean isBootstrapInProgress = true;
    private Disposable updateSubscription;
    private Disposable pollingIntervalSubscription;


    @Inject
    public DynamicEntityManager(ConfigStoreClient client, //NOSONAR
                                RuntimeUpdateEventLogger eventLogger,
                                @Value("${identitybuilder.policycore.feature.strict-policy-start-mode:false}")
                                boolean strictStartupMode,
                                OperationalAuditor operationalAuditor,
                                DynamicUpdateConfigurationProperties updateProperties,
                                EntityUpdateService entityUpdateService,
                                ClientEnvironment clientEnvironment) {
        this.client = client;
        this.eventLogger = eventLogger;
        this.operationalAuditor = operationalAuditor;
        this.strictStartupMode = strictStartupMode;
        this.pollingConfigurationStreamProvider = () -> Flux.just(Objects.requireNonNull(updateProperties));
        this.entityUpdateService = entityUpdateService;
        this.clientEnvironment = clientEnvironment;
    }

    public boolean isAccessControlLoadComplete() {
        return isAccessControlLoadComplete;
    }

    @PostConstruct
    protected void doStart() {

        loadExternalEntities();
        isBootstrapInProgress = false;

        // updates the retry interval when a new value is emitted from configuration stream
        pollingIntervalSubscription = pollingConfigurationStreamProvider.getPollingConfigurationStream()
                .filter(properties -> Objects.nonNull(properties.getInterval()))
                .map(properties1 -> Objects.requireNonNull(properties1.getInterval()))
                .subscribe();

        // observes the stream of entity changes from config store
        updateSubscription = Flux.defer(() ->
                        client.getEntityUpdatesBatch(entityUpdateService.getCurrentLoadedEntities(), EntityType.ACCESS))
                .map(this::tryLoadBatchedEntities)
                .doOnError(error -> eventLogger.auditTermination(ExceptionUtils.getStackTrace(error)))
                .retryWhen(Retry.backoff(10000, Duration.ofSeconds(1)).maxBackoff(findDurationByEnv(clientEnvironment)).jitter(0.4).transientErrors(true))
                .doOnTerminate(() -> eventLogger.auditTermination(null))
                .subscribe();

        isAccessControlLoadComplete = true;

    }

    @PreDestroy
    protected void doStop() {
        Optional.ofNullable(pollingIntervalSubscription).ifPresent(Disposable::dispose);
        Optional.ofNullable(updateSubscription).ifPresent(Disposable::dispose);
    }

    private void loadExternalEntities() {
        // loads externalized access control definitions into abac registry
        List<LinkedHashMap<EntityInfo, UpdateTransactionResult>> results = client.getEntityInfo(EntityType.ACCESS)
                .map(EntityState.Delta::add)
                .map(this::tryLoadEntity)
                .collectList()
                .block();

        // collects all errors that happened during loading
        final int loadErrorCount = getLoadErrorCount(results);

        if (strictStartupMode && loadErrorCount > 0) {
            throw new IllegalStateException(String.format(
                    "Failed to load %s entities from Configuration Store during startup. See logs for details.",
                    loadErrorCount));
        }
    }

    private int getLoadErrorCount(List<LinkedHashMap<EntityInfo, UpdateTransactionResult>> results) {
        return Objects.requireNonNull(results).stream()
                .map(entryMap -> entryMap.values().stream().filter(UpdateTransactionResult::isError)
                        .map(UpdateTransactionResult::getError).filter(Objects::nonNull)
                        .reduce(Exceptions::addSuppressed))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toSet())
                .size();
    }

    final List<LinkedHashMap<EntityInfo, UpdateTransactionResult>> tryLoadBatchedEntities(
            List<EntityState.Delta<EntityInfo>> deltas) {
        final List<LinkedHashMap<EntityInfo, UpdateTransactionResult>> results = new ArrayList<>();
        for (EntityState.Delta<EntityInfo> delta : deltas) {
            LinkedHashMap<EntityInfo, UpdateTransactionResult> result = tryLoadEntity(delta);
            results.add(result);

        }
        int errors = getLoadErrorCount(results);
        if (errors > 0) {
            throw new IllegalStateException(
                    String.format("%s error(s) encountered during batch loading process. see logs for details",
                            errors));
        } else {
            return results;
        }
    }

    private LinkedHashMap<EntityInfo, UpdateTransactionResult> tryLoadEntity(
            EntityState.Delta<EntityInfo> delta) {

        final EntityState.Delta.ChangeType changeType = delta.getType();
        final LinkedHashMap<EntityInfo, UpdateTransactionResult> results = new LinkedHashMap<>();

        // Load the entity - re-attempting with prior version(s) on failure
        EntityInfo info = delta.getEntityInfo();
        while (info != null) {
            final Long startTimestamp = System.currentTimeMillis();
            final UpdateTransactionResult updateTransactionResult = entityUpdateService.applyEntityUpdate(info, changeType);
            if (updateTransactionResult.isSuccess()) {
                eventLogger.auditUpdate(changeType, info, EntityActivationStatus.ACTIVE);
            } else {
                eventLogger.auditLoadOperationFailed(changeType, info, updateTransactionResult.getError());
            }
            operationalAuditor.audit(EntityUpdateEventOccurred.builder()
                    .operationalEventType(OperationalEventType.fromChangeType(changeType, isBootstrapInProgress))
                    .changeType(changeType)
                    .info(info)
                    .result(updateTransactionResult)
                    .startTimestamp(startTimestamp)
                    .endTimestamp(System.currentTimeMillis())
                    .build(),
                  EntityUpdateEventMapper.Factory.class);
            results.put(info, updateTransactionResult);
            if (updateTransactionResult.isSuccess()) {
                return results;
            } else {
                log.error("Error loading this version of access entity. Looking for prior " +
                        "version to load.", updateTransactionResult.getError());
                info = info.getPriorVersion();
            }
        }
        return results;
    }

}
