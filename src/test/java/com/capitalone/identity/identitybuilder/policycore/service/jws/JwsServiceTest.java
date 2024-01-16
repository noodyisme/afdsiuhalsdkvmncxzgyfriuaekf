package com.capitalone.identity.identitybuilder.policycore.service.jws;

import com.capitalone.chassis.engine.model.exception.ChassisBusinessException;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.mapper.JwkValidatedEventMapper;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.model.event_models.JwkValidated_Publisher;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.Algorithm;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.Payload;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.text.ParseException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.capitalone.identity.identitybuilder.policycore.service.jws.JWTExtractionException.JwsExceptionType.JWS_INVALID_CLAIMS;
import static com.capitalone.identity.identitybuilder.policycore.service.jws.JWTExtractionException.JwsExceptionType.JWT_INVALID_KEY;
import static com.capitalone.identity.identitybuilder.policycore.service.jws.JWTExtractionException.JwsExceptionType.JWT_INVALID_SIGNATURE;
import static com.capitalone.identity.identitybuilder.policycore.service.jws.JWTExtractionException.JwsExceptionType.JWT_NULL_TOKEN;
import static com.capitalone.identity.identitybuilder.policycore.service.jws.JWTExtractionException.JwsExceptionType.JWT_PUBLIC_KEY_EXPIRED;
import static com.capitalone.identity.identitybuilder.policycore.service.jws.JwsService.ENV_NON_PROD;
import static com.capitalone.identity.identitybuilder.policycore.service.jws.JwsService.PRODUCT_ID;
import static java.time.ZoneOffset.UTC;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.util.ReflectionTestUtils.setField;

@ExtendWith(MockitoExtension.class)
class JwsServiceTest {

    public static final long TWO_SECONDS_OFFSET = 2L;
    final String JWK_KEY_JSON_STRING = "{\n" +
            "  \"productId\" : \"testProductId\",\n" +
            "  \"exp\" : 0,\n" +
            "  \"kid\" : \"kid123\",\n" +
            "  \"kty\" : \"RSA\",\n" +
            "  \"n\" : \"mQlFoHl1Xnm-DK_T1oDS_l4Quz3HvB7Y_bVnVISHqUMw1QEJIwg1xify8Hfrf06R7BL3t11X4RqYvlNS-_5sPmQBEIsF_-VjIKA3QcK6G4TOjxF7xB_4vjjtKtaMzh9Ttn3q5zxnnh4kVkHlq9WvKudRXAXbbCvyH-LenLeWhOSasKXQ9tlOzrdi03bz2p2qr5NCkwD3LTQkhsJgrUVSmDRy6KTjmg9rkoWOMBE1unTimwW72XpIvQMaudqNxth5N4n1jRt6ES0-Z-Tvh40h_Jlq1ttJYFWgSdF7mhR0xECwzn9cHZvazCS1ZwBduPGYNSbr7ZBuEpI_brvt2Wt9CQ\",\n" +
            "  \"e\" : \"AQAB\",\n" +
            "  \"crv\" : \"string\",\n" +
            "  \"x\" : \"string\",\n" +
            "  \"y\" : \"string\",\n" +
            "  \"use\" : \"sig\",\n" +
            "  \"alg\" : \"RS256\"\n" +
            "}";

