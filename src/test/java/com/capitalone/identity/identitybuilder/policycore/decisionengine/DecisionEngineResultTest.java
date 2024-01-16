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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class DecisionEngineResultTest {
    private DecisionEvaluateResponse defaultDecisionEvaluateResponse;
    private DecisionEvaluateResponse booleanDecisionEvaluateResponse;
    private HashMap<String, Object> booleanMockResponse;
    private HashMap<String, Object> defaultMockResponse;
    private static final String OUTPUT_NAME = "StablePhoneNumberCheck";
    private static final String RESULT = "DMN_OUTPUT_CONTENT";
    private final static boolean BOOLEAN_RESULT = true;

    @BeforeEach
    void setup() {


        defaultMockResponse = buildMockDecisionEngineResponse(OUTPUT_NAME, RESULT);
        booleanMockResponse = buildMockDecisionEngineResponse(OUTPUT_NAME, BOOLEAN_RESULT);

        EvaluationInfo evaluationInfo = Mockito.mock(EvaluationInfo.class);
        DMNResult droolsDmnResult = Mockito.mock(DMNResult.class);
        DecisionRuntimeConfiguration decisionRuntimeConfiguration = Mockito.mock(DecisionRuntimeConfiguration.class);
        DecisionPolicyInfo decisionPolicyInfo = DecisionPolicyInfo.builder()
                .policyName("DEFAULT_POLICY_NAME")
                .policyVersion("DEFAULT_POLICY_PATCH_VERSION")
                .build();

        defaultDecisionEvaluateResponse = DecisionEvaluateResponse.builder()
                .status(DecisionEngineStatus.SUCCESS)
                .evaluationInfo(evaluationInfo)
                .decisionEngineInvocationId("Sample")
                .runtimeId("DEFAULT_RUNTIME_ID")
                .dmnName("DEFAULT_DMN_NAME")
                .result(defaultMockResponse)
                .droolsDMNResult(droolsDmnResult)
                .decisionRuntimeConfiguration(decisionRuntimeConfiguration)
                .authorBA(Optional.of("Sample-author"))
                .decisionPolicyInfo(Optional.ofNullable(decisionPolicyInfo))
                .supplementalAttributes(new HashMap<>())
                .build();

        booleanDecisionEvaluateResponse = DecisionEvaluateResponse.builder()
                .status(DecisionEngineStatus.SUCCESS)
                .evaluationInfo(evaluationInfo)
                .decisionEngineInvocationId("Sample")
                .runtimeId("DEFAULT_RUNTIME_ID")
                .dmnName("DEFAULT_DMN_NAME")
                .result(booleanMockResponse)
                .droolsDMNResult(droolsDmnResult)
                .decisionRuntimeConfiguration(decisionRuntimeConfiguration)
                .authorBA(Optional.of("Sample-author"))
                .decisionPolicyInfo(Optional.ofNullable(decisionPolicyInfo))
                .supplementalAttributes(new HashMap<>())
                .build();

    }

    private static HashMap<String, Object> buildMockDecisionEngineResponse(String outputKey, Object result) {
        HashMap<String, Object> resultHashMap = new HashMap<>();
        resultHashMap.put(outputKey, result);
        return resultHashMap;
    }

    @Test
    public void getDecisionEngineResult() {
        DecisionEngineResult decisionEngineResult = new DecisionEngineResult(OUTPUT_NAME, defaultMockResponse, defaultDecisionEvaluateResponse);
        assertEquals(RESULT, decisionEngineResult.getResult());
        assertEquals(OUTPUT_NAME, decisionEngineResult.getName());
    }

    @Test
    public void getDecisionEngineBooleanResult() {
        DecisionEngineResult decisionEngineResult = new DecisionEngineResult(OUTPUT_NAME, booleanMockResponse, booleanDecisionEvaluateResponse);
        assertTrue(decisionEngineResult.getResultAsBoolean());
    }

    @Test
    public void getDecisionEngineBooleanResultClassCastError() {

        assertThrows(ClassCastException.class, () -> {
            DecisionEngineResult decisionEngineResult = new DecisionEngineResult(OUTPUT_NAME, defaultMockResponse, defaultDecisionEvaluateResponse);
            decisionEngineResult.getResultAsBoolean();
        });
    }

}
