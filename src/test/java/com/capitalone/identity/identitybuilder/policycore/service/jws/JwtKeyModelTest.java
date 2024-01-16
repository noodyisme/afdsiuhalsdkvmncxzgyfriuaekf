package com.capitalone.identity.identitybuilder.policycore.service.jws;

import com.nimbusds.jose.Algorithm;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


class JwtKeyModelTest {

    @Test
    void testSettersAndGetters() throws JOSEException {
        RSAKey keyPair = new RSAKeyGenerator(2048)
                .keyUse(KeyUse.SIGNATURE)
                .keyID("testKid")
                .algorithm(new Algorithm("RS256"))
                .generate();
        JwtKeyModel keyModel = JwtKeyModel.builder()
                .jwtKeyPair(keyPair)
                .transformedPublicJWK("transformedPublicJWK")
                .keyPairExp(720)
                .keyPairKid("testKid")
                .build();
        Assertions.assertEquals("testKid", keyModel.getKeyPairKid());
        Assertions.assertEquals(keyPair, keyModel.getJwtKeyPair());
        Assertions.assertEquals("transformedPublicJWK", keyModel.getTransformedPublicJWK());
        Assertions.assertEquals(720, keyModel.getKeyPairExp());
    }
}