    private static final String TOKEN_PAYLOAD_2 = "eyJraWQiOiJ2ZXJpZmllZC1zc28tdG9rZW4xIiwiYWxnIjoiUlMyNTYifQ.eyJzdWIiOiJyZDVHRGVENlwvTW45Sk9CWXVrdGdrTHpYUDNMdzVZejFRbFZublRtSjM5VTdQd1JNaHArTWw5T0dROXJjT2xFSkdEQ0RpT1FWaXJNamlSallZTEREZmc9PSIsImlzcyI6InZlcmlmaWVkLmNsb3VkZHF0LmNhcGl0YWxvbmUuY29tIiwicHJvZmlsZVJlZmVyZW5jZUlkM1BLZXkiOiJyZDVHRGVENlwvTW45Sk9CWXVrdGdrTHpYUDNMdzVZejFRbFZublRtSjM5VTdQd1JNaHArTWw5T0dROXJjT2xFSkdEQ0RpT1FWaXJNamlSallZTEREZmc9PSIsImN1c3RvbWVyQXBwRGF0YVJlZktleSI6ImM0MjRjMjNiLWRhNWQtNDUwYi1iMGEyLTk3N2IwNDMxYTNjMiIsImV4cCI6MTY0NzI5NzEwMSwiaWF0IjoxNjQ3Mjg5OTAxfQ.WMhXKZQVv5IuNJQ6heSFnAqHJMfceHN13vO7HUpTpZ_Erx8hs-ujmVUTfU1Q97woK930wi_ODzkJwX53NXv3jhc2GQ-NcT6d5MARCJJTkzWUdb3vzPePqJwied9P-rd9XFVRDIIMTrUe-jwDf6aeAYt5Al4vc3UaMiJYzP_eA2-hNkG9N7XLnh02nU1RclcHt9tDBypxtrOKLHsfIl9-s9ORGtMXbyvlrVx8KLu6PL40sWTS9NfwHKlboOEtZH1oIa95bFX9Y_KKsrIZxahgzAN6Kl_ON2z15PAVJQ0RUEmfpZkwOWa1dwpzxrzD6rOF5UcHgzGV2A5EVdKVdShzsw";

    private final JwtPublicKeyRetrievalService jwtPublicKeyRetrievalService1 = mock(JwtPublicKeyRetrievalService.class);
    private final JwkValidated_Publisher jwkValidatedPublisher = mock(JwkValidatedEventMapper.JwkValidationPublisher.class);
    private final JwsService jwsService1 = spy(new JwsService(jwtPublicKeyRetrievalService1, jwkValidatedPublisher));
    private RSAKey testKeyPair;
    private JwtKeyModel testKeyModel;
    private final String testProductId = "testProductId";
    private final String testKid = "testKid";
    private String testKeyPairKid = "testKeyPairKid";
    private long testKeyPairExp = Instant.now().plus(600, ChronoUnit.SECONDS).getEpochSecond();
    private String testTransformedPublicJWK;
    private JwtPublicKeyRetrievalResponse testJwtPublicKeyRetrievalResponse;

    private JwkValidated_Publisher jwkValidationPublisher1 = mock(JwkValidated_Publisher.class);

    @Test
    public void when_correct_jwk_passed_signature_is_valid() throws JOSEException, ParseException, NoSuchAlgorithmException, InvalidKeySpecException {
        RSAKey gatewayKeyPair = generateRSAKeyPair();
        RSAKey gatewayPublicKey = gatewayKeyPair.toPublicJWK();

        JWSObject customerIdToken = createJWS("{ a : b }", generateRSAKeyPair().getKeyID());
        signTheToken(gatewayKeyPair, customerIdToken);

        String customerIDTokenSigned = customerIdToken.serialize();
        String gatewayJWK = gatewayPublicKey.toJSONString();

        JwsService jwsService = dummyJwsService();
        assertTrue(jwsService.verifyTokenSignature(gatewayJWK, customerIDTokenSigned));
    }

    @NotNull
    private JwsService dummyJwsService() {
        return new JwsService(null, jwkValidatedPublisher);
    }

    @Test
    public void when_incorrect_jwk_passed_signature_is_not_valid() throws JOSEException {
        RSAKey gatewayKeyPair = generateRSAKeyPair();

        RSAKey wrongKeyPair = generateRSAKeyPair();

        RSAKey wrongKeyPairPublicKey = wrongKeyPair.toPublicJWK();

        JWSSigner gatewaySigner = new RSASSASigner(gatewayKeyPair);
        JWSObject customerIdToken = createJWS(TOKEN_PAYLOAD_2, generateRSAKeyPair().getKeyID());

        customerIdToken.sign(gatewaySigner);

        String customerIDTokenSigned = customerIdToken.serialize();
        String wrongJWK = wrongKeyPairPublicKey.toJSONString();

        JwsService jwsService = dummyJwsService();

        assertFalse(jwsService.verifyTokenSignature(wrongJWK, customerIDTokenSigned));
    }

