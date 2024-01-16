package com.capitalone.identity.identitybuilder.policycore.operational_audit;

import com.capitalone.identity.identitybuilder.audit.models.AuditEvent;
import com.capitalone.identity.identitybuilder.audit.sdpv4.AuditPublisher;
import com.capitalone.identity.identitybuilder.audit.sdpv4.ProducerProperties;
import com.capitalone.identity.identitybuilder.decisionengine.service.api.DecisionEvaluateRequest;
import com.capitalone.identity.identitybuilder.decisionengine.service.api.DecisionEvaluateResponse;
import com.capitalone.identity.identitybuilder.decisionengine.service.api.DecisionRuntimeConfiguration;
import com.capitalone.identity.identitybuilder.decisionengine.service.audit.model.DecisionEngineStatus;
import com.capitalone.identity.identitybuilder.decisionengine.service.audit.model.DecisionPolicyInfo;
import com.capitalone.identity.identitybuilder.decisionengine.service.audit.model.EvaluationInfo;
import com.capitalone.identity.identitybuilder.decisionengine.service.exception.DmnEvaluateException;
import com.capitalone.identity.identitybuilder.policycore.decisionengine.DecisionEngineResult;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.cyber.AWSHostContextCollector;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.mapper.DecisionEvaluatedEventMapper;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.mapper.OperationalEventEnricher;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.mapper.OperationalEventMapper;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.model.*;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.util.MockHostContext;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.util.OperationalAuditPublisher;
import com.capitalone.identity.identitybuilder.policycore.service.constants.ApplicationConstants;
import com.capitalone.identity.identitybuilder.policycore.service.dmn.PolicyRulesProcessor;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.kie.dmn.api.core.DMNResult;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static com.capitalone.identity.identitybuilder.policycore.operational_audit.mapper.OperationalEventMapper.AspectEventFactory;

class DecisionEvaluatedAuditTest {

    private static final String DEFAULT_POLICY_NAME = "testPolicyName";
    private static final String DEFAULT_POLICY_VERSION = "1.0";
    private static final String DEFAULT_POLICY_PATCH_VERSION = "1.0.0";
    private static final String DMN_OUTPUT_NAME = "sample";
    private static final String DEFAULT_DMN_NAME = "testDmnName";
    private static final String DEFAULT_RUNTIME_ID = String.format("%s/%s", DEFAULT_POLICY_NAME, DEFAULT_POLICY_VERSION);
    private PolicyRulesProcessor.DecisionEngineRequest decisionEngineRequest;
    private DecisionEngineResult decisionEngineSuccessResult;
    private DmnEvaluateException decisionEngineFailureResult;
    private final AWSHostContextCollector mockAWSHostContextCollector = new AWSHostContextCollector(new MockHostContext());
    private AuditPublisher auditPublisher;
    private ArgumentCaptor<AuditEvent> auditEventArgumentCaptor;
    private OperationalAuditAspect operationalAuditAspect;
    private PolicyRulesProcessor processor;
    private OperationalSystemContext operationalSystemContext;

    @SneakyThrows
    @BeforeEach
    void setup() {
        processor = Mockito.mock(PolicyRulesProcessor.class);
        auditPublisher = Mockito.mock(AuditPublisher.class);
        ProducerProperties producerProperties = Mockito.mock(ProducerProperties.class);
        Mockito.lenient().when(producerProperties.getSchemaName()).thenReturn("testSchema");
        operationalSystemContext = Mockito.mock(OperationalSystemContext.class);
        Map<Class<? extends AspectEventFactory>, AspectEventFactory> factoryMap = new HashMap<>();
        factoryMap.put(DecisionEvaluatedEventMapper.Factory.class, new DecisionEvaluatedEventMapper.Factory(true, true));

        operationalAuditAspect = new OperationalAuditAspect(
                new OperationalEventEnricher(operationalSystemContext, true),
                new OperationalAuditPublisher(auditPublisher, producerProperties.getSchemaName()),
                factoryMap, mockAWSHostContextCollector);

        // Initialize the argument captor used for verification
        auditEventArgumentCaptor = ArgumentCaptor.forClass(AuditEvent.class);

        // Mapping request and response
        createDefaultDmnResponse();
    }

    @Test
    void verifyDecisionAuditAspect() {
        createDefaultDmnRequest();
        createDefaultDmnResponse();
        Mockito.when(processor.execute(decisionEngineRequest, false)).thenReturn(decisionEngineSuccessResult);
        processor = OperationalAuditTestUtil.registerWithAspect(processor, operationalAuditAspect);
        processor.execute(decisionEngineRequest, false);
        expectedDmnEvaluatedEvent(OperationalEventOutcome.of(OperationalStatus.SUCCESS, "SUCCESS"),
                decisionEngineRequest
                , decisionEngineSuccessResult, null);
    }

    @Test
    void verifyDecisionAuditFailureOutcome() {
        createDefaultDmnRequest();
        createFailureDmnResponse();
        Mockito.when(processor.execute(decisionEngineRequest, false)).thenThrow(decisionEngineFailureResult);
        processor = OperationalAuditTestUtil.registerWithAspect(processor, operationalAuditAspect);
        assertThrows(DmnEvaluateException.class, () -> processor.execute(decisionEngineRequest, false));

        expectedDmnEvaluatedEvent(OperationalEventOutcome.of(OperationalStatus.FAILURE, "FAILURE"),
                decisionEngineRequest
                , null, OperationalError.builder().
                        text(decisionEngineFailureResult.getMessage()).
                        developerText(decisionEngineFailureResult.getLocalizedMessage()).
                        build());
    }

