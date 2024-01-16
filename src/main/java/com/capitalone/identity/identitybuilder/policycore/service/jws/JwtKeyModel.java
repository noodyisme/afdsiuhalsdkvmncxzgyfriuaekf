package com.capitalone.identity.identitybuilder.policycore.service.jws;

import com.nimbusds.jose.jwk.RSAKey;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class JwtKeyModel {
    RSAKey jwtKeyPair;
    String transformedPublicJWK;
    String keyPairKid;
    long keyPairExp;

}
