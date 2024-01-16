package com.capitalone.identity.identitybuilder.policycore.dmn_rules;

import com.capitalone.identity.identitybuilder.policycore.camel.PolicyConstants;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;

@CamelSpringBootTest
@SpringBootTest
@SpringBootConfiguration
@ContextConfiguration(classes = DmnRulesComponentTestConfig.class)
@TestPropertySource(properties = {
        "spring.main.allow-bean-definition-overriding=true",
        "csc.dynamic-updates.enabled=false",
        "csc.client-environment=dev",
        "csc.dev-local.enabled=true",
        "csc.dev-local.debug-root-directory=decisionengine/listening_mode_policy",

})
class DmnRulesComponentListeningModeTest {

    @Autowired
    CamelContext camelContext;

    @Autowired
    ProducerTemplate producer;

    MockEndpoint mockEnd;

    @BeforeEach
    void setup() {
        camelContext.start();
        mockEnd = camelContext.getEndpoint("mock:end", MockEndpoint.class);
        producer.setDefaultEndpointUri("policy:listening_mode_policy_1.0.0");
    }

    @Test
    void sendMessage() throws Exception {

        String rawPhone = "{\"phoneType\":\"mobile\",\"phoneNumber\":\"8923425254\",\"isPrimary\":true,\"hasTcpaConsent\":true,\"voiceStatus\":{\"status\":\"ALL_CALLS\",\"reasons\":[]},\"smsStatus\":{\"status\":\"ALL_TEXT\",\"reasons\":[]},\"timeUpdated\":\"2021-12-23T19:45:13.298Z\",\"addedTimestamp\":\"2021-12-23T19:45:13.298Z\",\"phoneLabel\":\"CustomerMobile\"}";
        HashMap<String, Object> actualResultMap = (HashMap<String, Object>) new ObjectMapper().readValue(rawPhone, HashMap.class);
        HashMap<String, Object> headers = new HashMap<>();
        headers.put(PolicyConstants.HEADER_POLICYNAME, "listening_mode_policy");
        headers.put(PolicyConstants.HEADER_POLICYVERSION_PATCH, "1.0.0");
        headers.put("accountLookupStatus", 200);
        headers.put("accountLookupResults", Collections.singletonList(actualResultMap));
        headers.put("accountReferenceIds", Arrays.asList("ID_SUSPENDED", "HIJKLMN", "OPQRSTUV"));
        headers.put("mobilePhoneNumber", "8923425254");

        mockEnd.expectedBodiesReceived("{\"isTestPolicyRulePassed\": false}");
        producer.sendBodyAndHeaders(null, headers);

        mockEnd.assertIsSatisfied();
    }
}
