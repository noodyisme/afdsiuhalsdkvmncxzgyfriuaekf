package com.capitalone.identity.identitybuilder.policycore.abac.service;

import com.capitalone.identity.identitybuilder.model.abac.AccessGrant;
import com.capitalone.identity.identitybuilder.model.abac.PolicyAccess;
import com.capitalone.identity.identitybuilder.policycore.abac.config.PolicyAccessRegistry;
import com.capitalone.identity.identitybuilder.policycore.abac.model.AbacResponse;
import com.capitalone.identity.identitybuilder.policycore.abac.model.PolicyAuthAction;
import com.capitalone.identity.identitybuilder.policycore.abac.model.PolicyAuthResource;
import com.capitalone.identity.identitybuilder.policycore.abac.model.PolicyAuthSubject;
import com.capitalone.identity.identitybuilder.policycore.service.constants.ApplicationConstants;
import lombok.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class PolicyAuthorizationEnforcer implements AuthorizationEnforcer<PolicyAuthSubject, PolicyAuthResource, PolicyAuthAction> {

    private final boolean strictEnforcement;
    private final PolicyAccessRegistry policyAccessRegistry;
    @Value("${chassis.app.code}")
    private String applicationId;

    public PolicyAuthorizationEnforcer(
            @Value("${identitybuilder.policycore.abac-enforcement.strict:false}") boolean strictEnforcement,
            PolicyAccessRegistry policyAccessRegistry) {
        this.strictEnforcement = strictEnforcement;
        this.policyAccessRegistry = policyAccessRegistry;
    }

    @Override
    public AbacResponse authorize(@NonNull PolicyAuthSubject policyAuthSubject,
                                  @NonNull PolicyAuthResource policyAuthResource,
                                  @NonNull PolicyAuthAction policyAuthAction) {
        switch (policyAuthAction) {
            case GET:
                return new AbacResponse(strictEnforcement, true, null, null);
            case INITIATE:
                if (applicationId.equals(policyAuthSubject.getClientId())) {
                    return new AbacResponse(strictEnforcement, true, null, null);
                } else {
                    return enforceAuthorization(policyAuthSubject, policyAuthResource);
                }
            case RESUME:
                return enforceAuthorization(policyAuthSubject, policyAuthResource);
            default: // Exhaustive case
                throw new UnsupportedOperationException("No such actions are allowed");
        }
    }

    private AbacResponse enforceAuthorization(@NonNull PolicyAuthSubject subject,
                                              @NonNull PolicyAuthResource resource) {

        final PolicyAccess policyAccess = policyAccessRegistry.getPolicyAccess(resource.getPolicyName(),
                resource.getPolicyMajorVersion());

        if (policyAccess == null) {
            return new AbacResponse(strictEnforcement,
                    mapIsAuthorized(false),
                    ApplicationConstants.ABAC_POLICY_ACCESS_UNAVAILABLE_DEV_TEXT,
                    ApplicationConstants.ABAC_POLICY_ACCESS_UNAVAILABLE_ERROR_CODE);
        }

        final AccessGrant grant = policyAccess.getAccess(subject.getClientId());
        switch (grant) {
            case ALLOW:
                return new AbacResponse(strictEnforcement,
                        mapIsAuthorized(true),
                        null,
                        null);
            case DENY:
                return new AbacResponse(strictEnforcement,
                        mapIsAuthorized(false),
                        ApplicationConstants.ABAC_CLIENT_DENIED_DEV_TEXT,
                        ApplicationConstants.ABAC_CLIENT_DENIED_ERROR_CODE);
            case UNDEFINED:
                return new AbacResponse(strictEnforcement,
                        mapIsAuthorized(false),
                        ApplicationConstants.ABAC_CLIENT_DOES_NOT_EXIST_DEV_TEXT,
                        ApplicationConstants.ABAC_CLIENT_DOES_NOT_EXIST_ERROR_CODE);
            default:
                throw new UnsupportedOperationException("grant not recognized " + grant);
        }
    }

    private boolean mapIsAuthorized(boolean authorizationEvaluationResult) {
        if (!this.strictEnforcement)
            return true;
        else
            return authorizationEvaluationResult;
    }
}
