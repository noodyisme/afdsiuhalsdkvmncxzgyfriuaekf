package com.capitalone.identity.identitybuilder.policycore.operational_audit;

import com.capitalone.chassis.engine.model.context.RequestContextHolder;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.cyber.AWSHostContextCollector;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.mapper.OperationalEventEnricher;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.mapper.OperationalEventMapper;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.model.OperationalEventOccurred;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.model.type.OperationalEventType;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.util.OperationalAuditPublisher;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.util.OperationalAuditUtil;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Log4j2
@AllArgsConstructor
@Component
public class OperationalAuditor {

    private final @Nullable OperationalAuditDispatcher operationalAuditDispatcher;

    public void audit(final OperationalEventOccurred event, final Class<? extends OperationalEventMapper.NotificationEventFactory> mapperFactoryType) {
        if (operationalAuditDispatcher == null) {
            return;
        }
        try {
            operationalAuditDispatcher.doAudit(event, mapperFactoryType);
        } catch (final RuntimeException e) {
            log.error("Caught runtime exception during operational audit.", e);
        }
    }

    @AllArgsConstructor
    public static class OperationalAuditDispatcher {

        private final OperationalEventEnricher operationalEventEnricher;
        private final OperationalAuditPublisher operationalAuditPublisher;
        private final AWSHostContextCollector awsHostContextCollector;

        void doAudit(final OperationalEventOccurred operationalEventOccurred,
                     final Class<? extends OperationalEventMapper.NotificationEventFactory> mapperFactoryType) {

            // Obtain mapper
            final OperationalEventType operationalEventType = Objects.requireNonNull(operationalEventOccurred.getOperationalEventType());
            final OperationalEventMapper.NotificationEventFactory operationalEventMapperFactory;
            try {
                operationalEventMapperFactory = mapperFactoryType.newInstance();
            } catch (final Exception e) {
                log.error(String.format("Could not instantiate mapper factory of type %s. "
                        + "Cancelling audit attempt.", mapperFactoryType), e);
                return;
            }
            final OperationalEventMapper operationalEventMapper;
            try {
                operationalEventMapper = operationalEventMapperFactory.create(operationalEventOccurred);
            } catch (Exception e) {
                log.error("Failed to instantiate operational event mapper for " + mapperFactoryType +
                        ". Cancelling audit attempt.", e);
                return;
            }
            // Populate common attributes
            final OperationalEventEnricher.CommonAttributes commonAttributes = OperationalEventEnricher.CommonAttributes.builder()
                    .eventType(operationalEventType)
                    .eventOutcome(operationalEventMapper.toOperationalEventOutcome())
                    .errorOptional(operationalEventMapper.toOperationalError())
                    .requestContextOptional(Optional.of(OperationalAuditUtil.getChassisRequestContext()))
                    .eventOperationId(operationalEventOccurred
                            .getLoggerContextEventIdOptional()
                            .map(UUID::toString).orElse(null))
                    .startTimestamp(operationalEventOccurred.getStartTimestamp())
                    .endTimestamp(operationalEventOccurred.getEndTimestamp())
                    .build();
            final OperationalEventEnricher.CyberAttributes cyberAttributes =
                    awsHostContextCollector.setCyberAttributes(this.getClass().getName(),
                            RequestContextHolder.getRequestContextOrDefault());

            // Build and Publish the Event with entity-specific and common attributes combined
            operationalAuditPublisher.publish(operationalEventEnricher.enrich(
                    operationalEventMapper.buildWithEntitySpecificAttributes(),
                    commonAttributes, cyberAttributes));

        }
    }


}
