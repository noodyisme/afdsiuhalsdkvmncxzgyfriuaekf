package com.capitalone.identity.identitybuilder.policycore.operational_audit.model;

import com.capitalone.identity.identitybuilder.policycore.operational_audit.model.type.OperationalEventType;
import lombok.NonNull;

import java.util.Optional;
import java.util.UUID;

public interface OperationalEventOccurred {

    /**
     * The type of operational event - e.g. POLICY_EVALUATED
     * @return the type of operational event
     */
    @NonNull OperationalEventType getOperationalEventType();

    /**
     * The start time in ms of the operation that occurred if this value is known. Leveraged to calculate duration of timed event.
     * Defaults to endTimeStamp value if unspecified by concrete class.
     * @return the start time in ms
     */
    @NonNull default Long getStartTimestamp() {
        return getEndTimestamp();
    }

    /**
     * The end time in ms of the operation that occurred (time at which the event completed). Leveraged to calculate
     * duration of timed event if start time is also known.
     * @return the end time in ms
     */
    @NonNull default Long getEndTimestamp() {
        return System.currentTimeMillis();
    }

    /***
     * The unique id assigned to this event - ideally matching an id that can be correlated to logs to obtain further details
     * @return the optional unique id
     */
    @NonNull default Optional<UUID> getLoggerContextEventIdOptional() {
        return Optional.empty();
    }
}
