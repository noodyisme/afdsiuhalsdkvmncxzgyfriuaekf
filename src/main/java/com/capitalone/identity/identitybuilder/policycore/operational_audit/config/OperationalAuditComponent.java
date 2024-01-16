package com.capitalone.identity.identitybuilder.policycore.operational_audit.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.AliasFor;
import org.springframework.stereotype.Component;

import java.lang.annotation.*;

/**
 * Convenience annotation that specifies a component with feature flag(s) tied to operational audit configuration.
 */
@Target({ ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@ConditionalOnProperty("identitybuilder.policycore.operational_audit.enabled")
@Component
public @interface OperationalAuditComponent {
    @AliasFor(
            annotation = Component.class
    )
    String value() default "";
}