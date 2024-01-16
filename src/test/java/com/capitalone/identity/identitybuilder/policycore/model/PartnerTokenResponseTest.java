package com.capitalone.identity.identitybuilder.policycore.model;

import org.junit.jupiter.api.Test;
import org.meanbean.test.BeanTester;
import org.meanbean.test.EqualsMethodTester;
import org.meanbean.test.HashCodeMethodTester;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = PartnerTokenResponse.class)
public class PartnerTokenResponseTest {

    @Test
    public void testSettersAndGetters() {
        new BeanTester().testBean(PartnerTokenResponse.class);
    }

    @Test
    public void testHashCode() {
        new HashCodeMethodTester().testHashCodeMethod(PartnerTokenResponse.class);
    }

    @Test
    public void testEquals() {
        new EqualsMethodTester().testEqualsMethod(PartnerTokenResponse.class);
    }
}
