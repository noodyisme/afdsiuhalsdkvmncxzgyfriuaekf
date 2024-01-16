package com.capitalone.identity.identitybuilder.policycore.service.logging;

import com.capitalone.chassis.engine.model.error.ErrorResponse;
import com.capitalone.chassis.engine.model.exception.AbstractChassisException;
import com.capitalone.identity.identitybuilder.policycore.model.*;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.PolicyEvaluatedAuditor;
import com.capitalone.identity.identitybuilder.policycore.service.PolicyService;
import com.newrelic.api.agent.Trace;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.extern.log4j.Log4j2;
import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * This class is for invoking a policy execution and auditing its results to Sdpv4/Sdpv3.
 */
@Component
@AllArgsConstructor
@Log4j2
public class PolicyServiceExecutor {

    private final AuditLogger auditLogger;
    private final PolicyEvaluatedAuditor policyEvaluatedAuditor;
    private final ConversionService conversionService;
    private final PolicyService.PolicyExecutionHelper policyExecutionHelper;

    /**
     * Invokes a policy and handles policy execution audit requirements
     * @param policyRequestSupplier the supplier for the request object (if an error occurs during request creation,
     *                              then it must be audited also).
     * @return the policy execution response
     */
    @Trace
    public PolicyService.ExecutedPolicyResponse executePolicyWithAuditing(
            final @NonNull Supplier<PolicyRequest> policyRequestSupplier,
            final @NonNull PolicyAuditContext policyAuditContext) {

        final long startTimestamp = System.currentTimeMillis();

        final PolicyRequest policyRequest;
        try {
            policyRequest = policyRequestSupplier.get();
        } catch (final Exception e) {
            handleRequestGenerationFailed(policyAuditContext, e, startTimestamp);
            throw e;
        }

        try {
            final PolicyService.ExecutedPolicyResponse executedPolicyResponse = policyExecutionHelper.executePolicy(policyRequest);
            handleEvaluationResponse(policyRequest, executedPolicyResponse, startTimestamp, policyAuditContext);
            return executedPolicyResponse;
        } catch (final Exception e) {
            handleException(policyRequest, e, startTimestamp, policyAuditContext);
            throw e;
        }
    }

    private void handleRequestGenerationFailed(final PolicyAuditContext policyAuditContext, final Exception e,
            final long startTimestamp) {
        policyEvaluatedAuditor.audit(getPlaceholderPolicyRequest(policyAuditContext), null,
                e, startTimestamp, policyAuditContext);
    }

    private void handleException(final PolicyRequest policyRequest, final Exception e, final long startTimestamp,
            final PolicyAuditContext policyAuditContext) {
        final PolicyService.ExecutedPolicyResponse executedPolicyResponse = getPlaceholderExecutePolicyResponse(e);
        if (e instanceof AbstractChassisException) {
            auditChassisException((AbstractChassisException) e, executedPolicyResponse, policyRequest, startTimestamp,
                    policyAuditContext);
        } else {
            policyEvaluatedAuditor.audit(policyRequest, executedPolicyResponse, e, startTimestamp, policyAuditContext);
        }
    }

    private void handleEvaluationResponse(final PolicyRequest policyRequest,
            final PolicyService.ExecutedPolicyResponse executedPolicyResponse,
            final long startTimestamp, final PolicyAuditContext policyAuditContext) {
        if (executedPolicyResponse.getResult() instanceof PolicyService.Result.PolicySuccess) {
            auditLogger.logAudits(policyRequest, policyRequest.getPolicyName(),
                    executedPolicyResponse.getExecutedPolicyVersion(),
                    policyRequest.getStep(), executedPolicyResponse, policyAuditContext.getParentPolicyName());
            policyEvaluatedAuditor.audit(policyRequest, executedPolicyResponse,
                    null, startTimestamp, policyAuditContext);
        } else if(executedPolicyResponse.getResult() instanceof PolicyService.Result.SystemError) {
            auditChassisException(executedPolicyResponse.getResult().getException(),
                    executedPolicyResponse, policyRequest, startTimestamp, policyAuditContext);
        } else {
            log.error("Policy evaluation operational audit failed due to unexpected state.");
        }

    }

    private void auditChassisException(@NonNull AbstractChassisException chassisException,
            @NonNull final PolicyService.ExecutedPolicyResponse response,
            @NonNull final PolicyRequest policyRequest, final Long startTimestamp,
            final PolicyAuditContext policyAuditContext) {
        final ErrorResponse chassisErrorResponse = conversionService.convert(
                chassisException, ErrorResponse.class);

        auditLogger.logChassisAudits(policyRequest,
                policyRequest.getPolicyName(),
                policyRequest.getPolicyVersionRequested(),
                policyRequest.getStep(), chassisErrorResponse, response, chassisException);
        policyEvaluatedAuditor.auditChassisError(policyRequest, chassisException,
                chassisErrorResponse, response, startTimestamp, policyAuditContext);
    }

    private PolicyRequest getPlaceholderPolicyRequest(final PolicyAuditContext policyAuditContext) {
        return PolicyRequest.builder()
                .policyName(String.valueOf(policyAuditContext.getPolicyName()))
                .policyVersionRequested(String.valueOf(policyAuditContext.getPolicyVersionRequested()))
                .policyInfo(Optional.empty())
                .step(policyAuditContext.getStep())
                .businessEvent(policyAuditContext.getBusinessEvent())
                .xJourneyPolicy(policyAuditContext.getXJourneyPolicy())
                .clientId(policyAuditContext.getClientId())
                .policyParametersAudit("N/A")
                .processId(policyAuditContext.getProcessId())
                .policyRequestType(policyAuditContext.getPolicyRequestType())
                .dxHeaders(new HashMap<>())
                .childPolicy(policyAuditContext.getParentPolicyName() != null)
                .build();
    }

    private PolicyService.ExecutedPolicyResponse getPlaceholderExecutePolicyResponse(final @NonNull Exception e) {
        final ErrorInfo errorInfo = new ErrorInfo();
        errorInfo.setId((e instanceof AbstractChassisException) ?
                ((AbstractChassisException)e).getApiError().getId() : null);
        errorInfo.setText("Our system experienced an error. Please try again later");
        errorInfo.setDeveloperText(e.getMessage());
        final PolicyResponse policyResponse = PolicyResponse.builder()
                .policyStatus(PolicyStatus.FAILURE)
                .errorInfo(errorInfo)
                .build();
        return PolicyService.ExecutedPolicyResponse.builder()
                .setResult(new PolicyService.Result.PolicySuccess(policyResponse))
                .build();
    }

}
