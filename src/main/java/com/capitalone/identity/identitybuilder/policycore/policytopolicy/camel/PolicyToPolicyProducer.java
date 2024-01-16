package com.capitalone.identity.identitybuilder.policycore.policytopolicy.camel;

import com.capitalone.chassis.engine.model.exception.AbstractChassisException;
import com.capitalone.identity.identitybuilder.model.PolicyInfo;
import com.capitalone.identity.identitybuilder.policycore.camel.PolicyConstants;
import com.capitalone.identity.identitybuilder.policycore.camel.external.dynamic.PolicyValidator;
import com.capitalone.identity.identitybuilder.policycore.model.PolicyRequest;
import com.capitalone.identity.identitybuilder.policycore.model.PolicyResponse;
import com.capitalone.identity.identitybuilder.policycore.model.PolicyStatus;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.model.policy.PolicyRequestType;
import com.capitalone.identity.identitybuilder.policycore.service.PolicyService;
import com.capitalone.identity.identitybuilder.policycore.service.logging.PolicyAuditContext;
import com.capitalone.identity.identitybuilder.policycore.service.logging.PolicyServiceExecutor;
import com.capitalone.identity.identitybuilder.policycore.service.versions.PolicyVersionService;
import lombok.NonNull;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.support.DefaultProducer;

import javax.naming.OperationNotSupportedException;
import java.io.IOException;
import java.io.Serializable;
import java.util.Map;

import static com.capitalone.identity.identitybuilder.policycore.camel.util.CamelCacheUtil.CACHE_KEY;

public class PolicyToPolicyProducer extends DefaultProducer {


    private final PolicyServiceExecutor policyServiceExecutor;
    private final PolicyValidator policyValidator;
    private final PolicyVersionService policyVersionService;
    private final PolicyRequest.SystemParams systemParams;

    public PolicyToPolicyProducer(@NonNull PolicyToPolicyEndpoint endpoint, @NonNull PolicyServiceExecutor policyServiceExecutor, //NOSONAR
                                  @NonNull PolicyValidator policyValidator, @NonNull PolicyVersionService policyVersionService,
                                  @NonNull String fieldsToFilter, @NonNull String appName, @NonNull String applicationId) {
        super(endpoint);
        this.policyServiceExecutor = policyServiceExecutor;
        this.policyValidator = policyValidator;
        this.policyVersionService = policyVersionService;
        this.systemParams = new PolicyRequest.SystemParams(fieldsToFilter, appName, applicationId);
    }

    @Override
    public void process(Exchange exchange) throws IOException, OperationNotSupportedException {
        Message message = exchange.getIn();
        PolicyToPolicyEndpoint endpoint = getEndpoint();

        if(message.getHeader(PolicyConstants.HEADER_AVAILABLENEXTSTEPS) != null){
            throw new PolicyToPolicyException(String.format("The camel header \"" + PolicyConstants.HEADER_AVAILABLENEXTSTEPS +
                    "\" was set before calling child policy: \"%s\". Please ensure \"" + PolicyConstants.HEADER_AVAILABLENEXTSTEPS +
                    "\" is not set until after all P2P calls in the parent policy", endpoint.getEndpointUri()));
        }

        final PolicyInfo.Patch policyInfo = policyVersionService.getPolicyVersion(endpoint.getPolicyName(), endpoint.getPolicyVersion());

        if (policyInfo == null) {
            if (!endpoint.getPolicyVersion().contains(".")) {
                throw new PolicyToPolicyException(String.format("P2P unknown child policy: \"%s\". Please ensure at" +
                        " least one minor version is ACTIVE and that version forwarder is enabled", endpoint.getEndpointUri()));
            }
            throw new PolicyToPolicyException(String.format("P2P unknown child policy: \"%s\"", endpoint.getEndpointUri()));
        }

        String p2pPolicyUri = String.format("policy:%s_%s", policyInfo.getPolicyShortName(), policyInfo.getPolicyPatchVersionString());

        // If the P2P policy being called is not a child policy, throw an exception
        if (!policyValidator.getChildPolicies().contains(p2pPolicyUri)) {
            throw new PolicyToPolicyException(String.format("Policy \"%s\" is not marked as a child policy. All policies" +
                    " being invoked by P2P must be marked as child policies", endpoint.getEndpointUri()));
        }

        String parentPolicyName = exchange.getProperty("policyName", String.class);

        Map<String, String> dxHeaders = (Map<String, String>) message.getHeader(PolicyConstants.HEADER_DXHEADERS);
        Map<String, Serializable> body;
        try {
            body = (Map<String, Serializable>) message.getBody();
        } catch (ClassCastException e){
            throw new PolicyToPolicyException(String.format("Error converting the parent policy body to a Map when " +
                    "calling child policy: \"%s\"", endpoint.getEndpointUri()), e);
        }

        PolicyService.ExecutedPolicyResponse executionResponse = policyServiceExecutor.executePolicyWithAuditing(
                () -> new PolicyRequest(body, policyInfo,
                        null, (String) message.getHeader(PolicyConstants.HEADER_BUSINESS_EVENT),
                        dxHeaders.get("x-journey-policy"), systemParams.getApplicationId(), systemParams,
                        (String) message.getHeader(CACHE_KEY), PolicyRequestType.CREATE, null, dxHeaders, true,
                        message.getHeader(PolicyConstants.CUSTOMER_ID_TOKEN, String.class),
                        message.getHeader(PolicyConstants.HEADER_MOCKMODE, Boolean.class)),
                PolicyAuditContext.of(policyInfo, message, dxHeaders, PolicyRequestType.CREATE, parentPolicyName));
        final PolicyService.Result result = executionResponse.getResult();
        final PolicyResponse policyResponse = result.getPolicyResponse();
        final AbstractChassisException systemError = result.getException();
        if (policyResponse != null) {
            if (policyResponse.getPolicyStatus() == PolicyStatus.SUCCESS) {
                message.setBody(policyResponse.getResults());
            } else {
                throw new PolicyFailureException(policyResponse.getErrorInfo());
            }
        } else {
            // A system-level error occurred. Policy was not able to execute. API would return a non-200 here.
            // It's not reasonable for a policy to policy call to catch and handle this class of error.
            throw new PolicyToPolicyException(String.format("Policy \"%s\" " +
                    "returned the following error: " + systemError.getMessage(), endpoint.getEndpointUri()),
                    result.getException());
        }
    }

    @Override
    public PolicyToPolicyEndpoint getEndpoint() {
        return (PolicyToPolicyEndpoint) super.getEndpoint();
    }

}