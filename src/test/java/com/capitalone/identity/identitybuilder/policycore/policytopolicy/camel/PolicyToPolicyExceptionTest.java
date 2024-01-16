package com.capitalone.identity.identitybuilder.policycore.policytopolicy.camel;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class PolicyToPolicyExceptionTest {

    @Test
    void testConstructorSingleArg(){
        String error = "error";
        PolicyToPolicyException policyToPolicyException = new PolicyToPolicyException(error);
        assertEquals(error, policyToPolicyException.getMessage());
        assertNull(policyToPolicyException.getCause());
    }

    @Test
    void testConstructorTwoArgs(){
        String error = "error";
        Throwable throwable = new Throwable("throwable");
        PolicyToPolicyException policyToPolicyException = new PolicyToPolicyException(error, throwable);
        assertEquals(error, policyToPolicyException.getMessage());
        assertEquals(throwable, policyToPolicyException.getCause());
    }

}