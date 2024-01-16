package com.capitalone.identity.identitybuilder.policycore.operational_audit.model.configstore;

import com.capitalone.identity.identitybuilder.model.DynamicUpdateProperties;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.model.OperationalEventOccurred;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.model.type.OperationalEventType;
import com.capitalone.identity.identitybuilder.polling.PollingConfigurationApplied;
import com.capitalone.identity.identitybuilder.polling.PollingConfigurationErrorOccurred;
import lombok.Value;

@Value
public class PollingConfigEvent implements OperationalEventOccurred {

    OperationalEventType operationalEventType = OperationalEventType.CONFIGSTORE_POLLING_CONFIG_UPDATED;
    Long endTimeStamp;
    DynamicUpdateProperties dynamicUpdateProperties;
    Throwable throwable;

    public static PollingConfigEvent of(PollingConfigurationApplied pollingConfigurationApplied) {
        return new PollingConfigEvent(
                pollingConfigurationApplied.getMetadata().getTimestamp(),
                pollingConfigurationApplied.getConfiguration(),
                null);
    }

    public static PollingConfigEvent of(PollingConfigurationErrorOccurred pollingConfigurationErrorOccurred) {
        return new PollingConfigEvent(
                pollingConfigurationErrorOccurred.getMetadata().getTimestamp(),
                pollingConfigurationErrorOccurred.getConfiguration(),
                pollingConfigurationErrorOccurred.getError());
    }
}
