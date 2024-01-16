package com.capitalone.identity.identitybuilder.policycore.decisionengine.exception;

/**
 * @deprecated Use {@link com.capitalone.identity.identitybuilder.decisionengine.service.exception.DmnEvaluateException}
 * instead for future cases. Kept here only to support legacy camel routes with direct references.
 */
@Deprecated
public class DmnEvaluateException extends RuntimeException {

    public DmnEvaluateException(final String message) {
        super(message);
    }

    public DmnEvaluateException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
