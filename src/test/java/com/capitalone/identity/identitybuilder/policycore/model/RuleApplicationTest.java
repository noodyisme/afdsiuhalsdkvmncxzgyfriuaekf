package com.capitalone.identity.identitybuilder.policycore.model;


import com.capitalone.identity.identitybuilder.policycore.model.RuleApplication.Result;
import org.junit.jupiter.api.Test;
import org.meanbean.test.BeanTester;
import org.meanbean.test.EqualsMethodTester;
import org.meanbean.test.HashCodeMethodTester;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertSame;

@SpringBootTest(classes = RuleApplication.class)
public class RuleApplicationTest {

    @Test
    public void testSettersAndGetters() {
        new BeanTester().testBean(RuleApplication.class);
    }
    @Test
    public void testHashCode() {
    	new HashCodeMethodTester().testHashCodeMethod(RuleApplication.class);
    }
    
    @Test
    public void testEquals() {
    	new EqualsMethodTester().testEqualsMethod(RuleApplication.class);
    }
    
    @Test
    public void testAllArgsConstructor() {
    	String name = "name";
    	Result result = Result.FAILED;
    	String reason = "reason";
    	RuleApplication obj = new RuleApplication(name, result, reason);
    	assertSame(name, obj.getRuleName());
    	assertSame(result, obj.getResult());
    	assertSame(reason, obj.getReason());
    }
}