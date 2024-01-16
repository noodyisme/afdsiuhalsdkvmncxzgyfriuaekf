package com.capitalone.identity.identitybuilder.policycore.operational_audit;

import com.capitalone.identity.identitybuilder.policycore.operational_audit.mapper.OperationalEventMapper;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.model.type.OperationalEventType;

import java.lang.annotation.*;

/**
 * This annotation is used to mark a method invoking an operationally significant event so that
 * necessary auditing requirements associated with it can be handled separately
 * in a configurable manner.
 *
 * Note: When using this annotation, make sure that the annotated method has its own dedicated
 * spring object (its own java class) whose method is invoked from an external object.
 * Calling a spring aop annotated method within the same object will not work due to the manner
 * in which spring aop is implemented, unless using this workaround:
 * https://stackoverflow.com/questions/30598118/spring-aop-not-working-when-the-method-is-called-internally-within-a-bean
 */
@Documented
@Retention(value = RetentionPolicy.RUNTIME)
@Target(value = { ElementType.METHOD})
public @interface OperationalAudit {

    /**
     * The event type associated with this aspect, specifying the entity and action of the event being audited
     * @return the event type
     */
    OperationalEventType eventType();

    /**
     * Factory type for mapper which translates method param/return values into operational
     * event for audit. 
     * @return the factory type
     */
    Class<? extends OperationalEventMapper.AspectEventFactory> mapperFactory();

}
