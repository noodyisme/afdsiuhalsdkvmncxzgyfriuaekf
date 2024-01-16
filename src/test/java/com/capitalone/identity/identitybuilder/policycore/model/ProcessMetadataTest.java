package com.capitalone.identity.identitybuilder.policycore.model;

import org.junit.jupiter.api.Test;
import org.meanbean.lang.EquivalentFactory;
import org.meanbean.lang.Factory;
import org.meanbean.test.*;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;

@SpringBootTest(classes = ProcessMetadata.class)
public class ProcessMetadataTest {
	
	private static final LocalDateTime FIXED_DATE_TIME = LocalDateTime.now();
	
    private static class DateTimeFactory implements Factory<LocalDateTime> {
        @Override
        public LocalDateTime create() {
            return LocalDateTime.now().plusMinutes((int) (Math.random() * 100));
        }
    }
    
    private static class MetaDataFactory implements EquivalentFactory<ProcessMetadata> {
		@Override
		public ProcessMetadata create() {
            return new ProcessMetadata("resourceId", "policyName", "1.0", FIXED_DATE_TIME.plusDays(0), new HashSet<>(), "step2", new ArrayList<>());
		}
    }
    
    // The tester needs to know how to make a LocalDateTime.
    private final Configuration configuration = new ConfigurationBuilder().overrideFactory("stepExpiration", new DateTimeFactory()).build();

    @Test
    public void testSettersAndGetters() {
        new BeanTester().testBean(ProcessMetadata.class, configuration);
    }
    
    @Test
    public void testHashCode() {
    	new HashCodeMethodTester().testHashCodeMethod(new MetaDataFactory());
    }
    
    @Test
    public void testEquals() {
    	new EqualsMethodTester().testEqualsMethod(new MetaDataFactory(), configuration);
    }
}