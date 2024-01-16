package com.capitalone.identity.identitybuilder.policycore.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PolicyResponseTest {

    @Test
    public void testAllArgConstructor() {
        Object results = new Object();
        PolicyStatus policyStatus = PolicyStatus.SUCCESS;
        ErrorInfo errorInfo = new ErrorInfo();
        ProcessMetadata metadata = new ProcessMetadata();
        ErrorInfo suppressedErrorInfo = new ErrorInfo();
        PolicyResponse policyResponse = new PolicyResponse(results, policyStatus, errorInfo, metadata, suppressedErrorInfo);
        assertSame(results, policyResponse.getResults());
        assertSame(policyStatus, policyResponse.getPolicyStatus());
        assertSame(errorInfo, policyResponse.getErrorInfo());
        assertSame(metadata, policyResponse.getMetadata());
    }
}