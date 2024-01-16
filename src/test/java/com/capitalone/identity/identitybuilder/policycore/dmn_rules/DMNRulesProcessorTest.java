package com.capitalone.identity.identitybuilder.policycore.dmn_rules;

import com.capitalone.identity.identitybuilder.decisionengine.service.api.DecisionEvaluateResponse;
import com.capitalone.identity.identitybuilder.decisionengine.service.api.DecisionRuntimeConfiguration;
import com.capitalone.identity.identitybuilder.decisionengine.service.audit.model.DecisionEngineStatus;
import com.capitalone.identity.identitybuilder.decisionengine.service.audit.model.DecisionPolicyInfo;
import com.capitalone.identity.identitybuilder.decisionengine.service.audit.model.EvaluationInfo;
import com.capitalone.identity.identitybuilder.policycore.camel.PolicyConstants;
import com.capitalone.identity.identitybuilder.policycore.decisionengine.DecisionEngineResult;
import com.capitalone.identity.identitybuilder.policycore.dmnrules.DMNRulesProcessor;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.model.policy.OperationalAuditExecutionData;
import com.capitalone.identity.identitybuilder.policycore.service.dmn.PolicyRulesProcessor;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.support.DefaultExchange;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.kie.dmn.api.core.DMNResult;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class DMNRulesProcessorTest {

    private static final String BR_TRANSACTION_NAME = "brTransactionName";
    private static final String BUSINESS_RULES_INPUT = "businessRulesInput";
    private static final String BR_DECISION_OUTPUTNAME = "brDecisionOutputname";

    @Mock
    PolicyRulesProcessor policyRulesProcessor;

    Exchange exchange;

    AutoCloseable mockitoMocks;

    private DecisionEvaluateResponse decisionEvaluateResponse;

    @BeforeEach
    public void setup() {
        this.mockitoMocks = MockitoAnnotations.openMocks(this);

        CamelContext context = new DefaultCamelContext();
        exchange = new DefaultExchange(context);
        exchange.getIn().setBody(new HashMap<>());

        HashMap<String, Object> dmnResult = new HashMap<>();
        HashMap<Object, Boolean> output = new HashMap<>();
        dmnResult.put("StablePhoneNumberCheck", output);

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
                .result(dmnResult)
                .droolsDMNResult(droolsDmnResult)
                .decisionRuntimeConfiguration(decisionRuntimeConfiguration)
                .authorBA(Optional.of("Sample-author"))
                .decisionPolicyInfo(Optional.ofNullable(decisionPolicyInfo))
                .supplementalAttributes(new HashMap<>())
                .build();

        when(policyRulesProcessor.execute(any(PolicyRulesProcessor.DecisionEngineRequest.class), Mockito.anyBoolean()))
                .thenReturn(new DecisionEngineResult("StablePhoneNumberCheck", dmnResult, decisionEvaluateResponse));
    }

    @AfterEach
    public void tearDown() throws Exception {
        this.mockitoMocks.close();
    }

    @Test
    public void processor_called_with_arguments() throws Exception {
        Processor processor = new DMNRulesProcessor(policyRulesProcessor, false, false);
        Map<String, Object> body = (Map<String, Object>) exchange.getIn().getBody();
        body.put(BR_TRANSACTION_NAME, "testTransactionName.dmn");
        HashMap<String, Object> input = new HashMap<>();
        body.put(BUSINESS_RULES_INPUT, input);
        body.put(BR_DECISION_OUTPUTNAME, "StablePhoneNumberCheck");
        processor.process(exchange);
        verify(policyRulesProcessor, times(1)).execute(any(PolicyRulesProcessor.DecisionEngineRequest.class), Mockito.eq(false));
    }

    @ParameterizedTest
    @ValueSource(strings = {"testTransactionName1.dmn", "testTransactionName2.dmn", "another.dmn"})
    public void dmnsEvaluatedAudit(final String transactionName) throws Exception {
        // Arrange
        exchange.getIn().setHeader(PolicyConstants.HEADER_POLICYNAME, "policyfoo");
        exchange.getIn().setHeader(PolicyConstants.HEADER_POLICYVERSION_PATCH, "1.0.0");
        final Processor processor = new DMNRulesProcessor(policyRulesProcessor, false, false);
        final Map<String, Object> body = (Map<String, Object>) (exchange.getIn().getBody());
        body.put(BR_TRANSACTION_NAME, transactionName);
        HashMap<String, Object> input = new HashMap<>();
        body.put(BUSINESS_RULES_INPUT, input);
        body.put(BR_DECISION_OUTPUTNAME, "StablePhoneNumberCheck");
        exchange.getIn().setHeader(PolicyConstants.OPERATIONAL_AUDIT_EXECUTION_DATA, new OperationalAuditExecutionData());

        // Act
        processor.process(exchange);

        // Assert
        final List<String> dmnsEvaluated = ((OperationalAuditExecutionData) exchange.getIn().getHeader(PolicyConstants.OPERATIONAL_AUDIT_EXECUTION_DATA)).getDmnsEvaluated();
        Assertions.assertEquals(1, dmnsEvaluated.size());
        Assertions.assertEquals(String.format("policyfoo/1.0.0/%s", transactionName), dmnsEvaluated.get(0));
    }
}