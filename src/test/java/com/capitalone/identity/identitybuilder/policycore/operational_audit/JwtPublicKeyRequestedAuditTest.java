package com.capitalone.identity.identitybuilder.policycore.operational_audit;

import com.capitalone.identity.identitybuilder.audit.models.AuditEvent;
import com.capitalone.identity.identitybuilder.audit.sdpv4.AuditPublisher;
import com.capitalone.identity.identitybuilder.audit.sdpv4.ProducerProperties;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.cyber.AWSHostContextCollector;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.mapper.OperationalEventEnricher;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.model.*;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.util.MockHostContext;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.util.OperationalAuditPublisher;
import com.capitalone.identity.identitybuilder.policycore.service.jws.JwtPublicKeyRetrievalResponse;
import com.capitalone.identity.identitybuilder.policycore.service.jws.JwtPublicKeyRetrievalService;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.HashMap;

import static org.mockito.ArgumentMatchers.any;

class JwtPublicKeyRequestedAuditTest {
    private static final String TEST_KID = "testKid";
    private static final String UNABLE_TO_RETRIEVE_PUBLIC_KEY_TO_DXGW = "Unable to retrieve public key from DXGW Key Management API";
    private static final String TEST_DXGW_RESPONSE = "testResponse";
    private final AWSHostContextCollector mockAWSHostContextCollector = new AWSHostContextCollector(new MockHostContext());
    // Dependencies
    private AuditPublisher auditPublisher;
    private OperationalSystemContext operationalSystemContext;
    private JwtPublicKeyRetrievalResponse retrievalResponse;
    private JwtPublicKeyRetrievalService testKeyRetrievalService;
    // Argument captor
    private ArgumentCaptor<AuditEvent> auditEventArgumentCaptor;
    // Object under test
    private OperationalAuditAspect operationalAuditAspect;
    private static JwtPublicKeyRetrievalResponse defaultJwtPublicKeyRetrievalResponse(final int statusCode) {
        return new JwtPublicKeyRetrievalResponse(statusCode,statusCode==200?"testJwk":null,String.format("testResponse"));
    }

    @SneakyThrows
    @BeforeEach
    void setup() {
        // Initialize the aspect
        auditPublisher = Mockito.mock(AuditPublisher.class);
        ProducerProperties producerProperties = Mockito.mock(ProducerProperties.class);
        Mockito.lenient().when(producerProperties.getSchemaName()).thenReturn("testSchema");
        operationalSystemContext = Mockito.mock(OperationalSystemContext.class);

        retrievalResponse = Mockito.mock(JwtPublicKeyRetrievalResponse.class);

        operationalAuditAspect = new OperationalAuditAspect(
                new OperationalEventEnricher(operationalSystemContext, true),
                new OperationalAuditPublisher(auditPublisher, producerProperties.getSchemaName()),
                new HashMap<>(), mockAWSHostContextCollector);

        testKeyRetrievalService = Mockito.mock(JwtPublicKeyRetrievalService.class);

        // Initialize the argument captor used for verification
        auditEventArgumentCaptor = ArgumentCaptor.forClass(AuditEvent.class);
    }

    @ParameterizedTest
    @ValueSource(ints = {200})
    void auditKeyRegisteredSuccessfully(final int statusCode) {
        final JwtPublicKeyRetrievalResponse jwtPublicKeyRetrievalResponseExpected = defaultJwtPublicKeyRetrievalResponse(statusCode);
        Mockito.when(testKeyRetrievalService.fetchJwtPublicKey(any())).thenReturn(jwtPublicKeyRetrievalResponseExpected);
        testKeyRetrievalService = OperationalAuditTestUtil.registerWithAspect(testKeyRetrievalService, operationalAuditAspect);
        testKeyRetrievalService.fetchJwtPublicKey(TEST_KID);
        expectedKeyRegisteredEvent(OperationalEventOutcome.of(OperationalStatus.SUCCESS, "SUCCESS"),
                TEST_KID,
                jwtPublicKeyRetrievalResponseExpected,
                null);
    }

