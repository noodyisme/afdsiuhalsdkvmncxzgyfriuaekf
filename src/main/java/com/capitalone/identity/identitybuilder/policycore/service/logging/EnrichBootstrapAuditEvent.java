package com.capitalone.identity.identitybuilder.policycore.service.logging;

import com.capitalone.identity.identitybuilder.policycore.operational_audit.config.OperationalAuditComponent;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * This interface can be used to log execution time in milliseconds for any method at the INFO level,
 * but is intended for use in auditing event timings during Application Bootstrap.
 * Use sparingly outside Bootstrap context.
 */

@Target({ElementType.METHOD})
@OperationalAuditComponent
public @interface EnrichBootstrapAuditEvent {
}
