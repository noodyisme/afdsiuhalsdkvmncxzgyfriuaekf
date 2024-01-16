package com.capitalone.identity.identitybuilder.policycore.operational_audit.mapper;

import com.capitalone.identity.identitybuilder.policycore.camel.external.dynamic.UpdateTransactionResult;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.model.OperationalError;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.model.OperationalEvent;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.model.OperationalEventOccurred;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.model.OperationalEventOutcome;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.model.configstore.ScannerTerminationEventOccurred;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.util.OperationalAuditConstants;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.extern.log4j.Log4j2;

import java.util.Optional;

@AllArgsConstructor
@Log4j2
public class ScannerTerminationEventMapper implements OperationalEventMapper{

    private final @NonNull ScannerTerminationEventOccurred scannerTerminationEventOccurred;
    @Override
    public OperationalEvent buildWithEntitySpecificAttributes() {
        final OperationalEvent.OperationalEventBuilder operationalEventBuilder = OperationalEvent.builder();
        return operationalEventBuilder.build();
    }

    @Override
    public OperationalEventOutcome toOperationalEventOutcome() {
        final UpdateTransactionResult updateTransactionResult = scannerTerminationEventOccurred.getResult();
        if(updateTransactionResult.isSuccess()) {
            return OperationalAuditConstants.OUTCOME_SUCCESS_INFO;
        }
        return OperationalAuditConstants.OUTCOME_FAILURE_ERROR;
    }

    @Override
    public Optional<OperationalError> toOperationalError() {
        final Throwable throwable = scannerTerminationEventOccurred.getResult().getError();
        return Optional.ofNullable(OperationalError.of(throwable));
    }

    public static class Factory implements  OperationalEventMapper.NotificationEventFactory {

        @Override public @NonNull OperationalEventMapper create(
                final OperationalEventOccurred operationalEventOccurred) {
            return new ScannerTerminationEventMapper((ScannerTerminationEventOccurred)operationalEventOccurred);
        }
    }
}
