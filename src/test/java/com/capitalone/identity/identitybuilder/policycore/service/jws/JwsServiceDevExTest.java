package com.capitalone.identity.identitybuilder.policycore.service.jws;

import com.capitalone.identity.identitybuilder.policycore.operational_audit.mapper.JwkValidatedEventMapper;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.model.event_models.JwkValidated_Publisher;
import com.capitalone.identity.identitybuilder.policycore.service.MockServerInitializer;
import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.web.reactive.function.client.WebClient;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.TestInstance.Lifecycle;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ContextConfiguration(initializers = {MockServerInitializer.class})
@TestInstance(Lifecycle.PER_CLASS)
public class JwsServiceDevExTest {

    private static final String devExUrl = "http://localhost:9123";
    public static final String UNAUTHENTICATED = "Unauthenticated";
    public static final String UNAUTHORIZED = "Unauthorized";
    public static final String KEY_STORED = "Key stored";
    public static final String CONFLICT = "Key id kid321 already exists";

    String kid_123 = "kid123";
    String kid_404 = "kid404";
    String kid_500 = "kid500";
    String kid_401 = "kid401";
    String kid_403 = "kid403";
    String kid_321 = "kid321";

    @Autowired
    private ApplicationEventPublisher publisher;

    private final JwkValidated_Publisher jwkValidatedPublisher = new JwkValidatedEventMapper.JwkValidationPublisher(null);


    @Autowired
    WireMockServer wireMock;

    @BeforeAll
    void init() {
        wireMock
                .givenThat(get(urlEqualTo("/developer-platform/key-management/certificates/keys/" + kid_123))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("a_valid_token")));

        wireMock
                .givenThat(get(urlEqualTo("/developer-platform/key-management/certificates/keys/" + kid_404))
                        .willReturn(aResponse()
                                .withHeader("Content-Type", "application/json")
                                .withBody("Not found")
                                .withStatus(404)));

        wireMock
                .givenThat(get(urlEqualTo("/developer-platform/key-management/certificates/keys/" + kid_500))
                        .willReturn(aResponse()
                                .withHeader("Content-Type", "application/json")
                                .withBody("Server error")
                                .withStatus(500)));

        wireMock
                .givenThat(get(urlEqualTo("/developer-platform/key-management/certificates/keys/" + kid_401))
                        .willReturn(aResponse()
                                .withHeader("Content-Type", "application/json")
                                .withBody(UNAUTHENTICATED)
                                .withStatus(401)));

        wireMock
                .givenThat(get(urlEqualTo("/developer-platform/key-management/certificates/keys/" + kid_403))
                        .willReturn(aResponse()
                                .withHeader("Content-Type", "application/json")
                                .withBody(UNAUTHORIZED)
                                .withStatus(403)));

        wireMock
                .givenThat(put(urlEqualTo("/developer-platform/key-management/certificates/keys/" + kid_123))
                        .willReturn(aResponse()
                                .withHeader("Content-Type", "application/json")
                                .withBody(KEY_STORED)
                                .withStatus(200)));

