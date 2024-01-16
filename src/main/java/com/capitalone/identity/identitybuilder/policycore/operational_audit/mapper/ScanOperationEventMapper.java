package com.capitalone.identity.identitybuilder.policycore.operational_audit.mapper;

import com.capitalone.identity.identitybuilder.model.ScanRequest;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.model.OperationalError;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.model.OperationalEvent;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.model.OperationalEventOccurred;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.model.OperationalEventOutcome;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.model.configstore.OperationalConfigStoreMetadata;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.model.configstore.ScanOperationEvent;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.util.OperationalAuditConstants;
import lombok.AllArgsConstructor;
import lombok.NonNull;

import java.util.Optional;

@AllArgsConstructor
public class ScanOperationEventMapper implements OperationalEventMapper{

    private ScanOperationEvent scanOperationEvent;

    @Override
    public OperationalEvent buildWithEntitySpecificAttributes() {

        // Initialize builders
        final OperationalEvent.OperationalEventBuilder operationalEventBuilder = OperationalEvent.builder();
        final OperationalConfigStoreMetadata.OperationalConfigStoreMetadataBuilder configStoreMetadataBuilder =
                OperationalConfigStoreMetadata.builder();

        final ScanRequest scanRequest =
                scanOperationEvent.getScanRequest();
        final  ScanRequest.ScanType scanType = scanRequest.getScanType();
        final Long scanScheduled = scanRequest.getStartScheduled();

        configStoreMetadataBuilder.scanScheduled(scanScheduled.toString());
        configStoreMetadataBuilder.scanType(scanType.name());

        return operationalEventBuilder
                .configStoreMetadata(configStoreMetadataBuilder.build())
                .build();
    }

    @Override
    public OperationalEventOutcome toOperationalEventOutcome() {
        return OperationalAuditConstants.OUTCOME_SUCCESS_INFO;
    }

    @Override
    public Optional<OperationalError> toOperationalError() {
        return Optional.empty();
    }

    public static class Factory implements OperationalEventMapper.NotificationEventFactory{
        @Override public @NonNull OperationalEventMapper create(
                final OperationalEventOccurred operationalEventOccurred) {
            return new ScanOperationEventMapper((ScanOperationEvent) operationalEventOccurred);
        }
    }
}
