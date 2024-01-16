package com.capitalone.identity.identitybuilder.policycore.operational_audit.model.event_models;

import com.capitalone.identity.identitybuilder.events.ApplicationEvent;
import com.capitalone.identity.identitybuilder.events.PolicyCoreEvent;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.model.OperationalEventOccurred;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.model.type.OperationalEventType;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.Value;

import java.util.HashMap;
import java.util.Optional;
import java.util.UUID;

@PolicyCoreEvent
@Value
@EqualsAndHashCode(callSuper = true)
public class ApplicationBootstrapComplete extends ApplicationEvent implements OperationalEventOccurred {

    ApplicationEvent.Metadata eventMetadata;
    Long applicationAvailableTime;
    HashMap<String, String> auditEventDurations;
    UUID vmIdentifier;

    @Override
    public @NonNull OperationalEventType getOperationalEventType() {
        return OperationalEventType.SERVICE_STARTUP_FINISHED;
    }

    @Override
    public @NonNull Long getStartTimestamp() {
        return eventMetadata.getStart();
    }

    @Override
    public @NonNull Long getEndTimestamp() {
        return eventMetadata.getEnd();
    }

    @Override
    public @NonNull Optional<UUID> getLoggerContextEventIdOptional() {
        return Optional.of(vmIdentifier);
    }
}
