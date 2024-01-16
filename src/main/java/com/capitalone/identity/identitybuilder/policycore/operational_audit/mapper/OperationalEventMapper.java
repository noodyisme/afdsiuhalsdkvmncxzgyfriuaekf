package com.capitalone.identity.identitybuilder.policycore.operational_audit.mapper;

import com.capitalone.identity.identitybuilder.policycore.operational_audit.model.*;
import lombok.NonNull;

import java.util.Optional;

/**
 * An interface which formalizes the conversion logic specific to each distinct entity_action operation (e.g.
 * POLICY_EVALUATED) that is necessary for mapping the entity/action-specific request and response data into
 * the appropriate operational event fields for auditing.
 */
public interface OperationalEventMapper {

    /**
     * Populates an operational event with data that varies based on event type
     *
     * @return the event with pertinent data pre-populated
     */
    OperationalEvent buildWithEntitySpecificAttributes();

    /**
     * Populates the outcome data of the event
     *
     * @return the event outcome data
     */
    OperationalEventOutcome toOperationalEventOutcome();

    /**
     * Populates error info from an event output assumed to have a non-successful status also leveraging any
     * exception if available. If the result object does not contain any error information and the exception
     * is empty, this should return empty.
     *
     * @return the populated error info or empty if none present
     */
    Optional<OperationalError> toOperationalError();

    /**
     * OperationalEventMapper Factory interface leveraged for Aspect Audits
     */
    interface AspectEventFactory {
        @NonNull OperationalEventMapper create(final Object request, final Object response);
    }

    /**
     * OperationalEventMapper Factory interface leveraged for Notification Audits
     */
    interface NotificationEventFactory {
        @NonNull OperationalEventMapper create(final OperationalEventOccurred operationalEventOccurred);
    }
}
