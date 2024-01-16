package com.capitalone.identity.identitybuilder.policycore.operational_audit.util;

import com.capitalone.identity.identitybuilder.audit.models.AuditEvent;
import com.capitalone.identity.identitybuilder.audit.sdpv4.AuditPublisher;
import com.capitalone.identity.identitybuilder.audit.sdpv4.ProducerProperties;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.OperationalAuditTestUtil;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.OperationalAuditor;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.cyber.AWSHostContextCollector;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.mapper.HealthCheckEvaluatedMapper;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.mapper.OperationalEventEnricher;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.model.OperationalEvent;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.model.OperationalSupplementalAttribute;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.model.OperationalSystemContext;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.model.event_models.HealthCheckEvaluated;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.model.type.OperationalEventType;
import com.capitalone.identity.identitybuilder.policycore.service.logging.EnrichBootstrapAuditEvent;
import org.aspectj.lang.ProceedingJoinPoint;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.aop.aspectj.annotation.AnnotationAwareAspectJAutoProxyCreator;
import org.springframework.boot.availability.ApplicationAvailabilityBean;
import org.springframework.boot.availability.AvailabilityChangeEvent;
import org.springframework.boot.availability.ReadinessState;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

import java.lang.management.ManagementFactory;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest(classes = {AnnotationAwareAspectJAutoProxyCreator.class, ApplicationAvailabilityBean.class, EnrichBootstrapAuditEvent.class})
@ContextConfiguration(classes = {ApplicationBootstrapListenerTest.CustomAspectContext.class})
@TestPropertySource(properties = {
        "spring.main.allow-bean-definition-overriding=true"
})
@ExtendWith(MockitoExtension.class)
public class ApplicationBootstrapListenerTest {

    // test specific fields
    private static final long baseTime = ManagementFactory.getRuntimeMXBean().getStartTime();
    private static final long applicationAcceptingTrafficTime = baseTime + 5L;
    private static final long finalTime = baseTime + 38L;
    private final AWSHostContextCollector mockAWSHostContextCollector = new AWSHostContextCollector(new MockHostContext());
    TimingTestUtil timingTestUtil = new TimingTestUtil();


    //health check specific fields
    private HealthCheckEvaluatedMapper.HealthCheckPublisher healthCheckPublisher;

    private final HealthCheckEvaluated positiveHealthCheck = new HealthCheckEvaluated(200);

    // bootstrap event specific fields

    ApplicationAvailabilityBean availabilityBean;
    ApplicationBootstrapListener applicationBootstrapListener;
    //audit essentials
    private AuditPublisher auditPublisher;
    private ArgumentCaptor<AuditEvent> auditEventArgumentCaptor;

    @BeforeEach
    void setup() {

        //audit essentials
        auditPublisher = Mockito.mock(AuditPublisher.class);
        ProducerProperties producerProperties = Mockito.mock(ProducerProperties.class);
        Mockito.lenient().when(producerProperties.getSchemaName()).thenReturn("testSchema");
        OperationalSystemContext operationalSystemContext = Mockito.mock(OperationalSystemContext.class);
        OperationalAuditor operationalAuditor = new OperationalAuditor(
                new OperationalAuditor.OperationalAuditDispatcher(
                        new OperationalEventEnricher(operationalSystemContext, true),
                        new OperationalAuditPublisher(auditPublisher, producerProperties.getSchemaName()),
                        mockAWSHostContextCollector));
        applicationBootstrapListener = new ApplicationBootstrapListener();
        auditEventArgumentCaptor = ArgumentCaptor.forClass(AuditEvent.class);
        healthCheckPublisher = new HealthCheckEvaluatedMapper.HealthCheckPublisher(operationalAuditor);
        //register pointcuts with aspect and mock services
        availabilityBean = new ApplicationAvailabilityBean();
        timingTestUtil = OperationalAuditTestUtil.registerMockWithCustomAspect(timingTestUtil, applicationBootstrapListener);
    }

    @Test
    void auditEventPublishedOnFirstHealthyResponseOnly_Test() {
        healthCheckPublisher.publishEvent(positiveHealthCheck);
        verifyAuditEvent("success", null, null);
        healthCheckPublisher.publishEvent(positiveHealthCheck);
        // initial health check publish will have two audit events, bootstrap and health check evaluated, second will only
        // have one, just the health check evaluated. expected publish() calls = 3
        verify(auditPublisher, times(3)).publish(any());
    }

