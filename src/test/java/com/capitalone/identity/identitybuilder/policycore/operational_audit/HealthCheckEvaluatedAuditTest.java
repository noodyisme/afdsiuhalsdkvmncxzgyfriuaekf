package com.capitalone.identity.identitybuilder.policycore.operational_audit;

import com.capitalone.identity.identitybuilder.audit.models.AuditEvent;
import com.capitalone.identity.identitybuilder.audit.sdpv4.AuditPublisher;
import com.capitalone.identity.identitybuilder.audit.sdpv4.ProducerProperties;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.cyber.AWSHostContextCollector;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.mapper.HealthCheckEvaluatedMapper;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.mapper.OperationalEventEnricher;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.model.OperationalEvent;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.model.OperationalSystemContext;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.model.event_models.HealthCheckEvaluated;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.model.event_models.HealthCheckEvaluated_Publisher;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.util.MockHostContext;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.util.OperationalAuditPublisher;
import lombok.SneakyThrows;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;

import java.util.Objects;

@ContextConfiguration(classes = {
        HealthCheckEvaluatedAuditTest.StartupTestConfig.class
})
public class HealthCheckEvaluatedAuditTest {

    // Non-mocked Dependencies
    private AuditPublisher auditPublisher;
    private final AWSHostContextCollector mockAWSHostContextCollector = new AWSHostContextCollector(new MockHostContext());
    private ArgumentCaptor<AuditEvent> auditEventArgumentCaptor;
    @Autowired
    private HealthCheckEvaluated_Publisher healthPublisher;

    @SneakyThrows
    @BeforeEach
    void setup() {
        auditPublisher = Mockito.mock(AuditPublisher.class);
        ProducerProperties producerProperties = Mockito.mock(ProducerProperties.class);
        Mockito.lenient().when(producerProperties.getSchemaName()).thenReturn("testSchema");
        OperationalSystemContext operationalSystemContext = Mockito.mock(OperationalSystemContext.class);
        OperationalAuditor operationalAuditor = new OperationalAuditor(
                new OperationalAuditor.OperationalAuditDispatcher(
                        new OperationalEventEnricher(operationalSystemContext, true),
                        new OperationalAuditPublisher(auditPublisher, producerProperties.getSchemaName()),
                        mockAWSHostContextCollector));
        healthPublisher = new HealthCheckEvaluatedMapper.HealthCheckPublisher(operationalAuditor);
        auditEventArgumentCaptor = ArgumentCaptor.forClass(AuditEvent.class);
    }

    @Test
    void auditServiceAvailableTest() {
        healthPublisher.publishEvent(new HealthCheckEvaluated(HttpStatus.SC_OK));
        verifyAuditSuccessEvent("SUCCESS", 2);
    }

    @Test
    void auditServiceUnavailableTest() {
        healthPublisher.publishEvent(new HealthCheckEvaluated(HttpStatus.SC_FAILED_DEPENDENCY));
        verifyAuditSuccessEvent("FAILURE", 1);
    }


    private void verifyAuditSuccessEvent(String expectedResult, int numExpected) {
        //multiple events are captured, return the last operational event captured, this should always be the HealthCheckEvaluated event
        OperationalEvent operationalEvent = OperationalAuditTestUtil.captureOperationalEvents(
                auditPublisher, auditEventArgumentCaptor, numExpected).get(numExpected - 1);
        Assertions.assertEquals("HEALTH_CHECK_EVALUATED", operationalEvent.getEventName());
        Assertions.assertEquals("HEALTH_CHECK", operationalEvent.getEventEntity());
        Assertions.assertEquals("EVALUATED", operationalEvent.getEventAction());
        if (Objects.equals(expectedResult, "SUCCESS")) {
            Assertions.assertEquals("200", operationalEvent.getEventOutcome().getSeverity());
            Assertions.assertEquals("SUCCESS", operationalEvent.getEventOutcome().getResult());
        } else {
            Assertions.assertEquals("424", operationalEvent.getEventOutcome().getSeverity());
            Assertions.assertEquals("FAILURE", operationalEvent.getEventOutcome().getResult());
        }

    }

    @Configuration
    public static class StartupTestConfig {
        @Bean
        OperationalAuditor operationalAuditor() {
            return new OperationalAuditor(null);
        }

        @Bean
        HealthCheckEvaluated_Publisher healthCheckEvaluated_publisher() {
            return new HealthCheckEvaluatedMapper.HealthCheckPublisher(operationalAuditor());
        }


    }

}
