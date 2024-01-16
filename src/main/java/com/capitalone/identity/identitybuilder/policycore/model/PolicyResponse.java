package com.capitalone.identity.identitybuilder.policycore.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import org.springframework.lang.NonNull;

@Value
@Builder
public class PolicyResponse {
    @JsonInclude(JsonInclude.Include.NON_NULL)
    Object results;
    @NonNull
    PolicyStatus policyStatus;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    ErrorInfo errorInfo;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    ProcessMetadata metadata;
    @JsonIgnore
    ErrorInfo suppressedErrorInfo;
}
