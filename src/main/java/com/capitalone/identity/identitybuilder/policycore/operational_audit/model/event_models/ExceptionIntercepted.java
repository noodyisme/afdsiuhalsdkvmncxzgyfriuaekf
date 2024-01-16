package com.capitalone.identity.identitybuilder.policycore.operational_audit.model.event_models;

import com.capitalone.identity.identitybuilder.events.ApplicationEvent;
import com.capitalone.identity.identitybuilder.events.PolicyCoreEvent;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.model.OperationalEventOccurred;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.model.type.OperationalEventType;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.Value;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.UUID;

@PolicyCoreEvent
@Value
@EqualsAndHashCode(callSuper = true)
public class ExceptionIntercepted extends ApplicationEvent implements OperationalEventOccurred {
    String methodSource;
    String callingClass;
    String args;
    Throwable throwable;
    Integer httpStatus;
    UUID identifier;

    @Override
    public @NonNull OperationalEventType getOperationalEventType() {
        return OperationalEventType.EXTERNAL_EXCEPTION_ENCOUNTERED;
    }

    @NotNull
    @Override
    public Optional<UUID> getLoggerContextEventIdOptional() {
        return Optional.of(identifier);
    }

}
