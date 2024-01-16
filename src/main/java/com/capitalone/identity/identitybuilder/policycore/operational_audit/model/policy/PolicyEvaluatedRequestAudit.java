package com.capitalone.identity.identitybuilder.policycore.operational_audit.model.policy;

import com.capitalone.identity.identitybuilder.policycore.model.PolicyRequest;
import com.capitalone.identity.identitybuilder.policycore.service.logging.PolicyAuditContext;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

@Builder
@Value
public class PolicyEvaluatedRequestAudit {
    @NonNull PolicyRequest policyRequest;
    @NonNull PolicyAuditContext policyAuditContext;
}
