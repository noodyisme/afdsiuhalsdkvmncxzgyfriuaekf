package com.capitalone.identity.identitybuilder.policycore.service.exception;


import com.capitalone.identity.identitybuilder.policycore.model.ErrorInfo;

public class ForwardedDownstreamException extends CustomPolicyException {
    private static final long serialVersionUID = -1L;

    public ForwardedDownstreamException(final ErrorInfo errorInfo) {
        super(errorInfo);
    }

    public ForwardedDownstreamException(final String message) {
        super(message);
    }
}