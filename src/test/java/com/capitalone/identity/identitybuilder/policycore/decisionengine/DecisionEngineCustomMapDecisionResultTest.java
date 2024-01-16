package com.capitalone.identity.identitybuilder.policycore.decisionengine;

import com.capitalone.identity.identitybuilder.decisionengine.service.api.DecisionEvaluateResponse;
import com.capitalone.identity.identitybuilder.decisionengine.service.api.DecisionRuntimeConfiguration;
import com.capitalone.identity.identitybuilder.decisionengine.service.audit.model.DecisionEngineStatus;
import com.capitalone.identity.identitybuilder.decisionengine.service.audit.model.DecisionPolicyInfo;
import com.capitalone.identity.identitybuilder.decisionengine.service.audit.model.EvaluationInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.kie.dmn.api.core.DMNResult;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class DecisionEngineCustomMapDecisionResultTest {

    /**
     * Raw output:
     * "{\n" +
     * "  \"matchCustomerPhoneNumber\" : {\n" +
     * "    \"phoneType\" : \"mobile\",\n" +
     * "    \"phoneNumber\" : \"8923425254\",\n" +
     * "    \"isPrimary\" : true,\n" +
     * "    \"hasTcpaConsent\" : true,\n" +
     * "    \"voiceStatus\" : {\n" +
     * "      \"status\" : \"ALL_CALLS\",\n" +
     * "      \"reasons\" : [ ]\n" +
     * "    },\n" +
     * "    \"smsStatus\" : {\n" +
     * "      \"status\" : \"ALL_TEXT\",\n" +
     * "      \"reasons\" : [ ]\n" +
     * "    },\n" +
     * "    \"timeUpdated\" : \"2018-01-17T19:45:13.298Z\",\n" +
     * "    \"addedTimestamp\" : \"2018-01-17T19:45:13.298Z\",\n" +
     * "    \"phoneLabel\" : \"CustomerMobile\"\n" +
     * "  },\n" +
     * "  \"StablePhoneNumberCheck\" : {\n" +
     * "    \"isPhoneNumberStable\" : true\n" +
     * "  }\n" +
     * "}";
     */
    static HashMap<String, Object> PASSING_DECISION_RESULT = buildMockDecisionEngineResult("StablePhoneNumberCheck", "isPhoneNumberStable", true);
    static HashMap<String, Object> FAILING_DECISION_RESULT = buildMockDecisionEngineResult("StablePhoneNumberCheck", "isPhoneNumberStable", false);

    private DecisionEvaluateResponse decisionEvaluateResponse;

    @BeforeEach
    void setup() {
        EvaluationInfo evaluationInfo = Mockito.mock(EvaluationInfo.class);
        DMNResult droolsDmnResult = Mockito.mock(DMNResult.class);
        DecisionRuntimeConfiguration decisionRuntimeConfiguration = Mockito.mock(DecisionRuntimeConfiguration.class);
        DecisionPolicyInfo decisionPolicyInfo = DecisionPolicyInfo.builder()
                .policyName("DEFAULT_POLICY_NAME")
                .policyVersion("DEFAULT_POLICY_PATCH_VERSION")
                .build();

        decisionEvaluateResponse = DecisionEvaluateResponse.builder()
                .status(DecisionEngineStatus.SUCCESS)
                .evaluationInfo(evaluationInfo)
                .decisionEngineInvocationId("Sample")
                .runtimeId("DEFAULT_RUNTIME_ID")
                .dmnName("DEFAULT_DMN_NAME")
                .result(PASSING_DECISION_RESULT)
                .droolsDMNResult(droolsDmnResult)
                .decisionRuntimeConfiguration(decisionRuntimeConfiguration)
                .authorBA(Optional.of("Sample-author"))
                .decisionPolicyInfo(Optional.ofNullable(decisionPolicyInfo))
                .supplementalAttributes(new HashMap<>())
                .build();
    }

    private static HashMap<String, Object> buildMockDecisionEngineResult(String outputKey, String resultKey, boolean didPass) {
        HashMap<String, Object> stringObjectHashMap = new HashMap<>();
        // decision engine output object is linked hash map
        LinkedHashMap<String, Boolean> outputResult = new LinkedHashMap<>();
        outputResult.put(resultKey, didPass);
        stringObjectHashMap.put(outputKey, outputResult);
        return stringObjectHashMap;
    }

    private static final String transactionName = "card_fraud_customer_overlap_phone_number_eligibility_3.0.dmn";
    private static final String outputName = "StablePhoneNumberCheck";


    @Test
    void getRulePassed() {
        DecisionEngineResult decisionEngineResult = new DecisionEngineResult(outputName, PASSING_DECISION_RESULT, decisionEvaluateResponse);
        DecisionEngineCustomMapDecisionResult result = new DecisionEngineCustomMapDecisionResult(decisionEngineResult);
        assertSame(Boolean.TRUE, result.getResult());
        assertTrue(result.getResultAsBoolean());
        assertEquals("isPhoneNumberStable", result.getName());
    }

    @Test
    void getRuleFailed() {
        DecisionEngineResult decisionEngineResult = new DecisionEngineResult(outputName, FAILING_DECISION_RESULT, decisionEvaluateResponse);
        DecisionEngineCustomMapDecisionResult result = new DecisionEngineCustomMapDecisionResult(decisionEngineResult);
        assertFalse(result.getResultAsBoolean());
        assertEquals("isPhoneNumberStable", result.getName());
    }

}
