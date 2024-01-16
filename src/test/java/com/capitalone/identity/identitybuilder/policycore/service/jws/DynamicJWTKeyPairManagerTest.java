package com.capitalone.identity.identitybuilder.policycore.service.jws;

import com.capitalone.chassis.engine.model.exception.ChassisBusinessException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.Algorithm;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.Environment;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.util.ReflectionTestUtils.setField;

class DynamicJWTKeyPairManagerTest {

    private final Environment environment1 = mock(Environment.class);
    private final JwsService jwsService1 = mock(JwsService.class);
    private final JwtPublicKeyPushService jwtPublicKeyPushService = mock(JwtPublicKeyPushService.class);
    private DynamicJWTKeyPairManager keyPairManager;

    private RSAKey testKeyPair;
    private JwtPublicKeyPushResponse jwtPublicKeyPushResponse;

    private static final String JWT_KEY_PAIR_NOT_VALID = "JWT Key Pair is not valid, shutting down application";


    @BeforeEach
    void setUp() throws JOSEException {
        String[] profiles = {"test"};
        doReturn(profiles).when(environment1).getActiveProfiles();
        keyPairManager = spy(new DynamicJWTKeyPairManager(environment1,jwsService1,jwtPublicKeyPushService));
        setField(keyPairManager,"jwtKeyPairExpirationTimeInDays",7);
        setField(keyPairManager,"jwtProductId","testProductId");
        setField(keyPairManager,"jwtKeyPairEnabled",true);

        testKeyPair = new RSAKeyGenerator(2048)
                .keyUse(KeyUse.SIGNATURE)
                .keyID("testKid")
                .algorithm(new Algorithm("RS256"))
                .generate();
        jwtPublicKeyPushResponse = new JwtPublicKeyPushResponse("test response", 200);
    }


    @Test
    void testTransformJWK() throws JsonProcessingException {
        String transformedJWK = keyPairManager.transformJWK(testKeyPair.toPublicJWK().toJSONString(),100,"testProductId",600);
        Map<String, String> map = new ObjectMapper().readValue(transformedJWK, new TypeReference<Map<String, String>>() {});
        assertEquals("testProductId",map.get("productId"));
    }

    @Test
    void testDynamicKeyRotationSuccessfulPush() {
        doReturn(jwtPublicKeyPushResponse).when(jwtPublicKeyPushService).pushPublicKey(any());
        keyPairManager.dynamicKeyRotation();
        verify(jwtPublicKeyPushService, times(1)).pushPublicKey(any());
        verify(jwsService1, times(1)).storeKeyModel(any());
    }

    @Test
    void testDynamicKeyRotationUnsuccessfulPushAndNotStoringIntoJwsService() {
        doReturn(new JwtPublicKeyPushResponse("failure",400)).when(jwtPublicKeyPushService).pushPublicKey(any());
        keyPairManager.dynamicKeyRotation();
        verify(jwtPublicKeyPushService, times(1)).pushPublicKey(any());
        verify(jwsService1, never()).storeKeyModel(any());
    }

    @Test
    void testDynamicKeyRotationDisabled() {
        setField(keyPairManager,"jwtKeyPairEnabled",false);
        keyPairManager.dynamicKeyRotation();
        verify(jwtPublicKeyPushService, never()).pushPublicKey(any());
        verify(jwsService1, never()).storeKeyModel(any());
    }

    @Test
    void testIsKeyPairReadyGetter() {
        assertTrue(keyPairManager.isKeyPairReady());
    }

    @Test
    void testGenerateJWTKeyPairAfterStartUp() {
        doReturn(jwtPublicKeyPushResponse).when(jwtPublicKeyPushService).pushPublicKey(any());
        keyPairManager.generateJWTKeyPairAfterStartUp();
        verify(jwtPublicKeyPushService, times(1)).pushPublicKey(any());
        verify(jwsService1, times(1)).storeKeyModel(any());
    }

    @Test
    void testGenerateJWTKeyPairAfterStartUpNotRunWhenProductIdIsMissing() {
        setField(keyPairManager,"jwtProductId",null);
        keyPairManager.generateJWTKeyPairAfterStartUp();
        verify(jwtPublicKeyPushService, never()).pushPublicKey(any());
        verify(jwsService1, never()).storeKeyModel(any());
    }

    @Test
    void testGenerateJWTKeyPairAfterStartUpRetryFor3Times(){
        doReturn(new JwtPublicKeyPushResponse("failure",400)).when(jwtPublicKeyPushService).pushPublicKey(any());
        keyPairManager.generateJWTKeyPairAfterStartUp();
        verify(jwtPublicKeyPushService, times(3)).pushPublicKey(any());
        verify(jwsService1, never()).storeKeyModel(any());
    }

    @Test
    void testGenerateJWTKeyPairAfterStartUpHealthCheckInvertedAfterFailure(){
        setField(keyPairManager,"jwtKeyPairHealthCheckAtStartUp",true);
        doReturn(new JwtPublicKeyPushResponse("failure",400)).when(jwtPublicKeyPushService).pushPublicKey(any());
        ChassisBusinessException exception = assertThrows(ChassisBusinessException.class,()->keyPairManager.generateJWTKeyPairAfterStartUp());
        verify(jwtPublicKeyPushService, times(3)).pushPublicKey(any());
        verify(jwsService1, never()).storeKeyModel(any());
        assertFalse(keyPairManager.isKeyPairReady());
        assertEquals(JWT_KEY_PAIR_NOT_VALID, exception.getMessage());
    }

    @Test
    void testGetDelay() {
        setField(keyPairManager,"rotationInterval",150000);
        assertEquals(150000,keyPairManager.getDelay());
    }
}