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
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@CamelSpringBootTest
@ContextConfiguration(classes = DmnRulesComponentTestConfig.class)
@TestPropertySource(properties = {
        "spring.main.allow-bean-definition-overriding=true",
        "csc.dynamic-updates.enabled=false",
        "csc.client-environment=dev",
        "csc.dev-local.enabled=true",
        "csc.dev-local.debug-root-directory=decisionengine/active_mode_policy",
})
class DmnRulesComponentActiveModeTest {

    private static final String SUPPLIED_PHONE_NUMBER = "8923425254";
    private static final String LOOKUP_RESULT_FORMAT = "{\"phoneType\":\"mobile\",\"phoneNumber\":\"%s\",\"isPrimary\":%s,\"hasTcpaConsent\":true,\"voiceStatus\":{\"status\":\"ALL_CALLS\",\"reasons\":[]},\"smsStatus\":{\"status\":\"ALL_TEXT\",\"reasons\":[]},\"timeUpdated\":\"2021-12-23T19:45:13.298Z\",\"addedTimestamp\":\"2021-12-23T19:45:13.298Z\",\"phoneLabel\":\"CustomerMobile\"}";
    private static final String LOOKUP_RESULT_PRIMARY = String.format(LOOKUP_RESULT_FORMAT, SUPPLIED_PHONE_NUMBER, true);
    private static final String LOOKUP_RESULT_NOT_PRIMARY = String.format(LOOKUP_RESULT_FORMAT, SUPPLIED_PHONE_NUMBER, false);

    @Autowired
    CamelContext camelContext;

    @Autowired
    ProducerTemplate producer;

    private MockEndpoint mockEnd;

    @BeforeEach
    public void setup() {
        camelContext.start();
        mockEnd = camelContext.getEndpoint("mock:end", MockEndpoint.class);
        producer.setDefaultEndpointUri("policy:active_mode_policy_1.0.0?timeout=500");
    }

    @Test
    @DirtiesContext
    void sendMessageNoAuditNoFail() {
        assertDoesNotThrow(() -> sendMessage(true, true, Arrays.asList(false, true, true)));
    }

    @Test
    @DirtiesContext
    void sendMessageDefaultResult() throws Exception {
        mockEnd.expectedBodiesReceived("{\"isCustomerEligible\": false, \"isPhoneNumberStable\": false}");
        sendMessage(false, true, Arrays.asList(true, true, true));
        mockEnd.assertIsSatisfied();
    }

    @Test
    @DirtiesContext
    void sendMessageFailedPhoneNumberResult() throws Exception {
        mockEnd.expectedBodiesReceived("{\"isCustomerEligible\": false, \"isPhoneNumberStable\": false}");
        sendMessage(true, false, Arrays.asList(true, true, true));
        mockEnd.assertIsSatisfied();
    }

    @Test
    @DirtiesContext
    void sendMessageFailedEligibilityResult() throws Exception {
        List<List<Boolean>> behaviorSuccessFlag = Arrays.asList(
                Arrays.asList(false, false, false),
                Arrays.asList(false, false, true),
                Arrays.asList(false, true, true),
                Arrays.asList(true, true, false),
                Arrays.asList(true, false, false)
        );

        for (List<Boolean> failCase : behaviorSuccessFlag) {
            mockEnd.expectedBodiesReceived("{\"isCustomerEligible\": false, \"isPhoneNumberStable\": true}");
            sendMessage(true, true, failCase);
            mockEnd.assertIsSatisfied();
            mockEnd.reset();
        }
    }

    @Test
    @DirtiesContext
    void sendMessagePassedEligibilityResult() throws Exception {
        mockEnd.expectedBodiesReceived("{\"isCustomerEligible\": true, \"isPhoneNumberStable\": true}");
        sendMessage(true, true, Arrays.asList(true, true, true));
        mockEnd.assertIsSatisfied();
        mockEnd.reset();
    }

    private void sendMessage(boolean lookupSuccessFlag, boolean phoneSuccessFlag, List<Boolean> behaviorSuccessFlag) throws IOException {
        HashMap<String, Object> headers = new HashMap<>();
        HashMap<String, Object> body = new HashMap<>();

        headers.put(PolicyConstants.HEADER_POLICYNAME, "active_mode_policy");
        headers.put(PolicyConstants.HEADER_POLICYVERSION_PATCH, "1.0.0");

        headers.put("mobilePhoneNumber", SUPPLIED_PHONE_NUMBER);

        // Lookup request failure
        if (lookupSuccessFlag) {
            headers.put("accountLookupStatus", 200);
        } else {
            headers.put("accountLookupStatus", 400);
        }

        // Phone lookup result
        String lookupResult = LOOKUP_RESULT_PRIMARY;
        if (!phoneSuccessFlag) {
            lookupResult = LOOKUP_RESULT_NOT_PRIMARY;
        }
        HashMap<String, Object> actualResultMap = (HashMap<String, Object>) new ObjectMapper().readValue(lookupResult, HashMap.class);
        headers.put("accountLookupResults", Collections.singletonList(actualResultMap));

        // Behavior reference ID results
        List<String> referenceIds = behaviorSuccessFlag.stream().map(success -> {
            if (success) {
                return UUID.randomUUID().toString();
            } else {
                return "ID_SUSPENDED";
            }
        }).collect(Collectors.toList());
        headers.put("accountReferenceIds", referenceIds);

        producer.sendBodyAndHeaders(body, headers);
    }

}