    @Test
    void happyPath_Test() {
        Instant applicationAvailabilityTime = Instant.ofEpochMilli(applicationAcceptingTrafficTime);
        Instant finalTimeInstant = Instant.ofEpochMilli(finalTime);
        try (MockedStatic<Instant> mockedTime = Mockito.mockStatic(Instant.class)) {

            mockedTime.when(Instant::now).thenReturn(applicationAvailabilityTime);
            availabilityBean.onApplicationEvent(new AvailabilityChangeEvent<>(new TestObject(), ReadinessState.ACCEPTING_TRAFFIC));
            timingTestUtil.concatenateStrings("this", "that");

            mockedTime.when(Instant::now).thenReturn(finalTimeInstant);
            healthCheckPublisher.publishEvent(positiveHealthCheck);

            verifyAuditEvent("happy path", null, finalTime);
        }
    }

    @ParameterizedTest
    @ValueSource(longs = {0L, 5L, 10L})
    void timedDynamicMethodExecutionAudit_Test(long timeToSleep) {
        timingTestUtil.waitForPeriod(timeToSleep);
        healthCheckPublisher.publishEvent(positiveHealthCheck);
        verifyAuditEvent("timed method", "TimingTestUtil.waitForPeriod", timeToSleep);
    }

    void checkForBaseAudit(OperationalEvent operationalEvent) {
        Assertions.assertEquals(OperationalEventType.SERVICE_STARTUP_FINISHED.name(), operationalEvent.getEventReason());
        Assertions.assertEquals(OperationalEventType.SERVICE_STARTUP_FINISHED.getEntity().name(), operationalEvent.getEventEntity());
        Assertions.assertEquals(OperationalEventType.SERVICE_STARTUP_FINISHED.getAction().name(), operationalEvent.getEventAction());
        Assertions.assertEquals(baseTime, operationalEvent.getEventStartTimestamp());
    }

    private void checkForSuccessEvent(OperationalEvent operationalEvent) {
        Assertions.assertEquals("SUCCESS", operationalEvent.getEventOutcome().getResult());
        Assertions.assertTrue(operationalEvent.getEventOutcome().getSeverity().startsWith("Application Bootstrap Duration: "));
    }


    private void validateSupplementalMetadataDuration(OperationalEvent operationalEvent, String identifier, long expectedTime) {
        //supplemental metadata check
        List<OperationalSupplementalAttribute> metadata = operationalEvent.getSupplementalMetadata();
        Assertions.assertEquals(identifier + "_duration_ms", metadata.get(1).getKey());
        long duration = Long.parseLong(metadata.get(1).getValue());
        Assertions.assertTrue(duration >= expectedTime);
    }


    private void verifyAuditEvent(String eventType, String identifier, Long expectedTime) {
        List<OperationalEvent> eventList = OperationalAuditTestUtil.captureOperationalEvents(
                auditPublisher, auditEventArgumentCaptor, 2);

        OperationalEvent bootstrapEvent = eventList.get(0);
        checkForBaseAudit(bootstrapEvent);


        //event specific checks
        switch (eventType) {
            case "happy path":
                checkForSuccessEvent(bootstrapEvent);
                Assertions.assertEquals(expectedTime, bootstrapEvent.getEventEndTimestamp());
                break;
            case "timed method":
                checkForSuccessEvent(bootstrapEvent);
                validateSupplementalMetadataDuration(bootstrapEvent, identifier, expectedTime);
                break;
            default:
                checkForSuccessEvent(bootstrapEvent);
                break;
        }
    }

    /**
     * Test Object For Application Readiness Testing.
     */
    static class TestObject {

    }

    /**
     * Used to confirm timing of annotated method is audited via
     * {@link ApplicationBootstrapListener#auditExecutionDuration(ProceedingJoinPoint) auditExecutionDuration}
     */
    static class TimingTestUtil {

        @EnrichBootstrapAuditEvent
        public void concatenateStrings(String str1, String str2) {
            String str3 = str1 + str2;
            Assertions.assertEquals(str1 + str2, str3);
        }

        @EnrichBootstrapAuditEvent
        public void waitForPeriod(long sleepTime_ms) {
            Awaitility.await()
                    .pollDelay(sleepTime_ms, TimeUnit.MILLISECONDS)
                    .untilAsserted(() -> Assertions.assertTrue(true));
        }

    }

    @Configuration
    @EnableAspectJAutoProxy
    @ComponentScan(basePackageClasses = {ApplicationBootstrapListener.class})
    static class CustomAspectContext {
    }
}
