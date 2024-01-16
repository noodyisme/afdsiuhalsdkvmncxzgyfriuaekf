package com.capitalone.identity.identitybuilder.policycore.abac.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Value;

import javax.annotation.Nullable;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Value
public class AbacResponse {
    boolean strictEnforcementFlag;
    boolean isAuthorized;
    @Nullable
    String text;
    @Nullable
    String errorCode;
}
