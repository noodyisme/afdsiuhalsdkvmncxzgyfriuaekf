package com.capitalone.identity.identitybuilder.policycore.model;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.meanbean.test.BeanTester;
import org.meanbean.test.EqualsMethodTester;
import org.meanbean.test.HashCodeMethodTester;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.AbstractMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = ErrorInfo.class)
public class ErrorInfoTest {
    @Test
    public void testSettersAndGetters() {
        new BeanTester().testBean(ErrorInfo.class);
    }
    @Test
    public void testHashCode() {
    	new HashCodeMethodTester().testHashCodeMethod(ErrorInfo.class);
    }
    
    @Test
    public void testEquals() {
    	new EqualsMethodTester().testEqualsMethod(ErrorInfo.class);
    }

    @Test
    public void testMissingAdditionalDetails() throws JsonProcessingException {

        ErrorInfo errorInfo = new ErrorInfo("id", "text", "developerText");
        String errorInfoStr = new ObjectMapper().writeValueAsString(errorInfo);
        assertThat(errorInfoStr).doesNotContain("additionalDetails");
    }

    @Test
    public void testIncludeAdditionalDetails() throws JsonProcessingException {

        ErrorInfo errorInfo = new ErrorInfo("id", "text", "developerText",
                Stream.of(new AbstractMap.SimpleEntry<>("KEY1", "VALUE1"))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
        String errorInfoStr = new ObjectMapper().writeValueAsString(errorInfo);
        assertThat(errorInfoStr).contains("additionalDetails");
    }
}