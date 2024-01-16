package com.capitalone.identity.identitybuilder.policycore.operational_audit;

import com.capitalone.identity.identitybuilder.audit.models.AuditEvent;
import com.capitalone.identity.identitybuilder.audit.sdpv4.AuditPublisher;
import com.capitalone.identity.identitybuilder.audit.sdpv4.ProducerProperties;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.cyber.AWSHostContextCollector;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.mapper.JwkValidatedEventMapper;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.mapper.OperationalEventEnricher;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.model.OperationalEvent;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.model.OperationalSystemContext;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.model.event_models.JwkValidated;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.model.event_models.JwkValidated_Publisher;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.util.MockHostContext;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.util.OperationalAuditPublisher;
import com.capitalone.identity.identitybuilder.policycore.service.jws.JWTExtractionException;
import lombok.SneakyThrows;
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
        JwkValidatedAuditTest.StartupTestConfig.class
})
class JwkValidatedAuditTest {

    // Non-mocked Dependencies
    private AuditPublisher auditPublisher;
    private final AWSHostContextCollector mockAWSHostContextCollector = new AWSHostContextCollector(new MockHostContext());
    private ArgumentCaptor<AuditEvent> auditEventArgumentCaptor;
    @Autowired
    private JwkValidated_Publisher jwkValidatedPublisher;

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
        jwkValidatedPublisher = new JwkValidatedEventMapper.JwkValidationPublisher(operationalAuditor);
        auditEventArgumentCaptor = ArgumentCaptor.forClass(AuditEvent.class);
    }


    @Test
    void auditServiceAvailableTest() {
        jwkValidatedPublisher.publishEvent(new JwkValidated("",null,true,true,"",""));
        verifyAuditSuccessEvent("SUCCESS");
    }

    @Test
    void auditServiceUnavailableTest() {
        jwkValidatedPublisher.publishEvent(new JwkValidated("", JWTExtractionException.JwsExceptionType.JWT_MISSING_CERTIFICATE,true,true,"",""));
        verifyAuditSuccessEvent("FAILURE");
    }


    private void verifyAuditSuccessEvent(String expectedResult) {
        OperationalEvent operationalEvent = OperationalAuditTestUtil.captureOperationalEvents(
                auditPublisher, auditEventArgumentCaptor, 1).get(0);
        Assertions.assertEquals("JWT_EVALUATED", operationalEvent.getEventName());
        Assertions.assertEquals("JWT", operationalEvent.getEventEntity());
        Assertions.assertEquals("EVALUATED", operationalEvent.getEventAction());
        if (Objects.equals(expectedResult, "SUCCESS")) {
            Assertions.assertEquals("SUCCESS", operationalEvent.getEventOutcome().getSeverity());
            Assertions.assertEquals("SUCCESS", operationalEvent.getEventOutcome().getResult());
        } else {
            Assertions.assertEquals("FAILURE", operationalEvent.getEventOutcome().getSeverity());
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
        JwkValidated_Publisher jwkValidated_publisher() {
            return new JwkValidatedEventMapper.JwkValidationPublisher(operationalAuditor());
        }


    }
}