    @Test
    public void when_customerIDToken_header_is_not_present_token_is_not_processed() {
        WebClient webClient = mock(WebClient.class);
        JwtPublicKeyRetrievalService jwtPublicKeyRetrievalService = mock(JwtPublicKeyRetrievalService.class);
        JwsService jwsService = spy(new JwsService(jwtPublicKeyRetrievalService, jwkValidatedPublisher));

        JWTExtractionException jwtExtractionException = assertThrows(JWTExtractionException.class, () -> jwsService.validate(null,null,false));
        assertEquals(JWT_NULL_TOKEN.toString(), jwtExtractionException.getMessage());
        verify(jwsService, never()).verifyTokenSignature(anyString(), anyString());
        verify(jwtPublicKeyRetrievalService, never()).obtainJWKFromExchange(anyString());
        verifyNoInteractions(webClient);
    }

    @Test
    public void when_kid_is_not_present_token_is_not_processed() throws JOSEException {
        WebClient webClient = mock(WebClient.class);
        JwtPublicKeyRetrievalService jwtPublicKeyRetrievalService = mock(JwtPublicKeyRetrievalService.class);
        JwsService jwsService = spy(new JwsService(jwtPublicKeyRetrievalService, jwkValidatedPublisher));
        setField(jwsService, "env", ENV_NON_PROD);

        RSAKey gatewayKeyPair = generateRSAKeyPair();

        JWSObject jwsToken = createJwtWithValidClaims(null);
        signTheToken(gatewayKeyPair, jwsToken);

        JWTExtractionException jwtExtractionException = assertThrows(JWTExtractionException.class, () -> jwsService.validate(jwsToken.serialize(),testProductId,false));
        assertEquals(JWT_INVALID_KEY.toString(), jwtExtractionException.getMessage());

        verify(jwsService, never()).verifyTokenSignature(anyString(), anyString());
        verify(jwtPublicKeyRetrievalService, never()).obtainJWKFromExchange(anyString());
    }

    @NotNull
    private JWSObject createJwtWithValidClaims(String kid) {
        String iat = String.valueOf(Instant.now().minusSeconds(60L).atZone(UTC).toEpochSecond());
        String exp = String.valueOf(Instant.now().plusSeconds(60L).atZone(UTC).toEpochSecond());
        return createJWS("{ \"iat\" : \"" + iat + "\", \"exp\" : \"" + exp + "\"}", kid);
    }

    @Test
    public void when_claims_are_not_present_token_is_not_processed() throws JOSEException {
        WebClient webClient = mock(WebClient.class);
        JwtPublicKeyRetrievalService jwtPublicKeyRetrievalService = mock(JwtPublicKeyRetrievalService.class);
        JwsService jwsService = spy(new JwsService(jwtPublicKeyRetrievalService, jwkValidatedPublisher));
        setField(jwsService, "env", ENV_NON_PROD);
        RSAKey gatewayKeyPair = generateRSAKeyPair();

        JWSObject jwsToken = createJWS("{ \"a\" : \"b\" }", null);
        signTheToken(gatewayKeyPair, jwsToken);

        JWTExtractionException jwtExtractionException = assertThrows(JWTExtractionException.class, () -> jwsService.validate(jwsToken.serialize(),testProductId,false));
        assertEquals(JWS_INVALID_CLAIMS.toString(), jwtExtractionException.getMessage());

        verify(jwsService, never()).verifyTokenSignature(anyString(), anyString());
        verify(jwtPublicKeyRetrievalService, never()).obtainJWKFromExchange(anyString());
    }

    @SneakyThrows
    @Test
    public void when_kid_is_present_it_will_be_verified() throws JOSEException {
        JwtPublicKeyRetrievalService jwtPublicKeyRetrievalService = mock(JwtPublicKeyRetrievalService.class);
        JwsService jwsService = spy(new JwsService(jwtPublicKeyRetrievalService, jwkValidatedPublisher));

        RSAKey gatewayKeyPair = testKeyPair;
        JWSObject aToken = createJwtWithValidClaims(gatewayKeyPair.getKeyID());
        signTheToken(gatewayKeyPair, aToken);
        doReturn(testJwtPublicKeyRetrievalResponse).when(jwtPublicKeyRetrievalService).fetchJwtPublicKey(anyString());
        setField(jwsService, "jwkValidatedPublisher", jwkValidationPublisher1);

        assertEquals(response().toString(), jwsService.validate(aToken.serialize(),testProductId,false).toString());

        verify(jwsService).verifyTokenSignature(anyString(), anyString());
    }

