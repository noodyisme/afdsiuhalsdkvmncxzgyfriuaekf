package com.capitalone.identity.identitybuilder.policycore.service.jws;

import lombok.AllArgsConstructor;
import lombok.Value;
import lombok.experimental.Accessors;

@Value
@AllArgsConstructor
public class JwsServiceValidationResponse {

    public JwsServiceValidationResponse() {
        this.result = true;
    }

    @Accessors(fluent = true)
    boolean result;

    @Override
    public String toString() {
        return String.valueOf(this.result);
    }
}