package com.capitalone.identity.identitybuilder.policycore.externalization.logging;

import com.capitalone.identity.identitybuilder.model.Entity;
import com.capitalone.identity.identitybuilder.model.EntityActivationStatus;
import com.capitalone.identity.identitybuilder.policycore.camel.external.dynamic.UpdateTransactionResult;
import com.capitalone.identity.identitybuilder.policycore.camel.external.logging.RuntimeUpdateEventLogger;
import com.capitalone.identity.identitybuilder.policycore.externalization.events.EntityLoadEventVisitor;
import com.capitalone.identity.identitybuilder.policycore.externalization.events.EntityLoadEvents;
import com.capitalone.identity.identitybuilder.policycore.externalization.events.EntityLoadResult;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.OperationalAuditor;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.mapper.EntityUpdateEventMapper;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.mapper.ScannerTerminationEventMapper;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.model.configstore.EntityUpdateEventOccurred;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.model.configstore.ScannerTerminationEventOccurred;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.model.type.OperationalEventType;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@ConditionalOnBean(value = {RuntimeUpdateEventLogger.class, OperationalAuditor.class})
public class DynamicLoadingEventLogger implements EntityLoadEventVisitor {
    private static final Logger logger = LogManager.getLogger(DynamicLoadingEventLogger.class.getName());
    private final RuntimeUpdateEventLogger eventLogger;
    private final OperationalAuditor auditor;

    public DynamicLoadingEventLogger(RuntimeUpdateEventLogger eventLogger, OperationalAuditor auditor) {
        this.eventLogger = eventLogger;
        this.auditor = auditor;
    }

    @Override
    public void onEntityLoaded(EntityLoadEvents.Loaded event) {
        doWithCatching(event, () -> {
            EntityActivationStatus activationStatus = event.getEntity() instanceof Entity.Policy
                    ? ((Entity.Policy) event.getEntity()).getEntityActivationStatus()
                    : EntityActivationStatus.ACTIVE;

            eventLogger.auditUpdate(event.getChangeType(), event.getInfo(),
                    activationStatus);
            auditor.audit(EntityUpdateEventOccurred.builder()
                    .operationalEventType(OperationalEventType.fromLoadOperationResult(event))
                    .changeType(event.getChangeType())
                    .info(event.getInfo())
                    .result(UpdateTransactionResult.success())
                    .startTimestamp(event.getStartTime())
                    .endTimestamp(event.getEndTime())
                    .activationStatus(activationStatus)
                    .build(),
                EntityUpdateEventMapper.Factory.class);
        });
    }

    @Override
    public void onEntityUnloaded(EntityLoadEvents.Unloaded event) {
        doWithCatching(event, () -> {
            eventLogger.auditUnload(event.getInfo());
            auditor.audit(EntityUpdateEventOccurred.builder()
                    .operationalEventType(OperationalEventType.fromLoadOperationResult(event))
                    .changeType(event.getChangeType())
                    .info(event.getInfo())
                    .result(UpdateTransactionResult.success())
                    .startTimestamp(event.getStartTime())
                    .endTimestamp(event.getEndTime())
                    .build(),
                EntityUpdateEventMapper.Factory.class);
        });
    }

    @Override
    public void onEntityLoadFailed(EntityLoadEvents.Failed event) {
        doWithCatching(event, () -> {
            eventLogger.auditLoadOperationFailed(event.getChangeType(), event.getInfo(), event.getError());
            auditor.audit(EntityUpdateEventOccurred.builder()
                    .operationalEventType(OperationalEventType.fromLoadOperationResult(event))
                    .changeType(event.getChangeType())
                    .info(event.getInfo())
                    .result(UpdateTransactionResult.error(event.getError()))
                    .startTimestamp(event.getStartTime())
                    .endTimestamp(event.getEndTime())
                    .build(),
                EntityUpdateEventMapper.Factory.class);
        });
    }

    @Override
    public void onNonEntityError(EntityLoadEvents.NonLoadingError event) {
        doWithCatching(event, () -> {
            eventLogger.auditTermination(Optional.ofNullable(event.getError()).map(ExceptionUtils::getStackTrace).orElse(null));
            auditor.audit(ScannerTerminationEventOccurred.builder()
                            .operationalEventType(OperationalEventType.SCANNER_TERMINATED)
                            .result(UpdateTransactionResult.error(event.getError()))
                            .build(),
                    ScannerTerminationEventMapper.Factory.class);
        });
    }

    private void doWithCatching(final EntityLoadResult event, final Runnable runnable) {
        try {
            runnable.run();
        } catch (final Exception e) {
            logger.error(String.format("Caught exception while processing EntityLoadResult, %s", event), e);
        }
    }
}
