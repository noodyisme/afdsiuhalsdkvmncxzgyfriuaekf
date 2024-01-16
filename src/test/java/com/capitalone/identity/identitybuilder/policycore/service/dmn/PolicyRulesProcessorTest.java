package com.capitalone.identity.identitybuilder.policycore.service.dmn;

import com.capitalone.identity.identitybuilder.decisionengine.service.api.DecisionEngineService;
import com.capitalone.identity.identitybuilder.decisionengine.service.api.DecisionEvaluateRequest;
import com.capitalone.identity.identitybuilder.decisionengine.service.api.DecisionEvaluateResponse;
import com.capitalone.identity.identitybuilder.decisionengine.service.audit.model.DecisionEngineStatus;
import com.capitalone.identity.identitybuilder.decisionengine.service.exception.DmnEvaluateException;
import com.capitalone.identity.identitybuilder.policycore.decisionengine.DecisionAuditLogger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PolicyRulesProcessorTest {

    private static final String DEFAULT_POLICY_NAME = "testPolicyName";
    private static final String DEFAULT_POLICY_VERSION = "1.3";
    private static final String DEFAULT_DMN_NAME = "testDmnName";
    private static final String DEFAULT_RUNTIME_ID = String.format("%s/%s", DEFAULT_POLICY_NAME, DEFAULT_POLICY_VERSION);

    @Mock
    DecisionEngineService mockDecisionEngine;

    @Mock
    DecisionAuditLogger mockAuditLogger;

    // Object under test
    PolicyRulesProcessor processor;

    private static final String outputName = "StablePhoneNumberCheck";
    private static final String outputNameLegacy = "isStablePhoneNumber";

    private static final DecisionEvaluateRequest decisionEvaluateRequest = DecisionEvaluateRequest.builder()
            .runtimeId(DEFAULT_RUNTIME_ID)
            .dmnName(DEFAULT_DMN_NAME)
            .input(new HashMap<>())
            .build();

    private final PolicyRulesProcessor.DecisionEngineRequest decisionEngineRequest = PolicyRulesProcessor.DecisionEngineRequest.builder()
            .decisionEvaluateRequest(decisionEvaluateRequest)
            .dmnDecisionOutputName(outputName)
            .exchangeHeaders(new HashMap<>())
            .build();

    AutoCloseable mockitoMocks;

    @BeforeEach
    public void setup() {
        this.mockitoMocks = MockitoAnnotations.openMocks(this);
        HashMap<String, Object> dmnResult = new HashMap<>();
        HashMap<String, Boolean> output = new HashMap<>();
        output.put(outputNameLegacy, true);
        dmnResult.put(outputName, output);
        DecisionEvaluateResponse decisionEvaluateResponse = mock(DecisionEvaluateResponse.class);
        when(decisionEvaluateResponse.getResult()).thenReturn(dmnResult);
        when(decisionEvaluateResponse.getStatus()).thenReturn(DecisionEngineStatus.SUCCESS);
        when(mockDecisionEngine.evaluate(any())).thenReturn(decisionEvaluateResponse);

        processor = new PolicyRulesProcessor(mockDecisionEngine, mockAuditLogger);
    }

    @AfterEach
    public void tearDown() throws Exception {
        mockitoMocks.close();
    }


    @Test
    public void executeNormalSuccess() {
        processor.execute(decisionEngineRequest, false);
        verify(mockDecisionEngine, times(1)).evaluate(decisionEvaluateRequest);
        verify(mockAuditLogger, times(1)).auditDecisionResults(
                eq(String.join("/", DEFAULT_RUNTIME_ID, DEFAULT_DMN_NAME)),
                eq(outputName),
                any());
    }

    @Test
    public void executeLegacySuccess() {
        processor.execute(decisionEngineRequest, true);
        verify(mockDecisionEngine, times(1)).evaluate(decisionEvaluateRequest);
        verify(mockAuditLogger, times(1)).auditDecisionResults(
                eq(String.join("/", DEFAULT_RUNTIME_ID, DEFAULT_DMN_NAME)),
                eq(outputNameLegacy),
                any());
    }

    @Test
    public void executeMissingExpectedEntryFailure() {
        final PolicyRulesProcessor.DecisionEngineRequest decisionEngineRequestMissingEntry = PolicyRulesProcessor.DecisionEngineRequest.builder()
                .decisionEvaluateRequest(decisionEvaluateRequest)
                .dmnDecisionOutputName("MissingEntry")
                .exchangeHeaders(new HashMap<>())
                .build();
        Assertions.assertThrows(DmnEvaluateException.class,
                () -> processor.execute(decisionEngineRequestMissingEntry, false));
    }
}
