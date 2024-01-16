package com.capitalone.identity.identitybuilder.policycore.model;

import org.junit.jupiter.api.Test;
import org.meanbean.test.BeanTester;
import org.meanbean.test.EqualsMethodTester;
import org.meanbean.test.HashCodeMethodTester;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertSame;

@SpringBootTest(classes = ExecutePolicyRequest.class)
public class ExecutePolicyRequestTest {

    @Test
    public void testSettersAndGetters() {
        new BeanTester().testBean(ExecutePolicyRequest.class);
    }
    @Test
    public void testHashCode() {
    	new HashCodeMethodTester().testHashCodeMethod(ExecutePolicyRequest.class);
    }
    
    @Test
    public void testEquals() {
    	new EqualsMethodTester().testEqualsMethod(ExecutePolicyRequest.class);
    }
    
    @Test
    public void testAllArgsConstructor() {
    	Map<String, Serializable> map = new HashMap<>();
    	ExecutePolicyRequest req = new ExecutePolicyRequest(map,"maskedData");
    	assertSame(map, req.getPolicyParameters());
    	assertSame("maskedData", req.getPolicyParametersAudit());
    }

}
