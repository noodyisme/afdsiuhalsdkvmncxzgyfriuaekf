package com.capitalone.identity.identitybuilder.policycore.operational_audit.model.configstore;

import com.capitalone.identity.identitybuilder.model.ConfigStoreScanCompleted;
import com.capitalone.identity.identitybuilder.model.ScanRequest;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.model.OperationalEventOccurred;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.model.type.OperationalEventType;
import lombok.Value;

@Value
public class ScanOperationEvent implements OperationalEventOccurred {

    OperationalEventType operationalEventType = OperationalEventType.SCAN_COMPLETED;
    Long endTimeStamp;
    ScanRequest scanRequest;

    public static ScanOperationEvent of(ConfigStoreScanCompleted configStoreScanCompleted) {
        return new ScanOperationEvent(
                configStoreScanCompleted.getEndActual(),
                configStoreScanCompleted.getRequest()
        );
    }
}
