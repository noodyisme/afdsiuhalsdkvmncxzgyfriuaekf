package com.capitalone.identity.identitybuilder.policycore.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.util.Optional;

/**
 * Response from an executed policy (or step). Returned by POST/PATCH methods.
 */
@Value
public class JourneyPolicyResponse {

    @NonNull
    Object policyResponseBody;
    @NonNull
    PolicyStatus policyStatus;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    ErrorInfo errorInformation;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    JourneyProcessMetadata metadata;

    public JourneyPolicyResponse(APIResponse.APISuccessResponse policyCoreResponse) {
        this.policyResponseBody = policyCoreResponse.getResults();
        this.policyStatus = policyCoreResponse.getPolicyStatus();
        this.errorInformation = policyCoreResponse.getErrorInfo();
        this.metadata = Optional.ofNullable(policyCoreResponse.getMetadata())
                .map(JourneyProcessMetadata::new).orElse(null);
    }
}
