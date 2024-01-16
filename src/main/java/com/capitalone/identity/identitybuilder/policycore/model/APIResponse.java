package com.capitalone.identity.identitybuilder.policycore.model;

import com.capitalone.identity.identitybuilder.decisionengine.service.audit.exceptions.ErrorDetails;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.util.List;

@NoArgsConstructor(access= AccessLevel.PRIVATE)
public class APIResponse {
    @Value
    @Builder
    @EqualsAndHashCode(callSuper = true)
    public static class APISuccessResponse extends  APIResponse{

        Object results;
        @NonNull
        PolicyStatus policyStatus;
        @JsonInclude(JsonInclude.Include.NON_NULL)
        ErrorInfo errorInfo;
        @JsonInclude(JsonInclude.Include.NON_NULL)
        ProcessMetadata metadata;
    }
    @Value
    @Builder
    @EqualsAndHashCode(callSuper = true)
    public static class APIFailureResponse extends APIResponse {
        @JsonInclude(JsonInclude.Include.NON_NULL)
        String id;
        @JsonInclude(JsonInclude.Include.NON_NULL)
        String text;
        @JsonInclude(JsonInclude.Include.NON_NULL)
        List<String> actions;
        @JsonInclude(JsonInclude.Include.NON_NULL)
        String developerText;
        @JsonInclude(JsonInclude.Include.NON_NULL)
        List<ErrorDetails> errorDetails;
    }
}
