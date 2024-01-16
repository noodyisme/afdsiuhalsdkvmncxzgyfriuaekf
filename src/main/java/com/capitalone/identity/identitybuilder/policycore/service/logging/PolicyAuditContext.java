package com.capitalone.identity.identitybuilder.policycore.service.logging;

import com.capitalone.identity.identitybuilder.model.PolicyInfo;
import com.capitalone.identity.identitybuilder.policycore.camel.PolicyConstants;
import com.capitalone.identity.identitybuilder.policycore.model.APIRequest;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.model.policy.PolicyRequestType;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import org.apache.camel.Message;

import java.util.Map;

import static com.capitalone.identity.identitybuilder.policycore.camel.util.CamelCacheUtil.CACHE_KEY;

@Value
@Builder
public class PolicyAuditContext {

    String policyName;
    String policyVersionRequested;
    String step;
    String businessEvent;
    String xJourneyPolicy;
    String clientId;
    String processId;
    PolicyRequestType policyRequestType;
    String parentPolicyName;
    String contentType;
    String acceptLanguage;
    String domain;
    String subDomain;
    String userAgent;
    String accept;

    public static PolicyAuditContext of(final @NonNull APIRequest apiRequest,
            final @NonNull PolicyRequestType policyRequestType) {
        return PolicyAuditContext.builder()
                .policyName(apiRequest.getPolicyName())
                .policyVersionRequested(apiRequest.getPolicyVersion())
                .step(apiRequest.getStep())
                .businessEvent(apiRequest.getBusinessEvent())
                .xJourneyPolicy(apiRequest.getXJourneyPolicy())
                .clientId(apiRequest.getClientId())
                .processId(apiRequest.getProcessId())
                .policyRequestType(policyRequestType)
                .parentPolicyName(null)
                .contentType(apiRequest.getContentType())
                .acceptLanguage(apiRequest.getLanguage())
                .domain(apiRequest.getDomain())
                .subDomain(apiRequest.getSubDomain())
                .userAgent(apiRequest.getUserAgent())
                .accept(apiRequest.getAccept())
                .build();
    }

    public static PolicyAuditContext of(
            final @NonNull PolicyInfo.Patch policyInfo,
            final @NonNull Message message,
            final @NonNull Map<String, String> dxHeaders,
            final @NonNull PolicyRequestType policyRequestType,
            final String parentPolicyName) {
        return PolicyAuditContext.builder()
                .policyName(policyInfo.getPolicyShortName())
                .policyVersionRequested(policyInfo.getPolicyVersion())
                .step(null)
                .businessEvent( (String) message.getHeader(PolicyConstants.HEADER_BUSINESS_EVENT))
                .xJourneyPolicy(dxHeaders.get("x-journey-policy"))
                .clientId( (String) message.getHeader(PolicyConstants.HEADER_CLIENTID))
                .processId( (String) message.getHeader(CACHE_KEY))
                .policyRequestType(policyRequestType)
                .parentPolicyName(parentPolicyName)
                .build();
    }
}