    @Test
    public void when_invalid_kid_is_provided_it_will_fail_validation() throws JOSEException {
        JwtPublicKeyRetrievalService jwtPublicKeyRetrievalService = mock(JwtPublicKeyRetrievalService.class);
        JwsService jwsService = spy(new JwsService(jwtPublicKeyRetrievalService, jwkValidatedPublisher));

        RSAKey gatewayKeyPair = generateRSAKeyPair();
        JWSObject customerIdToken = createJwtWithValidClaims(gatewayKeyPair.getKeyID());
        signTheToken(gatewayKeyPair, customerIdToken);

        doReturn(testJwtPublicKeyRetrievalResponse).when(jwtPublicKeyRetrievalService).fetchJwtPublicKey(anyString());
        setField(jwsService, "jwkValidatedPublisher", jwkValidationPublisher1);

        JWTExtractionException jwtExtractionException = assertThrows(JWTExtractionException.class, () -> jwsService.validate(customerIdToken.serialize(),testProductId,false));

        assertEquals(JWT_INVALID_SIGNATURE.toString(), jwtExtractionException.getMessage());

        verify(jwsService, times(1)).verifyTokenSignature(anyString(), anyString());
    }

    @SneakyThrows
    @Test
    public void when_kid_is_present_but_jwk_is_not_valid_exception_is_thrown() throws JOSEException {
        JwtPublicKeyRetrievalService jwtPublicKeyRetrievalService = mock(JwtPublicKeyRetrievalService.class);
        JwsService jwsService = spy(new JwsService(jwtPublicKeyRetrievalService, jwkValidatedPublisher));
        setField(jwsService, "env", ENV_NON_PROD);

        RSAKey gatewayKeyPair = generateRSAKeyPair();
        JWSObject aToken = createJwtWithValidClaims(gatewayKeyPair.getKeyID());
        signTheToken(gatewayKeyPair, aToken);

        doReturn(testJwtPublicKeyRetrievalResponse).when(jwtPublicKeyRetrievalService).fetchJwtPublicKey(anyString());
        setField(jwsService, "jwkValidatedPublisher", jwkValidationPublisher1);

        JWTExtractionException jwtExtractionException = assertThrows(JWTExtractionException.class, () -> jwsService.validate(aToken.serialize(),testProductId,false));

        assertEquals(JWT_INVALID_SIGNATURE.toString(), jwtExtractionException.getMessage());

        verify(jwsService).verifyTokenSignature(anyString(), anyString());
    }

    @Test
    void when_invalid_token_is_passed_to_extract_kid_exception_is_thrown() {
        JwtPublicKeyRetrievalService jwtPublicKeyRetrievalService = mock(JwtPublicKeyRetrievalService.class);
        JwsService jwsService = spy(new JwsService(jwtPublicKeyRetrievalService, jwkValidatedPublisher));

        assertEquals("JWTInvalidKeyResponse", jwsService.parseKeyId("not_a_token"));
    }

    @Test
    void when_invalid_jwk_is_passed_to_verify_signature_exception_is_thrown() {
        JwsService jwsService = dummyJwsService();
        assertFalse(jwsService.verifyTokenSignature("not_a_jwk", "assume_valid_token"));
    }

