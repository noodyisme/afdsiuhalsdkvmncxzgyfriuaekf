package com.capitalone.identity.identitybuilder.policycore.service.jws;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import static org.mockito.Mockito.*;

class JwtPublicKeyRetrievalServiceTest {
    private JwtPublicKeyRetrievalService jwtPublicKeyRetrievalService = spy(new JwtPublicKeyRetrievalService(WebClient.create()));

    @Test
    void jwtPublicKeyRetrievalHelperSuccessfulTest(){
        doReturn("test").when(jwtPublicKeyRetrievalService).obtainJWKFromExchange(any());
        Assertions.assertNotNull(jwtPublicKeyRetrievalService.fetchJwtPublicKey("abc").getJwk());
    }

    @Test
    void jwtPublicKeyRetrievalHelperFailureTest(){
        doThrow(JwkExchangeException.class).when(jwtPublicKeyRetrievalService).obtainJWKFromExchange(any());
        Assertions.assertNull(jwtPublicKeyRetrievalService.fetchJwtPublicKey("abc").getJwk());
    }

}