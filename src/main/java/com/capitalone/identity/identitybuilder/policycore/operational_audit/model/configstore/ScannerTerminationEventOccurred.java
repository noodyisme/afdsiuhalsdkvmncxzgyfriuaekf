package com.capitalone.identity.identitybuilder.policycore.operational_audit.model.configstore;

import com.capitalone.identity.identitybuilder.policycore.camel.external.dynamic.UpdateTransactionResult;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.model.OperationalEventOccurred;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.model.type.OperationalEventType;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ScannerTerminationEventOccurred implements OperationalEventOccurred {

    OperationalEventType operationalEventType;
    UpdateTransactionResult result;
}