    @Test
    public void verifySignature2() throws JOSEException, ParseException, NoSuchAlgorithmException, InvalidKeySpecException {
        final String CUSTOMER_ID_TOKEN_JWT = "eyJraWQiOiJraWQxMjMiLCJhbGciOiJSUzI1NiJ9.ewogICAicHJvZmlsZVJlZmVyZW5jZUlkM1BLZXkiOiAicHJvZmlsZVJlZmVyZW5jZUlkM1BLZXkxIiwKICAgImN1cnJlbnRfdG9rZW5fbGV2ZWwiOiAiTGV2ZWwxIiwKICAgInN1YiI6ICJwcm9maWxlUmVmZXJlbmNlSWQzUEtleSIsCiAgICJpc3MiOiAidmVyaWZpZWQuY2xvdWRkcXQuY2FwaXRhbG9uZS5jb20iLAogICAiYXNzb2NpYXRlX2VpZCI6ICJ4eXoiLAogICAiZXhwIjogMTYwNTgyODk3NCwKICAgImlhdCI6IDE2MDU4MDAxNzQKfQ.Tm0oU81ZpSu89xnmDPgaDQ7NJXsGR_A001fpaaGv8UvnepRT8JgJrxdqvvI1HkuiJcFbq2t9O0ekFprnhk2Z2CSpue6XgSdeXM-rrcjD7Z_ji-BJUKtX3VuB3iJ1Njj2_pTFclqBTSQDgeF21592K1eUHeVtGTlFwcpDzwz_u7UMVm1FMcehbnWHe4rmshG1IMRx-FtNNitM7LMPIQgEIEVomN_WPwI-WHXHyWNACfVQHmlyoLyy8XyFRAHTFgNOp4dL5s8-hoTd9pavjm79rUQidU7dAyYBd_m3F5eDXXlo_mW1U_As6yaYtc6lHO1xQqDB6mcjtAmACiEUOosmaA";
        JwsService jwsService = dummyJwsService();
        assertTrue(jwsService.verifyTokenSignature(JWK_KEY_JSON_STRING, CUSTOMER_ID_TOKEN_JWT));
    }

    @NotNull
    private JWSObject createJWS(String payload, String kid) {
        if (null == kid) {
            return new JWSObject(new JWSHeader.Builder(JWSAlgorithm.RS256)
                    .build(), new Payload(payload));
        } else {
            return new JWSObject(new JWSHeader.Builder(JWSAlgorithm.RS256)
                    .keyID(kid).build(), new Payload(payload));
        }
    }

    private RSAKey generateRSAKeyPair() throws JOSEException {
        return new RSAKeyGenerator(2048)
                .keyID("kid123")
                .keyUse(KeyUse.SIGNATURE)
                .algorithm(new Algorithm("RS256"))
                .generate();
    }

    private void signTheToken(RSAKey rsaKeyPair, JWSObject jws) throws JOSEException {
        JWSSigner gatewaySigner = new RSASSASigner(rsaKeyPair);
        jws.sign(gatewaySigner);
    }

    String newToken = "eyJraWQiOiJ2ZXJpZmllZC1zc28tdG9rZW4xIiwiYWxnIjoiUlMyNTYifQ.eyJzdWIiOiJyZDVHRGVENlwvTW45Sk9CWXVrdGdrTHpYUDNMdzVZejFRbFZublRtSjM5VTdQd1JNaHArTWw5T0dROXJjT2xFSkdEQ0RpT1FWaXJNamlSallZTEREZmc9PSIsImlzcyI6InZlcmlmaWVkLmNsb3VkZHF0LmNhcGl0YWxvbmUuY29tIiwicHJvZmlsZVJlZmVyZW5jZUlkM1BLZXkiOiJyZDVHRGVENlwvTW45Sk9CWXVrdGdrTHpYUDNMdzVZejFRbFZublRtSjM5VTdQd1JNaHArTWw5T0dROXJjT2xFSkdEQ0RpT1FWaXJNamlSallZTEREZmc9PSIsImN1c3RvbWVyQXBwRGF0YVJlZktleSI6ImM0MjRjMjNiLWRhNWQtNDUwYi1iMGEyLTk3N2IwNDMxYTNjMiIsImV4cCI6MTY1ODQ5ODU0MiwiaWF0IjoxNjU4NDkxMzQyfQ.V9LpryBK0k8wqixH7-2pLKdMK95ooFFyOu5pjuwkduOveemCDKGcUw0jvX_fmnk1Xq5n4z171Fqq4GaljXQJ_kcCGE2oBYaimSAgFab4Yyyya235xvw29mwgWPmUwDA8J1aS52l-Ms_sf44n_ySeu5P4ijtflI0DrfOyTufv3s0HhsVZ6f4gcaNjIYyRqErX2QC52JP2qIEru3FFZphW2udcMSU7x6Qdcpx_JZkVEajQ9SYQIRFEBN2VogeTyD924fDpPEqTWdDQUjRmo0PC_dcAugKdSPz-6t6Ial5Qc_QRgjk80O8_CunfCdkvLcepJvScuioibxxiK6jx3GquGw";