    @ParameterizedTest
    @ValueSource(ints = {400, 409, 500})
    void auditKeyRegisteredFailed(final int statusCode) {
        final JwtPublicKeyRetrievalResponse jwtPublicKeyRetrievalResponseExpected = defaultJwtPublicKeyRetrievalResponse(statusCode);
        Mockito.when(testKeyRetrievalService.fetchJwtPublicKey(any())).thenReturn(jwtPublicKeyRetrievalResponseExpected);
        testKeyRetrievalService = OperationalAuditTestUtil.registerWithAspect(testKeyRetrievalService, operationalAuditAspect);
        testKeyRetrievalService.fetchJwtPublicKey(TEST_KID);
        final OperationalError operationalErrorExpected = OperationalError.builder()
                .text(UNABLE_TO_RETRIEVE_PUBLIC_KEY_TO_DXGW)
                .developerText(TEST_DXGW_RESPONSE)
                .build();
        expectedKeyRegisteredEvent(OperationalEventOutcome.of(OperationalStatus.FAILURE, "FAILURE"),
                TEST_KID,
                jwtPublicKeyRetrievalResponseExpected,
                operationalErrorExpected);
    }

    private void expectedKeyRegisteredEvent(final OperationalEventOutcome expectedEventOutcome,
                                            final String expectedKid,
                                            final JwtPublicKeyRetrievalResponse expectedJwtPublicKeyRetrievalResponse,
                                            final OperationalError expectedErrorMetadata) {
        final OperationalEvent operationalEvent = OperationalAuditTestUtil.captureOperationalEvent(
                auditPublisher, auditEventArgumentCaptor);

        // Entity naming assertions
        Assertions.assertEquals("JWT_PUBLIC_KEY_REQUESTED", operationalEvent.getEventName());
        Assertions.assertEquals("JWT", operationalEvent.getEventEntity());
        Assertions.assertEquals("PUBLIC_KEY_REQUESTED", operationalEvent.getEventAction());

        // Operation id
        Assertions.assertNotNull(operationalEvent.getEventOperationId());

        // Event Entity Name
        Assertions.assertEquals("JWT_CERTIFICATE_REQUESTED", operationalEvent.getEventEntityName());

        // Status
        Assertions.assertEquals(expectedEventOutcome, operationalEvent.getEventOutcome());

        // Timestamp/duration assertions
        Assertions.assertTrue(operationalEvent.getEventStartTimestamp() <= System.currentTimeMillis());
        Assertions.assertTrue(operationalEvent.getEventEndTimestamp() >= operationalEvent.getEventStartTimestamp());
        Assertions.assertEquals(operationalEvent.getEventEndTimestamp() - operationalEvent.getEventStartTimestamp(),
                Long.valueOf(operationalEvent.getEventDurationMs()));

        // System context
        Assertions.assertEquals(operationalSystemContext, operationalEvent.getSystemContext());

        // Error metadata
        Assertions.assertEquals(!expectedEventOutcome.getResult().equals("SUCCESS"), operationalEvent.getErrorMetadata() != null);
        if (operationalEvent.getErrorMetadata() != null) {
            Assertions.assertEquals(expectedErrorMetadata, operationalEvent.getErrorMetadata());
        }

        // Policy metadata assertions
        Assertions.assertNull(operationalEvent.getPolicyMetadata());

        // Request context
        Assertions.assertNotNull(operationalEvent.getRequestMessageId());
        Assertions.assertNotNull(operationalEvent.getRequestCorrelationId());

        // Config Store Metadata
        Assertions.assertNull(operationalEvent.getConfigStoreMetadata());

        // Dmn Metadata
        Assertions.assertNull(operationalEvent.getDmnMetadata());

        // Supplemental Metadata
        Assertions.assertNotNull(operationalEvent.getSupplementalMetadata());

    }
}