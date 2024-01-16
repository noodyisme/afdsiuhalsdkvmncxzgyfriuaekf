package com.capitalone.identity.identitybuilder.policycore.model;


import org.junit.jupiter.api.Test;
import org.meanbean.test.BeanTester;
import org.meanbean.test.EqualsMethodTester;
import org.meanbean.test.HashCodeMethodTester;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = PolicyMetadata.class)
public class PolicyMetadataTest {

    @Test
    public void testSettersAndGetters() {
        new BeanTester().testBean(PolicyMetadata.class);
    }
    
    @Test
    public void testHashCode() {
    	new HashCodeMethodTester().testHashCodeMethod(PolicyMetadata.class);
    }
    
    @Test
    public void testEquals() {
    	new EqualsMethodTester().testEqualsMethod(PolicyMetadata.class);
   }
}