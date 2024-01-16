package com.capitalone.identity.identitybuilder.policycore.operational_audit.model.event_models;

import com.capitalone.identity.identitybuilder.events.ApplicationEvent;
import com.capitalone.identity.identitybuilder.events.PolicyCoreEvent;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.model.OperationalEventOccurred;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.model.type.OperationalEventType;
import com.capitalone.identity.identitybuilder.policycore.service.jws.JWTExtractionException;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.Value;

import javax.annotation.Nullable;


@PolicyCoreEvent
@Value
@EqualsAndHashCode(callSuper = true)
public class JwkValidated extends ApplicationEvent implements OperationalEventOccurred {

    @NonNull
    String eventMessage;

    @Nullable
    JWTExtractionException.JwsExceptionType jwsExceptionType;

    @NonNull
    Boolean applicationLevelProductIdValidation;

    @NonNull
    Boolean policyLevelProductIdValidation;

    @Nullable
    String jwkProductId;

    @Nullable
    String policyExpectedProductId;

    @NonNull
    @Override
    public OperationalEventType getOperationalEventType() {
        return OperationalEventType.JWT_EVALUATED;
    }
}
