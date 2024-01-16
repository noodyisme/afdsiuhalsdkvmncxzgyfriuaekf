package com.capitalone.identity.identitybuilder.policycore.operational_audit.model.event_models;

import com.capitalone.identity.identitybuilder.events.ApplicationEvent;
import com.capitalone.identity.identitybuilder.events.PolicyCoreEvent;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.model.OperationalEventOccurred;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.model.type.OperationalEventType;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.Value;

@PolicyCoreEvent
@Value
@EqualsAndHashCode(callSuper = true)
public class HealthCheckEvaluated extends ApplicationEvent implements OperationalEventOccurred {

    Integer status;

    @NonNull
    @Override
    public OperationalEventType getOperationalEventType() {
        return OperationalEventType.HEALTH_CHECK_EVALUATED;
    }

}
