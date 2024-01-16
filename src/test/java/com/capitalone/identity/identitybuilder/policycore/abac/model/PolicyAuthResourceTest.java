package com.capitalone.identity.identitybuilder.policycore.abac.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class PolicyAuthResourceTest {

    @Test
    void validFieldsTest() {
        PolicyAuthResource policyAuthResource = new PolicyAuthResource("sample", "1.0");
        assertEquals(1, policyAuthResource.getPolicyMajorVersion());
    }

    @Test
    void inValidFieldsTest() {
        PolicyAuthResource policyAuthResource = new PolicyAuthResource("sample", "");
        assertEquals(0, policyAuthResource.getPolicyMajorVersion());
    }

    @Test
    void nullFieldsTest() {
        assertThrows(NullPointerException.class, () -> {
            PolicyAuthResource policyAuthResource = new PolicyAuthResource(null, null);
        });
    }
}
