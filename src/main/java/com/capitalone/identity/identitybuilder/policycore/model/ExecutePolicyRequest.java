package com.capitalone.identity.identitybuilder.policycore.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import java.io.Serializable;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExecutePolicyRequest implements Serializable {
    private static final long serialVersionUID = 6204213683962480645L;
    private Map<String, Serializable> policyParameters;
    private String policyParametersAudit;

    public ExecutePolicyRequest(@NonNull PolicyRequest policyRequest) {
        this.policyParameters = policyRequest.getRequestBody();
        this.policyParametersAudit = policyRequest.getPolicyParametersAudit();
    }
}