        wireMock
                .givenThat(put(urlEqualTo("/developer-platform/key-management/certificates/keys/" + kid_321))
                        .willReturn(aResponse()
                                .withHeader("Content-Type", "application/json")
                                .withBody(CONFLICT)
                                .withStatus(409)));
    }

    @Test
    public void when_valid_kid_requested_client_should_get_a_token() {
        JwtPublicKeyRetrievalService jwtPublicKeyRetrievalService = new JwtPublicKeyRetrievalService(WebClient.create("http://127.0.0.1:9123"));
        JwsService jwsService = new JwsService(jwtPublicKeyRetrievalService, jwkValidatedPublisher);
        String kid123 = assertDoesNotThrow(() -> jwtPublicKeyRetrievalService.obtainJWKFromExchange(kid_123));
        assertEquals("a_valid_token", kid123);
    }

    @Test
    public void when_invalid_kid_requested_client_should_get_an_error() {
        JwtPublicKeyRetrievalService jwtPublicKeyRetrievalService = new JwtPublicKeyRetrievalService(WebClient.create("http://127.0.0.1:9123"));
        JwsService jwsService = new JwsService(jwtPublicKeyRetrievalService, jwkValidatedPublisher);
        JwkExchangeException exception = assertThrows(JwkExchangeException.class, () -> jwtPublicKeyRetrievalService.obtainJWKFromExchange(kid_404));
        assertEquals(String.format(JwsService.KID_IS_NOT_AVAILABLE_SERVER_RESPONSE,kid_404, "Not Found:404"), exception.getMessage());
    }

    @Test
    public void when_server_throws_error() {
        JwtPublicKeyRetrievalService jwtPublicKeyRetrievalService = new JwtPublicKeyRetrievalService(WebClient.create("http://127.0.0.1:9123"));
        JwsService jwsService = new JwsService(jwtPublicKeyRetrievalService, jwkValidatedPublisher);
        JwkExchangeException exception = assertThrows(JwkExchangeException.class, () -> jwtPublicKeyRetrievalService.obtainJWKFromExchange(kid_500));
        assertTrue(exception.getMessage().startsWith(
                String.format(JwsService.SERVER_ERROR, kid_500, "org.springframework.web")));
    }

    @Test
    public void when_client_is_not_authenticated() {
        JwtPublicKeyRetrievalService jwtPublicKeyRetrievalService = new JwtPublicKeyRetrievalService(WebClient.create("http://127.0.0.1:9123"));
        JwsService jwsService = new JwsService(jwtPublicKeyRetrievalService, jwkValidatedPublisher);
        JwkExchangeException exception = assertThrows(JwkExchangeException.class, () -> jwtPublicKeyRetrievalService.obtainJWKFromExchange(kid_401));
        assertEquals("Request for kid401 failed due to authentication or authorization. Server response: Unauthorized:401", exception.getMessage());
    }

    @Test
    public void when_client_is_not_authorized() {
        JwtPublicKeyRetrievalService jwtPublicKeyRetrievalService = new JwtPublicKeyRetrievalService(WebClient.create("http://127.0.0.1:9123"));
        JwsService jwsService = new JwsService(jwtPublicKeyRetrievalService, jwkValidatedPublisher);
        JwkExchangeException exception = assertThrows(JwkExchangeException.class, () -> jwtPublicKeyRetrievalService.obtainJWKFromExchange(kid_403));
        assertEquals(String.format(JwsService.AUTHENTICATION_OR_AUTHORIZATION_SERVER_RESPONSE, kid_403, "Forbidden:403" ), exception.getMessage());
    }

    @Test
    public void jwtPublicKeyPushServiceSuccessful(){
        JwsService jwsService = new JwsService(new JwtPublicKeyRetrievalService(WebClient.create("http://127.0.0.1:9123")), jwkValidatedPublisher);
        JwtPublicKeyPushService jwtPublicKeyPushService = new JwtPublicKeyPushService(WebClient.create("http://127.0.0.1:9123"),jwsService);
        JwtKeyModel keyModel = JwtKeyModel.builder()
                .jwtKeyPair(null)
                .keyPairExp(10000)
                .transformedPublicJWK("testTransformedPublicJWK")
                .keyPairKid(kid_123)
                .build();
        JwtPublicKeyPushResponse jwtPublicKeyPushResponse = jwtPublicKeyPushService.pushPublicKey(keyModel);
        assertEquals(200,jwtPublicKeyPushResponse.getRawHttpStatusCode());
    }

    @Test
    public void jwtPublicKeyPushServiceFailureResponse(){
        JwsService jwsService = new JwsService(new JwtPublicKeyRetrievalService(WebClient.create("http://127.0.0.1:9123")), jwkValidatedPublisher);
        JwtPublicKeyPushService jwtPublicKeyPushService = new JwtPublicKeyPushService(WebClient.create("http://127.0.0.1:9123"),jwsService);
        JwtKeyModel keyModel = JwtKeyModel.builder()
                .jwtKeyPair(null)
                .keyPairExp(10000)
                .transformedPublicJWK("testTransformedPublicJWK")
                .keyPairKid(kid_321)
                .build();
        JwtPublicKeyPushResponse jwtPublicKeyPushResponse = jwtPublicKeyPushService.pushPublicKey(keyModel);
        assertEquals(409,jwtPublicKeyPushResponse.getRawHttpStatusCode());
    }

    @Configuration
    static class JwsServiceContextConfig { }
}



