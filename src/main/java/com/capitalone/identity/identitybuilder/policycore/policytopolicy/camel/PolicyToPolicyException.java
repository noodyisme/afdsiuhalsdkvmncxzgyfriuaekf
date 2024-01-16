package com.capitalone.identity.identitybuilder.policycore.policytopolicy.camel;

public class PolicyToPolicyException extends RuntimeException {

    public PolicyToPolicyException(final String message) {
        super(message);
    }

    PolicyToPolicyException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
