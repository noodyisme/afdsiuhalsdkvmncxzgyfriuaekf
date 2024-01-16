package com.capitalone.identity.identitybuilder.policycore.operational_audit.mapper;

import com.capitalone.identity.identitybuilder.audit.util.JsonSerializationUtil;
import com.capitalone.identity.identitybuilder.decisionengine.service.audit.exceptions.ErrorCode;
import com.capitalone.identity.identitybuilder.decisionengine.service.audit.model.DecisionEngineStatus;
import com.capitalone.identity.identitybuilder.policycore.decisionengine.DecisionEngineResult;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.config.OperationalAuditComponent;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.model.OperationalError;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.model.OperationalEvent;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.model.OperationalEventOutcome;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.model.OperationalStatus;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.model.decision.OperationalDmnMetadata;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.model.policy.OperationalPolicyMetadata;
import com.capitalone.identity.identitybuilder.policycore.service.constants.ApplicationConstants;
import com.capitalone.identity.identitybuilder.policycore.service.dmn.PolicyRulesProcessor;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;

import java.util.Optional;

@RequiredArgsConstructor
@Log4j2
public class DecisionEvaluatedEventMapper implements OperationalEventMapper {
    private final @NonNull Optional<PolicyRulesProcessor.DecisionEngineRequest> decisionEvaluateRequestOptional;
    private final @NonNull Optional<DecisionEngineResult> decisionResultOptional;
    private final boolean inputLoggingEnabled;
    private final boolean outputLoggingEnabled;

    @Override
    public OperationalEvent buildWithEntitySpecificAttributes() {
        final OperationalEvent.OperationalEventBuilder operationalEventBuilder = OperationalEvent.builder();
        final OperationalDmnMetadata.OperationalDmnMetadataBuilder operationalDmnMetadataBuilder = OperationalDmnMetadata.builder();
        final OperationalPolicyMetadata.OperationalPolicyMetadataBuilder operationalPolicyMetadataBuilder = OperationalPolicyMetadata.builder();

        // ProcessId mapping
        decisionEvaluateRequestOptional.ifPresent(decisionEvaluateRequest -> {
            operationalEventBuilder.eventEntityName(decisionEvaluateRequest.getDecisionEvaluateRequest().getDmnName());
            if (inputLoggingEnabled) {
                operationalEventBuilder.protocolRequestDetail(JsonSerializationUtil.writeAsString(
                        decisionEvaluateRequest.getDecisionEvaluateRequest().getInput()));
            } else {
                operationalEventBuilder.protocolRequestDetail("Transaction handles sensitive data. Input data redacted.");
            }

            operationalDmnMetadataBuilder.dmnResultKey(decisionEvaluateRequest.getDmnDecisionOutputName());
            operationalDmnMetadataBuilder.dmnRuntimeName(decisionEvaluateRequest.getDecisionEvaluateRequest().getRuntimeId());

            operationalPolicyMetadataBuilder.policyName((String) decisionEvaluateRequest.getExchangeHeaders().get(ApplicationConstants.POLICY_NAME));
            operationalPolicyMetadataBuilder.versionExecuted((String) decisionEvaluateRequest.getExchangeHeaders().get(ApplicationConstants.POLICY_VERSION_PATCH));
            operationalPolicyMetadataBuilder.processId((String) decisionEvaluateRequest.getExchangeHeaders().get(ApplicationConstants.CACHE_KEY));
            operationalPolicyMetadataBuilder.step((String) decisionEvaluateRequest.getExchangeHeaders().get(ApplicationConstants.STEP_NAME));

            operationalEventBuilder.policyMetadata(operationalPolicyMetadataBuilder.build());

        });

        decisionResultOptional.ifPresent(result -> {
            if (outputLoggingEnabled) {
                operationalEventBuilder.protocolResponseDetail(JsonSerializationUtil.writeAsString(result.getResult()));
            } else {
                operationalEventBuilder.protocolResponseDetail("Transaction handles sensitive data. Output data redacted.");
            }
        });

        return operationalEventBuilder.dmnMetadata(operationalDmnMetadataBuilder.build()).build();
    }

    @Override
    public OperationalEventOutcome toOperationalEventOutcome() {
        if (decisionResultOptional.isPresent() &&
                decisionResultOptional.get().getDecisionEvaluateResponse().getStatus().equals(DecisionEngineStatus.SUCCESS)) {
            return OperationalEventOutcome.of(OperationalStatus.SUCCESS, "SUCCESS");
        } else {
            return OperationalEventOutcome.of(OperationalStatus.FAILURE, "FAILURE");
        }
    }

    @Override
    public Optional<OperationalError> toOperationalError() {
        if (decisionResultOptional.isPresent() &&
                decisionResultOptional.get().getDecisionEvaluateResponse().getStatus().equals(DecisionEngineStatus.FAILURE)) {
            return Optional.ofNullable(OperationalError.builder()
                    .errorId(ErrorCode.EVALUATE_ERROR.getErrorId())
                    .developerText(JsonSerializationUtil.writeAsString(decisionResultOptional.get().getDecisionEvaluateResponse().getEvaluationInfo()))
                    .build());
        } else {
            return Optional.empty();
        }
    }

    @OperationalAuditComponent
    @AllArgsConstructor
    public static class Factory implements OperationalEventMapper.AspectEventFactory {

        @Value("${identitybuilder.decisionengine.audit.input-logging.enable:false}")
        private final boolean inputLoggingEnabled;

        @Value("${identitybuilder.decisionengine.audit.output-logging.enable:false}")
        private final boolean outputLoggingEnabled;

        @NonNull
        @Override
        public DecisionEvaluatedEventMapper create(final Object request,
                                                   final Object response) {
            return new DecisionEvaluatedEventMapper(Optional.ofNullable((PolicyRulesProcessor.DecisionEngineRequest) request),
                    Optional.ofNullable((DecisionEngineResult) response), inputLoggingEnabled, outputLoggingEnabled);
        }
    }
}


