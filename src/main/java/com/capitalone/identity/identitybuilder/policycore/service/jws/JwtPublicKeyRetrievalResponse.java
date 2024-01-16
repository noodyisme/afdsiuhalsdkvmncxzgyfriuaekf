package com.capitalone.identity.identitybuilder.policycore.service.jws;

import lombok.NonNull;
import lombok.Value;
import org.springframework.lang.Nullable;

@Value
public class JwtPublicKeyRetrievalResponse {
    @NonNull
    Integer status;
    @Nullable
    String jwk;
    @Nullable
    String errorMessage;
}