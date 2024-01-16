package com.capitalone.identity.identitybuilder.policycore.operational_audit.model.type;

import com.capitalone.identity.identitybuilder.model.EntityState;
import com.capitalone.identity.identitybuilder.policycore.externalization.events.EntityLoadOperationResult;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;

@AllArgsConstructor
@Getter
public enum OperationalEventType {
    POLICY_EVALUATED(OperationalEventSourceObject.POLICY, OperationalEventAction.EVALUATED),
    PIP_EVALUATED(OperationalEventSourceObject.PIP, OperationalEventAction.EVALUATED),
    DECISION_EVALUATED(OperationalEventSourceObject.DECISION, OperationalEventAction.EVALUATED),
    ENTITY_BOOTSTRAPPED(OperationalEventSourceObject.CONFIGSTORE_ENTITY, OperationalEventAction.ENTITY_BOOTSTRAPPED),
    ENTITY_ADDED(OperationalEventSourceObject.CONFIGSTORE_ENTITY, OperationalEventAction.ENTITY_ADDED),
    ENTITY_UPDATED(OperationalEventSourceObject.CONFIGSTORE_ENTITY, OperationalEventAction.ENTITY_UPDATED),
    ENTITY_DELETED(OperationalEventSourceObject.CONFIGSTORE_ENTITY, OperationalEventAction.ENTITY_DELETED),
    SCANNER_TERMINATED(OperationalEventSourceObject.CONFIGSTORE, OperationalEventAction.SCANNER_TERMINATED),
    SCAN_COMPLETED(OperationalEventSourceObject.CONFIGSTORE, OperationalEventAction.SCANNED),
    CONFIGSTORE_POLLING_CONFIG_UPDATED(OperationalEventSourceObject.CONFIGSTORE, OperationalEventAction.POLLING_CONFIG_UPDATED),
    JWT_PUBLIC_KEY_REGISTERED(OperationalEventSourceObject.JWT, OperationalEventAction.PUBLIC_KEY_REGISTERED),
    JWT_PUBLIC_KEY_REQUESTED(OperationalEventSourceObject.JWT, OperationalEventAction.PUBLIC_KEY_REQUESTED),
    JWT_EVALUATED(OperationalEventSourceObject.JWT, OperationalEventAction.EVALUATED),
    HEALTH_CHECK_EVALUATED(OperationalEventSourceObject.HEALTH_CHECK, OperationalEventAction.EVALUATED),
    EXTERNAL_EXCEPTION_ENCOUNTERED(OperationalEventSourceObject.APPLICATION_METHOD, OperationalEventAction.EXCEPTION_THROWN),
    SERVICE_STARTUP_FINISHED(OperationalEventSourceObject.SYSTEM, OperationalEventAction.FIRST_POSITIVE_HEALTH_CHECK_RESPONSE),
    FILE_UPLOAD_PROCESS(OperationalEventSourceObject.FILE_UPLOAD, OperationalEventAction.UPLOAD_REQUEST_HANDLED);

    private final @NonNull OperationalEventSourceObject entity;
    private final @NonNull OperationalEventAction action;

    public static OperationalEventType fromLoadOperationResult(final EntityLoadOperationResult loadOperationResult) {
        return fromChangeType(loadOperationResult.getChangeType(), loadOperationResult.isBootstrap());
    }

    public static OperationalEventType fromChangeType(final EntityState.Delta.ChangeType changeType,
                                                      final boolean isBootstrap) {
        if (isBootstrap) {
            return OperationalEventType.ENTITY_BOOTSTRAPPED;
        }
        switch (changeType) {
            case ADD:
                return OperationalEventType.ENTITY_ADDED;
            case UPDATE:
                return OperationalEventType.ENTITY_UPDATED;
            case DELETE:
                return OperationalEventType.ENTITY_DELETED;
            default:
                throw new IllegalArgumentException(String.format("Unexpected change type received: %s", changeType));
        }

    }
}