    private void expectedDmnEvaluatedEvent(final OperationalEventOutcome expectedEventOutcome,
                                           final PolicyRulesProcessor.DecisionEngineRequest expectedDecisionEvaluateRequest,
                                           final DecisionEngineResult decisionEngineResult,
                                           final OperationalError expectedErrorMetadata) {

        final OperationalEvent operationalEvent = OperationalAuditTestUtil.captureOperationalEvent(
                auditPublisher, auditEventArgumentCaptor);

        // Validate EventEntity
        assertEquals("DECISION_EVALUATED", operationalEvent.getEventName());
        assertEquals("DECISION", operationalEvent.getEventEntity());
        assertEquals(expectedDecisionEvaluateRequest.getDecisionEvaluateRequest().getDmnName(), operationalEvent.getEventEntityName());
        assertEquals("EVALUATED", operationalEvent.getEventAction());

        // Validate DMN MetaData
        assertEquals(expectedDecisionEvaluateRequest.getDmnDecisionOutputName(), operationalEvent.getDmnMetadata().getDmnResultKey());
        assertEquals(expectedDecisionEvaluateRequest.getDecisionEvaluateRequest().getRuntimeId(), operationalEvent.getDmnMetadata().getDmnRuntimeName());

        // Validate Policy MetaData
        assertEquals(DEFAULT_POLICY_NAME, operationalEvent.getPolicyMetadata().getPolicyName());
        assertEquals(DEFAULT_POLICY_PATCH_VERSION, operationalEvent.getPolicyMetadata().getVersionExecuted());
        assertEquals("sampleProcessId", operationalEvent.getPolicyMetadata().getProcessId());
        assertEquals("start", operationalEvent.getPolicyMetadata().getStep());

        // Validate Operational System Context
        assertEquals(this.operationalSystemContext, operationalEvent.getSystemContext());

        // Validate Event Outcome
        assertEquals(expectedEventOutcome, operationalEvent.getEventOutcome());

        if (operationalEvent.getEventOutcome().equals(OperationalEventOutcome.of(OperationalStatus.FAILURE, "FAILURE")))
            assertEquals(expectedErrorMetadata.getErrorId(), operationalEvent.getErrorMetadata().getErrorId());


    }

    private void createDefaultDmnRequest() {
        Map<String, Object> decisionInput = new HashMap<>();

        Map<String, Object> exchangeHeaders = new HashMap<>();
        exchangeHeaders.put(ApplicationConstants.POLICY_NAME, DEFAULT_POLICY_NAME);
        exchangeHeaders.put(ApplicationConstants.POLICY_VERSION_PATCH, DEFAULT_POLICY_PATCH_VERSION);
        exchangeHeaders.put(ApplicationConstants.CACHE_KEY, "sampleProcessId");
        exchangeHeaders.put(ApplicationConstants.STEP_NAME, "start");

        DecisionEvaluateRequest decisionEvaluateRequest = DecisionEvaluateRequest.builder()
                .runtimeId(DEFAULT_RUNTIME_ID)
                .dmnName(DEFAULT_DMN_NAME)
                .input(decisionInput)
                .build();

        this.decisionEngineRequest = PolicyRulesProcessor.DecisionEngineRequest.builder()
                .decisionEvaluateRequest(decisionEvaluateRequest)
                .dmnDecisionOutputName(DMN_OUTPUT_NAME)
                .exchangeHeaders(exchangeHeaders)
                .build();
    }

    private void createDefaultDmnResponse() {
        HashMap<String, Object> dmnResult = new HashMap<>();
        HashMap<String, Boolean> output = new HashMap<>();
        output.put("placeholder", true);
        dmnResult.put(DMN_OUTPUT_NAME, output);

        EvaluationInfo evaluationInfo = Mockito.mock(EvaluationInfo.class);
        DMNResult droolsDmnResult = Mockito.mock(DMNResult.class);
        DecisionRuntimeConfiguration decisionRuntimeConfiguration = Mockito.mock(DecisionRuntimeConfiguration.class);
        DecisionPolicyInfo decisionPolicyInfo = DecisionPolicyInfo.builder()
                .policyName(DEFAULT_POLICY_NAME)
                .policyVersion(DEFAULT_POLICY_PATCH_VERSION)
                .build();

        DecisionEvaluateResponse decisionEvaluateResponse = DecisionEvaluateResponse.builder()
                .status(DecisionEngineStatus.SUCCESS)
                .evaluationInfo(evaluationInfo)
                .decisionEngineInvocationId("Sample")
                .runtimeId(DEFAULT_RUNTIME_ID)
                .dmnName(DEFAULT_DMN_NAME)
                .result(dmnResult)
                .droolsDMNResult(droolsDmnResult)
                .decisionRuntimeConfiguration(decisionRuntimeConfiguration)
                .authorBA(Optional.of("Sample-author"))
                .decisionPolicyInfo(Optional.ofNullable(decisionPolicyInfo))
                .supplementalAttributes(new HashMap<>())
                .build();
        this.decisionEngineSuccessResult = new DecisionEngineResult(DMN_OUTPUT_NAME, dmnResult, decisionEvaluateResponse);
    }

    private void createFailureDmnResponse() {
        this.decisionEngineFailureResult = new DmnEvaluateException(String.format("Expected result output key, %s, not found in decision engine output.", DMN_OUTPUT_NAME));
    }

}
