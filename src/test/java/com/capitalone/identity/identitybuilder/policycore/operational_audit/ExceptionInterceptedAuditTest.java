package com.capitalone.identity.identitybuilder.policycore.operational_audit;

import com.capitalone.chassis.engine.model.exception.ChassisSystemException;
import com.capitalone.identity.identitybuilder.audit.models.AuditEvent;
import com.capitalone.identity.identitybuilder.audit.sdpv4.AuditPublisher;
import com.capitalone.identity.identitybuilder.audit.sdpv4.ProducerProperties;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.cyber.AWSHostContextCollector;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.mapper.ExceptionInterceptedEventMapper;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.mapper.OperationalEventEnricher;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.model.OperationalEvent;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.model.OperationalSystemContext;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.model.event_models.ExceptionIntercepted;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.model.event_models.ExceptionIntercepted_Publisher;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.util.MockHostContext;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.util.OperationalAuditPublisher;
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

import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import java.util.Collections;
import java.util.UUID;

import static com.capitalone.identity.identitybuilder.policycore.operational_audit.model.type.OperationalEventType.EXTERNAL_EXCEPTION_ENCOUNTERED;
import static com.capitalone.identity.identitybuilder.policycore.operational_audit.util.AuditTestConstants.*;

@ContextConfiguration(classes = {
        ExceptionInterceptedAuditTest.StartupTestConfig.class
})
public class ExceptionInterceptedAuditTest {
    private final AWSHostContextCollector mockAWSHostContextCollector = new AWSHostContextCollector(new MockHostContext());
    @Autowired
    ExceptionIntercepted_Publisher exceptionPublisher;
    ClientRequestContext mockRequestContext;
    RuntimeException ex = new RuntimeException();
    UUID id = UUID.randomUUID();
    MultivaluedMap<String, String> requestHeaders = headersMapAllIncluded();
    private AuditPublisher auditPublisher;
    private ArgumentCaptor<AuditEvent> auditEventArgumentCaptor;

    private static MultivaluedMap<String, String> headersMapAllIncluded() {
        final MultivaluedHashMap<String, String> retVal = new MultivaluedHashMap<>();
        retVal.put("Channel-Type", Collections.singletonList(DX_CHANNEL_TYPE));
        retVal.put("Customer-IP-Address", Collections.singletonList("127.0.0.1"));
        retVal.put("Business-Event", Collections.singletonList(DX_BUSINESS_EVENT));
        retVal.put("Domain", Collections.singletonList(DX_DOMAIN));
        retVal.put("x-journey-policy", Collections.singletonList(DX_X_JOURNEY));
        retVal.put("Api-key", Collections.singletonList(DX_API_KEY));
        retVal.put("Client-Api-Key", Collections.singletonList("Mobile"));
        retVal.put("Client-Correlation-Id", Collections.singletonList("xxxxx-yyyyy-zzzz"));
        retVal.put("foo_client", Collections.singletonList("foo1")); // ASSUMED REDACTED B/C CONTAINS 'CLIENT'
        retVal.put("foo_customer", Collections.singletonList("foo2")); // ASSUMED REDACTED B/C CONTAINS 'CUSTOMER'
        retVal.put("Foo-Supplemental-Not-Redacted", Collections.singletonList("foo3"));
        retVal.put("Accept", Collections.singletonList(DX_ACCEPT));
        retVal.put("Content-Type", Collections.singletonList(DX_CONTENT_TYPE));
        retVal.put("x-upstream-env", Collections.singletonList("bar_upstream"));
        return retVal;
    }

    @SneakyThrows
    @BeforeEach
    void setup() {
        auditPublisher = Mockito.mock(AuditPublisher.class);
        mockRequestContext = Mockito.mock(ClientRequestContext.class);
        ProducerProperties producerProperties = Mockito.mock(ProducerProperties.class);
        Mockito.lenient().when(producerProperties.getSchemaName()).thenReturn("testSchema");
        OperationalSystemContext operationalSystemContext = Mockito.mock(OperationalSystemContext.class);
        OperationalAuditor operationalAuditor = new OperationalAuditor(
                new OperationalAuditor.OperationalAuditDispatcher(
                        new OperationalEventEnricher(operationalSystemContext, false),
                        new OperationalAuditPublisher(auditPublisher, producerProperties.getSchemaName()),
                        mockAWSHostContextCollector));
        exceptionPublisher = new ExceptionInterceptedEventMapper.ExceptionInterceptPublisher(operationalAuditor);
        auditEventArgumentCaptor = ArgumentCaptor.forClass(AuditEvent.class);
        Mockito.when(mockRequestContext.getStringHeaders()).thenReturn(requestHeaders);
    }

    @Test
    void auditRuntimeErrorTest() {

        exceptionPublisher.publishEvent(new ExceptionIntercepted("auditRuntimeErrorTest",
                "ExceptionInterceptedAuditTest", "", ex, 500, id));
        verifyAuditEvent();
    }

    @Test
    void auditChassisErrorTest() {
        exceptionPublisher.publishEvent(new ExceptionIntercepted("auditChassisErrorTest",
                "ExceptionInterceptedAuditTest", "[]", new ChassisSystemException(),
                500, UUID.randomUUID()));
        verifyAuditEvent();
    }


    private void verifyAuditEvent() {
        OperationalEvent operationalEvent = OperationalAuditTestUtil.captureOperationalEvent(
                auditPublisher, auditEventArgumentCaptor);
        Assertions.assertEquals(EXTERNAL_EXCEPTION_ENCOUNTERED.name(), operationalEvent.getEventName());
    }

    @Configuration
    public static class StartupTestConfig {
        @Bean
        OperationalAuditor operationalAuditor() {
            return new OperationalAuditor(null);
        }

        @Bean
        ExceptionIntercepted_Publisher exceptionIntercepted_publisher() {
            return new ExceptionInterceptedEventMapper.ExceptionInterceptPublisher(operationalAuditor());
        }
    }
}
