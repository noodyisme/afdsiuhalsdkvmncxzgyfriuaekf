package com.capitalone.identity.identitybuilder.policycore.operational_audit.model.type;

public enum OperationalEventAction {
    EVALUATED, ENTITY_BOOTSTRAPPED, ENTITY_ADDED, ENTITY_UPDATED, ENTITY_DELETED, SCANNED, PUBLIC_KEY_VALIDATED,
    POLLING_CONFIG_UPDATED, SCANNER_TERMINATED, PUBLIC_KEY_REGISTERED, EXCEPTION_THROWN, UPLOAD_REQUEST_HANDLED, PUBLIC_KEY_REQUESTED,
    FIRST_POSITIVE_HEALTH_CHECK_RESPONSE
}