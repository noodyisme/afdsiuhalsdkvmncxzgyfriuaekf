package com.capitalone.identity.identitybuilder.policycore.operational_audit;

import com.capitalone.identity.identitybuilder.audit.models.AuditEvent;
import com.capitalone.identity.identitybuilder.audit.sdpv4.AuditPublisher;
import com.capitalone.identity.identitybuilder.audit.sdpv4.ProducerProperties;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.cyber.AWSHostContextCollector;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.mapper.OperationalEventEnricher;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.model.*;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.util.MockHostContext;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.util.OperationalAuditPublisher;
import com.capitalone.identity.identitybuilder.policycore.service.jws.JwtKeyModel;
import com.capitalone.identity.identitybuilder.policycore.service.jws.JwtPublicKeyPushResponse;
import com.capitalone.identity.identitybuilder.policycore.service.jws.JwtPublicKeyPushService;
import com.nimbusds.jose.Algorithm;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.HashMap;

import static org.mockito.ArgumentMatchers.any;

public class JwtPublicKeyRegisteredAuditTest {

    private static final String TEST_KID = "testKid";
    private static final String TEST_TRANSFORMED_PUBLIC_JWK = "transformedPublicJWK";
    private static final long TEST_KEY_PAIR_EXP = 1000L;
    private static final String TEST_DXGW_RESPONSE = "test push response";
    private static final String UNABLE_TO_PUSH_PUBLIC_KEY_TO_DXGW = "Unable to push public key to DXGW";
    private final AWSHostContextCollector mockAWSHostContextCollector = new AWSHostContextCollector(new MockHostContext());
    // Dependencies
    private AuditPublisher auditPublisher;
    private OperationalSystemContext operationalSystemContext;
    private JwtKeyModel jwtKeyModel;
    private JwtPublicKeyPushResponse pushResponse;
    private JwtPublicKeyPushService testKeyPushService;
    // Argument captor
    private ArgumentCaptor<AuditEvent> auditEventArgumentCaptor;
    // Object under test
    private OperationalAuditAspect operationalAuditAspect;

    private static JwtPublicKeyPushResponse defaultJwtPublicKeyPushResponse(final int statusCode) {
        return new JwtPublicKeyPushResponse(TEST_DXGW_RESPONSE, statusCode);
    }

    @SneakyThrows
    @BeforeEach
    void setup() {
        // Initialize the aspect
        auditPublisher = Mockito.mock(AuditPublisher.class);
        ProducerProperties producerProperties = Mockito.mock(ProducerProperties.class);
        Mockito.lenient().when(producerProperties.getSchemaName()).thenReturn("testSchema");
        operationalSystemContext = Mockito.mock(OperationalSystemContext.class);

        jwtKeyModel = Mockito.mock(JwtKeyModel.class);
        pushResponse = Mockito.mock(JwtPublicKeyPushResponse.class);

        operationalAuditAspect = new OperationalAuditAspect(
                new OperationalEventEnricher(operationalSystemContext, true),
                new OperationalAuditPublisher(auditPublisher, producerProperties.getSchemaName()),
                new HashMap<>(), mockAWSHostContextCollector);

        testKeyPushService = Mockito.mock(JwtPublicKeyPushService.class);

        // Initialize the argument captor used for verification
        auditEventArgumentCaptor = ArgumentCaptor.forClass(AuditEvent.class);

        RSAKey testKeyPair = new RSAKeyGenerator(2048)
                .keyUse(KeyUse.SIGNATURE)
                .keyID(TEST_KID)
                .algorithm(new Algorithm("RS256"))
                .generate();
        jwtKeyModel = JwtKeyModel.builder()
                .jwtKeyPair(testKeyPair)
                .keyPairExp(TEST_KEY_PAIR_EXP)
                .transformedPublicJWK(TEST_TRANSFORMED_PUBLIC_JWK)
                .keyPairKid(TEST_KID)
                .build();
    }

    @ParameterizedTest
    @ValueSource(ints = {200})
    void auditKeyRegisteredSuccessfully(final int statusCode) {
        final JwtPublicKeyPushResponse jwtPublicKeyPushResponseExpected = defaultJwtPublicKeyPushResponse(statusCode);
        Mockito.when(testKeyPushService.pushPublicKey(any())).thenReturn(jwtPublicKeyPushResponseExpected);
        testKeyPushService = OperationalAuditTestUtil.registerWithAspect(testKeyPushService, operationalAuditAspect);
        testKeyPushService.pushPublicKey(jwtKeyModel);
        expectedKeyRegisteredEvent(OperationalEventOutcome.of(OperationalStatus.SUCCESS, String.valueOf(statusCode)),
                jwtKeyModel,
                jwtPublicKeyPushResponseExpected,
                null);
    }

    @ParameterizedTest
    @ValueSource(ints = {400, 409, 500})
    void auditKeyRegisteredFailed(final int statusCode) {
        final JwtPublicKeyPushResponse jwtPublicKeyPushResponseExpected = defaultJwtPublicKeyPushResponse(statusCode);
        Mockito.when(testKeyPushService.pushPublicKey(any())).thenReturn(jwtPublicKeyPushResponseExpected);
        testKeyPushService = OperationalAuditTestUtil.registerWithAspect(testKeyPushService, operationalAuditAspect);
        testKeyPushService.pushPublicKey(jwtKeyModel);
        final OperationalError operationalErrorExpected = OperationalError.builder()
                .text(UNABLE_TO_PUSH_PUBLIC_KEY_TO_DXGW)
                .developerText(TEST_DXGW_RESPONSE)
                .build();
        expectedKeyRegisteredEvent(OperationalEventOutcome.of(OperationalStatus.FAILURE, String.valueOf(statusCode)),
                jwtKeyModel,
                jwtPublicKeyPushResponseExpected,
                operationalErrorExpected);
    }

    private void expectedKeyRegisteredEvent(final OperationalEventOutcome expectedEventOutcome,
                                            final JwtKeyModel expectedJwtKeyModel,
                                            final JwtPublicKeyPushResponse expectedJwtPublicKeyPushResponse,
                                            final OperationalError expectedErrorMetadata) {
        final OperationalEvent operationalEvent = OperationalAuditTestUtil.captureOperationalEvent(
                auditPublisher, auditEventArgumentCaptor);

        // Entity naming assertions
        Assertions.assertEquals("JWT_PUBLIC_KEY_REGISTERED", operationalEvent.getEventName());
        Assertions.assertEquals("JWT", operationalEvent.getEventEntity());
        Assertions.assertEquals("PUBLIC_KEY_REGISTERED", operationalEvent.getEventAction());

        // Operation id
        Assertions.assertNotNull(operationalEvent.getEventOperationId());

        // Event Entity Name
        Assertions.assertEquals("JWT_KEY_PAIR_GENERATION", operationalEvent.getEventEntityName());

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
