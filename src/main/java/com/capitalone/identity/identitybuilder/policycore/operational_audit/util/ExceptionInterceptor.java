package com.capitalone.identity.identitybuilder.policycore.operational_audit.util;

import com.capitalone.chassis.engine.model.context.RequestContextHolder;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.mapper.ChassisStatusCodeResolver;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.model.event_models.ExceptionIntercepted;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.model.event_models.ExceptionIntercepted_Publisher;
import lombok.extern.log4j.Log4j2;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;


/**
 * This class is meant to intercept and audit any exceptions thrown by a Pointcut registered via the
 * {@link AfterThrowing} annotation on {@link #auditThrown(JoinPoint, Throwable)}
 */
@Log4j2
@Aspect
@Component
public class ExceptionInterceptor {
    private static final Logger logger = LogManager.getLogger(ExceptionInterceptor.class);
    private final ChassisStatusCodeResolver chassisStatusCodeResolver;
    private final ExceptionIntercepted_Publisher exceptionInterceptedPublisher;

    @Inject
    ExceptionInterceptor(@Autowired(required = false) ChassisStatusCodeResolver chassisStatusCodeResolver,
                         @Autowired(required = false) ExceptionIntercepted_Publisher publisher) {
        this.chassisStatusCodeResolver = chassisStatusCodeResolver;
        this.exceptionInterceptedPublisher = publisher;
    }

    @Pointcut("execution(* com.capitalone.identity.identitybuilder.policycore.fileupload..*.*(..))")
    private void fileUploadMethodsOnExecution() {
    }

    @Pointcut("execution(* com.capitalone.identity.identitybuilder.policycore.camel.util.CamelCacheUtil.*(..))")
    private void camelCacheUtilOnExecution() {
    }

    /**
     * This method is meant to be a "Break The Glass" utility to create system logs and Operational Audit
     * notification events for any exception thrown in the pointcut scope. This method should be used on an
     * AS NEEDED basis in any deployment, and is otherwise a useful tool during troubleshooting.
     *
     * @param joinPoint - AOP JoinPoint of executed method that threw ex
     * @param ex - Thrown Throwable that we will audit.
     */

    @AfterThrowing(pointcut = "fileUploadMethodsOnExecution()", throwing = "ex")
    public void auditThrown(final JoinPoint joinPoint, Throwable ex) {
        UUID exceptionIdentifier = UUID.randomUUID();
        Signature signature = joinPoint.getSignature();
        String methodName = signature.getName();
        String declaringClassName = signature.getDeclaringType().getSimpleName();
        String arguments = Arrays.toString(joinPoint.getArgs());
        String clientCorrelationId = RequestContextHolder.getRequestContextOrDefault().getCorrelationId();

        logger.error("{} caught during {} -> {} execution. Check audit logs for operational_event_id: {} and CCID: {} :",
                ex.getClass().getSimpleName(), declaringClassName, methodName, exceptionIdentifier, clientCorrelationId, ex);

        if(chassisStatusCodeResolver != null && exceptionInterceptedPublisher != null) {
            final Integer statusCode = chassisStatusCodeResolver.resolveHttpStatusCode(
                    RequestContextHolder.getRequestContextOrDefault(),
                    Optional.of(ex));

            exceptionInterceptedPublisher.publishEvent(new ExceptionIntercepted(methodName, declaringClassName, arguments,
                    ex, statusCode, exceptionIdentifier));
        }
    }

}
