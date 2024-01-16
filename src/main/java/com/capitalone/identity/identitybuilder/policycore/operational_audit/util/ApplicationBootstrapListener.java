package com.capitalone.identity.identitybuilder.policycore.operational_audit.util;

import com.capitalone.identity.identitybuilder.events.ApplicationEvent;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.model.OperationalEvent;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.model.event_models.ApplicationBootstrapComplete;
import com.capitalone.identity.identitybuilder.policycore.service.logging.EnrichBootstrapAuditEvent;
import lombok.extern.log4j.Log4j2;
import org.apache.logging.log4j.CloseableThreadContext;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.boot.availability.AvailabilityChangeEvent;
import org.springframework.boot.availability.ReadinessState;
import org.springframework.stereotype.Component;

import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashMap;
import java.util.UUID;

@Log4j2
@Aspect
@Component
@Configurable
public class ApplicationBootstrapListener {
    private static final long TOTAL_COMPILE_TIME = ManagementFactory.getCompilationMXBean().getTotalCompilationTime();
    private static final HashMap<String, String> durations = new HashMap<>();
    private static Long applicationAvailableTime = 0L;
    private boolean bootstrapInProgress = true;


    /**
     * Audit the event when Spring declares the Application ready to receive traffic. This event should always occur on
     * successful Spring Application Bootstrap.
     *
     * @param joinPoint execution JoinPoint
     */
    @After(value = "execution(void org.springframework.boot.availability.ApplicationAvailabilityBean.onApplicationEvent(..))")
    public static void auditApplicationReadiness(JoinPoint joinPoint) {
        long applicationAvailableUpTime;
        AvailabilityChangeEvent<?> event = (AvailabilityChangeEvent<?>) joinPoint.getArgs()[0];
        if (event.getState() instanceof ReadinessState && event.getPayload() == ReadinessState.ACCEPTING_TRAFFIC) {
            applicationAvailableUpTime = ManagementFactory.getRuntimeMXBean().getUptime();
            applicationAvailableTime = Instant.now().toEpochMilli();
            populateDurationsMap("application_compile_time", TOTAL_COMPILE_TIME);
            populateDurationsMap("spring_application_availability", applicationAvailableUpTime);
        }

    }

    static String getSimpleNameFromSignature(Signature joinPointSignature) {
        return String.format("%s.%s", joinPointSignature.getDeclaringType().getSimpleName(), joinPointSignature.getName());
    }

    /**
     * This helper method will put a Key, Value pair 'joinPointIdentifier duration', 'duration ms' into the
     * {@link ApplicationBootstrapComplete } durations map
     *
     * @param methodIdentifier - the Class simple name and method reference of the Join Point (i.e. "Class.method")
     * @param duration         - the execution duration of the JoinPoint
     */
    static void populateDurationsMap(String methodIdentifier, long duration) {
        durations.put(String.format("%s_duration_ms", methodIdentifier), String.format("%d", duration));
    }

    /**
     * <p>
     * The execution duration for any method annotated with  will always be system logged here
     * at the INFO level.
     * </p>
     * <p>
     * Any {@link EnrichBootstrapAuditEvent} annotated dynamic method (Spring AOP limitation) executed before the first positive
     * health check will be included in the  {@link ApplicationBootstrapComplete} audit event
     * {@link OperationalEvent#getSupplementalMetadata()  supplementalMetadata} list
     *
     * @param joinPoint - The method being audited.
     * @return the original Object, unaltered by this method.
     * @throws Throwable Audited method may throw a Throwable, this method will not modify the Throwable thrown.
     */
    @Around(value = ("@annotation(com.capitalone.identity.identitybuilder.policycore.service.logging.EnrichBootstrapAuditEvent)"))
    public Object auditExecutionDuration(final ProceedingJoinPoint joinPoint) throws Throwable {
        UUID vmIdentifier = UUID.nameUUIDFromBytes(ManagementFactory.getRuntimeMXBean().getName().getBytes(StandardCharsets.UTF_8));
        long startTime = Instant.now().toEpochMilli();
        // populate event_operation_id in app logs even in event of failed execution
        try (final CloseableThreadContext.Instance ctc = CloseableThreadContext.put("event_operation_id", vmIdentifier.toString())) {
            return joinPoint.proceed();
        } finally {
            long endTime = Instant.now().toEpochMilli();
            long duration = endTime - startTime;
            String joinPointIdentifier = getSimpleNameFromSignature(joinPoint.getSignature());
            log.info(String.format("method_identifier: %s execution_duration_ms: %d event_operation_id: %s", joinPointIdentifier, duration, vmIdentifier));
            if (bootstrapInProgress) {
                populateDurationsMap(joinPointIdentifier, duration);
            }
        }
    }

    public ApplicationBootstrapComplete setBootstrapCompleteAndReturnEvent() {
        ApplicationEvent.Metadata metadata = new ApplicationEvent.Metadata(ManagementFactory.getRuntimeMXBean().getStartTime(),
                Instant.now().toEpochMilli());
        UUID vmIdentifier = UUID.nameUUIDFromBytes(ManagementFactory.getRuntimeMXBean().getName().getBytes(StandardCharsets.UTF_8));
        bootstrapInProgress = false;
        return new ApplicationBootstrapComplete(metadata, applicationAvailableTime, durations, vmIdentifier);
    }

    public boolean isBootstrapComplete() {
        return bootstrapInProgress;
    }

}
