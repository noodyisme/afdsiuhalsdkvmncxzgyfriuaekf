package com.capitalone.identity.identitybuilder.policycore.operational_audit.util;

import com.capitalone.identity.identitybuilder.audit.models.AuditEvent;
import com.capitalone.identity.identitybuilder.audit.sdpv4.AuditPublisher;
import com.capitalone.identity.identitybuilder.audit.sdpv4.ProducerProperties;
import com.capitalone.identity.identitybuilder.policycore.fileupload.service.FileUploadEncryptionKeyService;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.OperationalAuditTestUtil;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.OperationalAuditor;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.cyber.AWSHostContextCollector;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.mapper.ChassisStatusCodeResolver;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.mapper.ExceptionInterceptedEventMapper;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.mapper.OperationalEventEnricher;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.model.OperationalEvent;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.model.OperationalSystemContext;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.model.event_models.ExceptionIntercepted_Publisher;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ContextConfiguration(classes = {
        ExceptionInterceptorAuditTest.StartupTestConfig.class
})
class ExceptionInterceptorAuditTest {
    private static final String T2V_IVR_PRODUCT = "t2v-ivr-product";

    private static final String ANY_STRING_YOU_WANT = "any string you want";
    private AuditPublisher auditPublisher;
    private final AWSHostContextCollector mockAWSHostContextCollector = new AWSHostContextCollector(new MockHostContext());
    private ArgumentCaptor<AuditEvent> auditEventArgumentCaptor;

    @Autowired
    private ExceptionIntercepted_Publisher exceptionPublisher;
    @Autowired
    private ExceptionInterceptor exceptionInterceptor;
    ChassisStatusCodeResolver chassisStatusCodeResolver;

    FileUploadEncryptionKeyService proxyFileUploadService;

    @BeforeEach
    void setup() {

        auditPublisher = Mockito.mock(AuditPublisher.class);
        chassisStatusCodeResolver = Mockito.mock(ChassisStatusCodeResolver.class);
        FileUploadEncryptionKeyService fileUploadEncryptionKeyService = Mockito.mock(FileUploadEncryptionKeyService.class);

        when(fileUploadEncryptionKeyService.encryptDocument(any(), any())).thenThrow(new RuntimeException("error."));
        ProducerProperties producerProperties = Mockito.mock(ProducerProperties.class);
        Mockito.lenient().when(producerProperties.getSchemaName()).thenReturn("testSchema");
        OperationalSystemContext operationalSystemContext = Mockito.mock(OperationalSystemContext.class);
        OperationalAuditor operationalAuditor = new OperationalAuditor(
                new OperationalAuditor.OperationalAuditDispatcher(
                        new OperationalEventEnricher(operationalSystemContext, true),
                        new OperationalAuditPublisher(auditPublisher, producerProperties.getSchemaName()),
                        mockAWSHostContextCollector));

        exceptionPublisher = new ExceptionInterceptedEventMapper.ExceptionInterceptPublisher(operationalAuditor);
        exceptionInterceptor = new ExceptionInterceptor(chassisStatusCodeResolver, exceptionPublisher);
        when(chassisStatusCodeResolver.resolveHttpStatusCode(any(), any())).thenReturn(500);
        AspectJProxyFactory factory = new AspectJProxyFactory(fileUploadEncryptionKeyService);
        factory.addAspect(exceptionInterceptor);

        proxyFileUploadService = factory.getProxy();
        auditEventArgumentCaptor = ArgumentCaptor.forClass(AuditEvent.class);
    }

    @Test
    void testFileUploadException() {
        byte[] data = ANY_STRING_YOU_WANT.getBytes();
        assertThrows(RuntimeException.class, () -> proxyFileUploadService.encryptDocument(T2V_IVR_PRODUCT, data));
        ArrayList<String> methodArgs = new ArrayList<>();
        methodArgs.add(T2V_IVR_PRODUCT);
        methodArgs.add(data.toString());
        verifyAuditEvent(methodArgs);
    }


    private void verifyAuditEvent(ArrayList<String> methodArgs) {
        OperationalEvent operationalEvent = OperationalAuditTestUtil.captureOperationalEvent(
                auditPublisher, auditEventArgumentCaptor);
        //validate type specific details are audited
        Assertions.assertEquals("EXTERNAL_EXCEPTION_ENCOUNTERED", operationalEvent.getEventName());
        Assertions.assertEquals("APPLICATION_METHOD", operationalEvent.getEventEntity());
        Assertions.assertEquals("EXCEPTION_THROWN", operationalEvent.getEventAction());
        Assertions.assertEquals("RuntimeException", operationalEvent.getEventEntityName());
        Assertions.assertEquals("FileUploadEncryptionKeyService#encryptDocument -> RuntimeException: error.", operationalEvent.getEventEntityValue());
        //validate operational error was audited
        Assertions.assertEquals("RuntimeException: error.", operationalEvent.getErrorMetadata().getDeveloperText());
        Assertions.assertEquals("500", operationalEvent.getEventOutcome().getSeverity());
        Assertions.assertEquals("FAILURE", operationalEvent.getEventOutcome().getResult());
        //validate method arguments list was audited
        Assertions.assertEquals(methodArgs.toString(), operationalEvent.getSupplementalMetadata().get(0).getValue());


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

        @MockBean
        ChassisStatusCodeResolver chassisStatusCodeResolver;

        @Bean
        ExceptionInterceptor exceptionInterceptor() {
            return new ExceptionInterceptor(chassisStatusCodeResolver, exceptionIntercepted_publisher());
        }

    }


}