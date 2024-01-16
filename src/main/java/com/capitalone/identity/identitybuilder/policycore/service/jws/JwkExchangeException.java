package com.capitalone.identity.identitybuilder.policycore.service.jws;

import lombok.Value;

@Value
public class JwkExchangeException extends RuntimeException {
    int httpStatusCode;

    public JwkExchangeException(String message, int httpStatusCode) {
        super(message);
        this.httpStatusCode = httpStatusCode;
    }
}