    @Test
    public void testQAJwk() {

        String jwk = "{\n" +
                "    \"exp\": 1668286610,\n" +
                "    \"productId\": \"verified-sso\",\n" +
                "    \"kid\": \"verified-sso-token1\",\n" +
                "    \"kty\": \"RSA\",\n" +
                "    \"clientIds\": [\n" +
                "        \"*\"\n" +
                "    ],\n" +
                "    \"n\": \"tgOmR_RmRf7NlH3uKAKuqQ1Po_EasyzZFBn-zZbyx1uZDufl1clnJkwDN_HcoH1hGIZcpOR2va74tenmIFWx2rYrBs8EoaLQyR6mjvPhtNr8Y44gpSNU57ocyWVv1K1ltpGr2ak43LjUUqS32Ovz29xGWSbWvLITP2_BXvy_gRDJF1dJYv3o6JF_INkhw7GpbesyWwl-TZMfmOcYVpSkRAHvHF_KqbsreBixfocmhf9qNrPyFBaleMV_-ZcxhERT6qhVbUc8KpfYn9gjpn_3tMF4Qz1fiAOmFVMohzMjkBzZA2sBq0E7XGlrPSlrpKBDVHWp18VgwjCk9E9CfgMZqw\",\n" +
                "    \"use\": \"enc\",\n" +
                "    \"e\": \"AQAB\"\n" +
                "}";

        new JwsService(new JwtPublicKeyRetrievalService(WebClient.create()), jwkValidatedPublisher).verifyTokenSignature(jwk, newToken);
    }

    @Test
    public void testGetPayload() throws JsonProcessingException {
        JwsService jwsService = new JwsService(new JwtPublicKeyRetrievalService(WebClient.create()), jwkValidatedPublisher);
        String jwtPayload = jwsService.getJwtPayload(newToken);
        Map<String, String> payloadAsMap = jwsService.getPayloadAsMap(jwtPayload);
        assertNotNull(payloadAsMap);
        assertEquals(6, payloadAsMap.size());
        assertEquals("verified.clouddqt.capitalone.com", payloadAsMap.get("iss"));
    }

    @Test
    public void testExtractTokenFields() {
        JwsService jwsService = new JwsService(new JwtPublicKeyRetrievalService(WebClient.create()), jwkValidatedPublisher);
        Map<String, String> completePayload = jwsService.extractFields(newToken, Collections.EMPTY_LIST);
        assertNotNull(completePayload);
        assertEquals(6, completePayload.size());
        assertEquals("verified.clouddqt.capitalone.com", completePayload.get("iss"));

        Map<String, String> partialPayload = jwsService.extractFields(newToken, Arrays.asList("iss", "customerAppDataRefKey"));
        assertNotNull(partialPayload);
        assertEquals(2, partialPayload.size());
        assertEquals("c424c23b-da5d-450b-b0a2-977b0431a3c2", partialPayload.get("customerAppDataRefKey"));
    }

    public JwsServiceValidationResponse response() {
        return new JwsServiceValidationResponse();
    }

    @Test
    public void testWhenIssuedAHappenedBeforeNowThenClaimIsValid(){
        JwsService jwsService = new JwsService(new JwtPublicKeyRetrievalService(WebClient.create()), jwkValidatedPublisher);
        assertTrue(jwsService.validateIssuedAt(String.valueOf(Instant.now().minusSeconds(TWO_SECONDS_OFFSET).atZone(UTC).toEpochSecond())));
    }

    @Test
    public void testWhenIssuedAHappensAfterNowThenClaimIsInvalid(){
        JwsService jwsService = new JwsService(new JwtPublicKeyRetrievalService(WebClient.create()), jwkValidatedPublisher);
        assertFalse(jwsService.validateIssuedAt(String.valueOf(Instant.now().plusSeconds(TWO_SECONDS_OFFSET).atZone(UTC).toEpochSecond())));
    }

