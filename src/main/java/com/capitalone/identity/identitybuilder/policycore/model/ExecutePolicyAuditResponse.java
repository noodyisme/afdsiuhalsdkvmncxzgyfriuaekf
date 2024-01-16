package com.capitalone.identity.identitybuilder.policycore.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

/**
 * Filtered results of a policy evaluation shared for audit purposes
 */
@Value
@Builder
public class ExecutePolicyAuditResponse {
    @NonNull
    PolicyStatus policyStatus;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    ErrorInfo errorInfo;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    ProcessMetadata metadata;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    String resultsAudit;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    String stepName;

    public static ExecutePolicyAuditResponse create(final PolicyResponse policyResponse,
            final String resultsAudit, final String stepName) {
        return ExecutePolicyAuditResponse.builder()
                .policyStatus(policyResponse.getPolicyStatus())
                .errorInfo(policyResponse.getErrorInfo())
                .metadata(policyResponse.getMetadata())
                .resultsAudit(resultsAudit)
                .stepName(stepName)
                .build();
    }
}
