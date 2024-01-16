package com.capitalone.identity.identitybuilder.policycore.service.dmn;

import com.capitalone.identity.identitybuilder.decisionengine.service.api.DecisionEngineService;
import com.capitalone.identity.identitybuilder.decisionengine.service.api.DecisionEvaluateRequest;
import com.capitalone.identity.identitybuilder.decisionengine.service.api.DecisionEvaluateResponse;
import com.capitalone.identity.identitybuilder.decisionengine.service.audit.model.DecisionEngineStatus;
import com.capitalone.identity.identitybuilder.decisionengine.service.exception.DmnEvaluateException;
import com.capitalone.identity.identitybuilder.policycore.decisionengine.DecisionAuditLogger;
import com.capitalone.identity.identitybuilder.policycore.decisionengine.DecisionEngineCustomMapDecisionResult;
import com.capitalone.identity.identitybuilder.policycore.decisionengine.DecisionEngineResult;
import com.capitalone.identity.identitybuilder.policycore.decisionengine.DecisionResult;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.OperationalAudit;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.mapper.DecisionEvaluatedEventMapper;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.model.type.OperationalEventType;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.Map;

@Log4j2
@Component
public class PolicyRulesProcessor {

    final DecisionEngineService decisionEngineService;
    final DecisionAuditLogger auditLogger;

    @Inject
    public PolicyRulesProcessor(DecisionEngineService decisionEngineService, DecisionAuditLogger auditLogger) {
        this.decisionEngineService = decisionEngineService;
        this.auditLogger = auditLogger;
    }

    /**
     * Invokes decision engine with the provided input, preserving only the specified value
     *
     * @param decisionEngineRequest     the decision engine evaluation inputs along with the key for the desired output to preserve in the result
     * @param useLegacyMapResultParsing if true will leverage a legacy result parsing mechanism
     * @return the result
     */
    @OperationalAudit(eventType = OperationalEventType.DECISION_EVALUATED, mapperFactory = DecisionEvaluatedEventMapper.Factory.class)
    public DecisionResult execute(final DecisionEngineRequest decisionEngineRequest, final boolean useLegacyMapResultParsing) {
        final DecisionEvaluateResponse evaluateResponse = decisionEngineService.evaluate(decisionEngineRequest.getDecisionEvaluateRequest());
        DecisionResult decisionResult = createDecisionResult(evaluateResponse, decisionEngineRequest.dmnDecisionOutputName);
        if (useLegacyMapResultParsing) {
            decisionResult = new DecisionEngineCustomMapDecisionResult(decisionResult);
        }
        auditLogger.auditDecisionResults(String.join("/", decisionEngineRequest.getDecisionEvaluateRequest().getRuntimeId(), decisionEngineRequest.getDecisionEvaluateRequest().getDmnName()),
                decisionResult.getName(), decisionResult.getResult());
        return decisionResult;
    }

    /**
     * Converts the decision evaluate response to the result object necessary for further policy core processing.
     * Raises a runtime exception if a failure is detected or if the expected output entry cannot be found.
     *
     * @param evaluateResponse      the decision engine service response
     * @param dmnDecisionOutputName the output entry key of interest which is expected to be present in the output map
     *                              from decision engine service.
     * @return The decision engine result
     */
    private static DecisionEngineResult createDecisionResult(final DecisionEvaluateResponse evaluateResponse,
                                                             final String dmnDecisionOutputName) {
        if (evaluateResponse.getStatus() == DecisionEngineStatus.SUCCESS && !evaluateResponse.getResult().containsKey(dmnDecisionOutputName)) {
            log.error("Could not find expected output key-value pair with key={} in decision engine response with invocation id {}", dmnDecisionOutputName,
                    evaluateResponse.getDecisionEngineInvocationId());
            throw new DmnEvaluateException(String.format("Expected result output key, %s, not found in decision engine output.", dmnDecisionOutputName));
        }
        try {
            return new DecisionEngineResult(dmnDecisionOutputName, evaluateResponse.getResult(), evaluateResponse);
        } catch (final NullPointerException e) {
            throw new DmnEvaluateException("Error occurred while populating DecisionEngineResult.", e);
        }
    }

    @Value
    @Builder(toBuilder = true)
    public static class DecisionEngineRequest {
        @NonNull
        DecisionEvaluateRequest decisionEvaluateRequest;
        @NonNull
        String dmnDecisionOutputName;

        @NonNull
        Map<String, Object> exchangeHeaders;
    }

}
