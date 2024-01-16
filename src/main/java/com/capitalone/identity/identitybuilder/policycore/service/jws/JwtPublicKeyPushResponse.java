package com.capitalone.identity.identitybuilder.policycore.service.jws;

import lombok.Value;

@Value
public class JwtPublicKeyPushResponse {
    String pushResponse;
    int rawHttpStatusCode;
}