    @Test
    public void testExpiredAtHappensAfterNowThenClaimIsValid(){
        JwsService jwsService = new JwsService(new JwtPublicKeyRetrievalService(WebClient.create()), jwkValidatedPublisher);
        assertTrue(jwsService.validateExp(String.valueOf(Instant.now().plusSeconds(TWO_SECONDS_OFFSET).atZone(UTC).toEpochSecond())));
    }

    @Test
    public void testExpiredAtHappensBeforeNowThenClaimIsInvalid(){
        JwsService jwsService = new JwsService(new JwtPublicKeyRetrievalService(WebClient.create()), jwkValidatedPublisher);
        assertFalse(jwsService.validateExp(String.valueOf(Instant.now().minusSeconds(TWO_SECONDS_OFFSET).atZone(UTC).toEpochSecond())));
    }

    @Test
    public void testExpiredJWK(){

        Map<String, String> testKidToJWK = new ConcurrentHashMap<>();
        testKidToJWK.put(testKeyPairKid,JWK_KEY_JSON_STRING);
        jwsService1.storeKeyModel(testKeyModel);
        String token = jwsService1.generate("testProcessId",null,"300");
        setField(jwsService1,"kidToJWK",testKidToJWK);
        JWTExtractionException jwtExtractionException = assertThrows(JWTExtractionException.class, () -> jwsService1.validate(token,testProductId,false));
        assertEquals(JWT_PUBLIC_KEY_EXPIRED.toString(), jwtExtractionException.getMessage());
    }

