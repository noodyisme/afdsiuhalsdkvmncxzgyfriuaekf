package com.capitalone.identity.identitybuilder.policycore.camel.external;

public class InvalidItemDefinitionException extends RuntimeException {

    public InvalidItemDefinitionException(String message) {
        super(message);
    }

    public InvalidItemDefinitionException(String message, Throwable error) {
        super(message, error);
    }

}
