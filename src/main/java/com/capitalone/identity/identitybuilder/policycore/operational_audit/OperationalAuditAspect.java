package com.capitalone.identity.identitybuilder.policycore.operational_audit;

import com.capitalone.chassis.engine.model.context.RequestContextHolder;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.cyber.AWSHostContextCollector;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.mapper.OperationalEventEnricher;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.mapper.OperationalEventMapper;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.model.OperationalError;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.util.OperationalAuditPublisher;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.util.OperationalAuditUtil;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.logging.log4j.CloseableThreadContext;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import static com.capitalone.identity.identitybuilder.policycore.operational_audit.mapper.OperationalEventMapper.AspectEventFactory;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Aspect which audits an operational event on any method annotated with @OperationalAudit. Recommended for
 * auditing timing metrics, status, and non-CuRE metadata on methods with a deterministic auditable output and single-object input.
 * For methods with complex inputs or a need to audit data internal to the method and not exposed in the response,
 * a custom auditor implementation is recommended instead.
 */
@Log4j2
@AllArgsConstructor
@Aspect
public class OperationalAuditAspect {

    private final OperationalEventEnricher operationalEventEnricher;
    private final OperationalAuditPublisher operationalAuditPublisher;
    private final Map<Class<? extends AspectEventFactory>, AspectEventFactory> factories;
    private final AWSHostContextCollector awsHostContextCollector;

    @Around("@annotation(audit)")
    public Object auditOperationalEvent(final ProceedingJoinPoint proceedingJoinPoint,
                                        final OperationalAudit audit) throws Throwable {

        // Prepare operational details
        Optional<?> response = Optional.empty();
        Optional<OperationalError> throwableEventError = Optional.empty();
        final Long startTimestamp = System.currentTimeMillis();
        final String eventOperationId = UUID.randomUUID().toString();

        // Begin the operation
        try (final CloseableThreadContext.Instance ignored = CloseableThreadContext.put("eventOperationId", eventOperationId)) {
            response = Optional.ofNullable(proceedingJoinPoint.proceed());
            return response.orElse(null);
        } catch (final Throwable throwable) {
            // Populate error info based on any caught throwable
            throwableEventError = Optional.of(OperationalAuditUtil.mergeThrowableInfo(
                    OperationalError.builder().build(),
                    Optional.of(throwable)));
            throw throwable;
        } finally {
            // Audit the outcome of the operation
            doAudit(audit, proceedingJoinPoint,
                    response, eventOperationId, startTimestamp, throwableEventError);
        }
    }

    private void doAudit(final OperationalAudit audit,
                         final JoinPoint joinPoint,
                         final Optional<?> response, final String eventOperationId,
                         final Long startTimestamp, final Optional<OperationalError> throwableError) {
        // Obtain the end timestamp of the operation
        final Long endTimestamp = System.currentTimeMillis();

        // Obtain the event mapper implementation for the current audit
        final OperationalEventMapper.AspectEventFactory operationalEventMapperFactory;
        try {
            final Class<? extends AspectEventFactory> factoryType = audit.mapperFactory();
            final AspectEventFactory factory = factories.get(factoryType);
            operationalEventMapperFactory = factory == null ? audit.mapperFactory().newInstance() : factory;
        } catch (Exception e) {
            log.error(String.format("Could not instantiate mapper factory of type %s. "
                    + "Cancelling audit attempt.", audit.mapperFactory()), e);
            return;
        }
        final OperationalEventMapper operationalEventMapper;
        try {
            operationalEventMapper = operationalEventMapperFactory.create(
                    extractFirstJoinPointArg(joinPoint),
                    response.orElse(null));
        } catch (Exception e) {
            log.error("Failed to instantiate operational event mapper for " + audit.eventType() +
                    ". Cancelling audit attempt.", e);
            return;
        }

        try {
            // Populate common attributes
            final OperationalEventEnricher.CommonAttributes commonAttributes = OperationalEventEnricher.CommonAttributes.builder()
                    .eventType(audit.eventType())
                    .eventOutcome(operationalEventMapper.toOperationalEventOutcome())
                    .errorOptional(throwableError.isPresent() ? throwableError : operationalEventMapper.toOperationalError())
                    .requestContextOptional(Optional.of(OperationalAuditUtil.getChassisRequestContext()))
                    .eventOperationId(eventOperationId)
                    .startTimestamp(startTimestamp)
                    .endTimestamp(endTimestamp)
                    .build();

            final OperationalEventEnricher.CyberAttributes cyberAttributes =
                    awsHostContextCollector.setCyberAttributes(this.getClass().getName(),
                            RequestContextHolder.getRequestContextOrDefault());

            // Build and Publish the Event with entity-specific and common attributes combined
            operationalAuditPublisher.publish(operationalEventEnricher.enrich(
                    operationalEventMapper.buildWithEntitySpecificAttributes(),
                    commonAttributes, cyberAttributes));
        } catch (final RuntimeException e) {
            log.error("Caught exception during operational audit. ", e);
        }
    }

    private Object extractFirstJoinPointArg(final JoinPoint joinPoint) {
        final Object[] args = joinPoint.getArgs();
        if (args.length == 0) {
            return null;
        }
        return args[0];
    }

}