    @SneakyThrows
    @BeforeEach
    void setUp() {
        testKeyPair = new RSAKeyGenerator(2048)
                .keyUse(KeyUse.SIGNATURE)
                .keyID(testKeyPairKid)
                .algorithm(new Algorithm("RS256"))
                .generate();

        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, String> map = objectMapper.readValue(testKeyPair.toPublicJWK().toJSONString(), new TypeReference<Map<String, String>>() {});
        map.put("exp", String.valueOf(Instant.now().getEpochSecond()+600));
        map.put(PRODUCT_ID, testProductId);
        testTransformedPublicJWK = objectMapper.writeValueAsString(map);
        testJwtPublicKeyRetrievalResponse = new JwtPublicKeyRetrievalResponse(200,testTransformedPublicJWK,"test");
        testKeyModel = JwtKeyModel.builder()
                .jwtKeyPair(testKeyPair)
                .keyPairExp(testKeyPairExp)
                .transformedPublicJWK(testTransformedPublicJWK)
                .keyPairKid(testKeyPairKid)
                .build();
        setField(jwsService1, "jwkValidatedPublisher", jwkValidationPublisher1);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testStoreJwkToMap(){
        Map<String, String> kidToJWK = (Map<String,String>) ReflectionTestUtils.getField(jwsService1, "kidToJWK");
        jwsService1.storeJwkToMap("testKid","testJwk");
        assertNotNull(kidToJWK);
        assertEquals("testJwk",kidToJWK.get("testKid"));
    }

    @Test
    public void testStoreKeyModel(){
        jwsService1.storeKeyModel(testKeyModel);
        JwtKeyModel keyModel = (JwtKeyModel) ReflectionTestUtils.getField(jwsService1, "jwtKeyModel");
        assertNotNull(keyModel);
        assertEquals("testKeyPairKid",keyModel.getKeyPairKid());
    }

    @Test
    public void testGenerateSuccessfulCall() throws JsonProcessingException {
        jwsService1.storeKeyModel(testKeyModel);
        String customPayloadRaw = "{\"customPayloadKey1\":\"customPayloadVal1\",\"customPayloadKey2\":\"customPayloadVal2\"}";
        TypeReference<HashMap<String,String>> typeRef
                = new TypeReference<HashMap<String,String>>() {};
        Map<String,String> customPayload = new ObjectMapper().readValue(customPayloadRaw, typeRef);
        String token = jwsService1.generate("testProcessId",customPayload,"0");
        assertNotNull(token);
    }

    @Test
    void testGenerateExpirationBelowMin(){
        jwsService1.storeKeyModel(testKeyModel);
        String token = jwsService1.generate("testProcessId",null,"59");
        doReturn(testJwtPublicKeyRetrievalResponse).when(jwtPublicKeyRetrievalService1).fetchJwtPublicKey(anyString());
        assertTrue(jwsService1.validate(token,testProductId,true).result());

        Map<String, String> fields = jwsService1.extractFields(token,Collections.emptyList());
        long issuedAt = Long.parseLong(fields.get("iat"));
        long expectedExpiration = issuedAt + 60;
        long actualExpiration = Long.parseLong(fields.get("exp"));
        assertEquals(expectedExpiration, actualExpiration);
    }

    @Test
    void testGenerateExpirationAboveMax(){
        jwsService1.storeKeyModel(testKeyModel);
        String token = jwsService1.generate("testProcessId",null,"7201");
        doReturn(testJwtPublicKeyRetrievalResponse).when(jwtPublicKeyRetrievalService1).fetchJwtPublicKey(anyString());
        assertTrue(jwsService1.validate(token,testProductId,true).result());

        Map<String, String> fields = jwsService1.extractFields(token,Collections.emptyList());
        long issuedAt = Long.parseLong(fields.get("iat"));
        long expectedExpiration = issuedAt + 7200;
        long actualExpiration = Long.parseLong(fields.get("exp"));
        assertEquals(expectedExpiration, actualExpiration);
    }

    @Test
    public void testGenerateKeyModelNotValid(){
        ChassisBusinessException error1 = assertThrows(ChassisBusinessException.class, () -> jwsService1.generate("testProcessId",null,null));
        assertEquals("JWT Key Pair is not valid",error1.getMessage());
    }

    @Test
    public void testGenerateAndValidate(){
        jwsService1.storeKeyModel(testKeyModel);
        String token = jwsService1.generate("testProcessId",null,"300");
        doReturn(testJwtPublicKeyRetrievalResponse).when(jwtPublicKeyRetrievalService1).fetchJwtPublicKey(anyString());
        assertTrue(jwsService1.validate(token,testProductId,true).result());
    }


    @Test
    public void testMisMatchProductIdValidationWithApplicationLevelEnabledAndQueryParamEnabled(){
        jwsService1.storeKeyModel(testKeyModel);
        setField(jwsService1, "productIdValidationEnabledAtApplicationLevel", true);
        String token = jwsService1.generate("testProcessId",null,"9999");
        doReturn(testJwtPublicKeyRetrievalResponse).when(jwtPublicKeyRetrievalService1).fetchJwtPublicKey(anyString());
        assertThrows(JWTExtractionException.class,()->jwsService1.validate(token,"wrongTestProductId",true).result());
    }

    @Test
    public void testMisMatchProductIdValidationWithApplicationLevelDisabledAndQueryParamEnabled(){
        jwsService1.storeKeyModel(testKeyModel);
        setField(jwsService1, "productIdValidationEnabledAtApplicationLevel", false);
        String token = jwsService1.generate("testProcessId",null,"9999");
        doReturn(testJwtPublicKeyRetrievalResponse).when(jwtPublicKeyRetrievalService1).fetchJwtPublicKey(anyString());
        assertTrue(jwsService1.validate(token,"wrongTestProductId",true).result());
    }

    @Test
    public void testMisMatchProductIdValidationWithApplicationLevelEnabledAndQueryParamDisabled(){
        jwsService1.storeKeyModel(testKeyModel);
        setField(jwsService1, "productIdValidationEnabledAtApplicationLevel", true);
        String token = jwsService1.generate("testProcessId",null,"9999");
        doReturn(testJwtPublicKeyRetrievalResponse).when(jwtPublicKeyRetrievalService1).fetchJwtPublicKey(anyString());
        assertTrue(jwsService1.validate(token,"wrongTestProductId",false).result());
    }

    @Test
    public void testMisMatchProductIdValidationWithApplicationLevelDisabledAndQueryParamDisabled(){
        jwsService1.storeKeyModel(testKeyModel);
        setField(jwsService1, "productIdValidationEnabledAtApplicationLevel", false);
        String token = jwsService1.generate("testProcessId",null,"9999");
        doReturn(testJwtPublicKeyRetrievalResponse).when(jwtPublicKeyRetrievalService1).fetchJwtPublicKey(anyString());
        assertTrue(jwsService1.validate(token,"wrongTestProductId",false).result());
    }
}