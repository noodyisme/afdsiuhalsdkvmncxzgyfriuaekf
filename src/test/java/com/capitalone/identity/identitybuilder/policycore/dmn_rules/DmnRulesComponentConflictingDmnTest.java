package com.capitalone.identity.identitybuilder.policycore.dmn_rules;

import com.capitalone.identity.identitybuilder.policycore.camel.PolicyConstants;
import org.apache.camel.CamelContext;
import org.apache.camel.EndpointInject;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;


/**
 * ********************* TEST SETUP ******************
 * Specification:
 * 1. Two different policies can have .dmn files with the same name and they won't conflict/overwrite
 * 2. A policy can have two .dmn files that are copy-paste of one another and there won't be
 * any conflicts caused by the files having identical namespace attributes internally.
 * <p>
 * Key test features:
 * 1. Dmn files are identical (copy-pasted) except for value returned by the dmn node
 * 2. Each policy references two .dmn files that are named differently, but have identical content apart
 * from returned value
 * <p>
 * Note: An equivalent test could be run based on changing name of the output node (`brDecisionOutputName`),
 * but the error handling involved (there would be a non-descriptive dmn error thrown) would be a
 * distraction from the main issue we're trying to avoid: nodes being overwritten. It's more clean
 * to compare results directly.
 */
@CamelSpringBootTest
@SpringBootTest
@SpringBootConfiguration
@ContextConfiguration(classes = DmnRulesComponentTestConfig.class)
@TestPropertySource(properties = {
        "spring.main.allow-bean-definition-overriding=true",
        "csc.dynamic-updates.enabled=false",
        "csc.client-environment=dev",
        "csc.dev-local.enabled=true",
        "csc.dev-local.debug-root-directory=decisionengine/conflicting_dmn_policy",
})
class DmnRulesComponentConflictingDmnTest {

    @Autowired
    CamelContext camelContext;

    @Autowired
    ApplicationContext applicationContext;

    ProducerTemplate producer20;

    ProducerTemplate producer21;

    @EndpointInject("mock:end")
    MockEndpoint mockEnd;

    @BeforeEach
    void setup() {
        camelContext.start();
        mockEnd = camelContext.getEndpoint("mock:end", MockEndpoint.class);
        producer20 = camelContext.createProducerTemplate();
        producer20.setDefaultEndpointUri("policy:conflicting_dmn_policy_2.0.0?timeout=500");

        producer21 = camelContext.createProducerTemplate();
        producer21.setDefaultEndpointUri("policy:conflicting_dmn_policy_2.1.0?timeout=500");
    }

    @Test
    @DirtiesContext
    @SuppressWarnings("unchecked")
    void sendConflictMessage() {
        HashMap<String, Object> headers = new HashMap<>();
        headers.put(PolicyConstants.HEADER_POLICYNAME, "conflicting_dmn_policy");

        headers.put(PolicyConstants.HEADER_POLICYVERSION_PATCH, "2.0.0");
        producer20.sendBodyAndHeaders(new HashMap<>(), headers);
        Map<String, String> body1 = (Map<String, String>) mockEnd.getReceivedExchanges().get(0).getMessage().getBody();
        assertEquals("2.0", body1.get("policy"));
        assertEquals("2.0-1", body1.get("decisionNode1"));
        assertEquals("2.0-2", body1.get("decisionNode2"));

        headers.put(PolicyConstants.HEADER_POLICYVERSION_PATCH, "2.1.0");
        producer21.sendBodyAndHeaders(new HashMap<>(), headers);
        Map<String, String> body2 = (Map<String, String>) mockEnd.getReceivedExchanges().get(1).getMessage().getBody();
        assertEquals("2.1", body2.get("policy"));
        assertEquals("2.1-1", body2.get("decisionNode1"));
        assertEquals("2.1-2", body2.get("decisionNode2"));

    }

}
