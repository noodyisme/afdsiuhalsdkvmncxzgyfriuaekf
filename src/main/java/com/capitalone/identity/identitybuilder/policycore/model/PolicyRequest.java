package com.capitalone.identity.identitybuilder.policycore.model;

import com.capitalone.chassis.engine.model.exception.ChassisSystemException;
import com.capitalone.identity.identitybuilder.model.PolicyInfo;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.model.policy.PolicyRequestType;
import com.capitalone.identity.identitybuilder.policycore.service.util.JsonUtil;
import com.capitalone.identity.identitybuilder.policycore.service.util.PathParamUtil;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import org.jetbrains.annotations.NotNull;
import org.springframework.lang.Nullable;

import java.io.Serializable;
import java.util.*;

import static com.capitalone.identity.identitybuilder.policycore.service.constants.ApplicationConstants.*;

@Value
@Builder (toBuilder = true)
public class PolicyRequest {

    @JsonIgnore
    private static final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Nullable
    Map<String, Serializable> requestBody;

    @NonNull
    String policyName;

    @NonNull
    String policyVersionRequested;

    @NonNull
    Optional<PolicyInfo.Patch> policyInfo;

    @Nullable
    String step;

    @Nullable
    String businessEvent;

    @Nullable
    String xJourneyPolicy;

    @NonNull
    String clientId;

    @NonNull
    String policyParametersAudit;

    @NonNull
    String processId;

    @NonNull
    PolicyRequestType policyRequestType;

    @Nullable
    String customerIdToken;

    @Nullable
    @JsonIgnore
    PolicyState policyState;

    @Nullable
    Boolean mockModeFlag;

    @NonNull
    Map<String, String> dxHeaders;

    boolean childPolicy;

    @lombok.experimental.Tolerate
    public PolicyRequest(APIRequest apiRequest, @NotNull PolicyRequestType policyRequestType,
                         @Nullable Map<String, Serializable> requestBody, SystemParams systemParams) {
        if (apiRequest.getProcessId() == null) {
            this.processId = UUID.randomUUID().toString();
        } else {
            this.processId = apiRequest.getProcessId();
        }
        this.policyRequestType = policyRequestType;
        this.requestBody = requestBody;
        this.policyName = apiRequest.getPolicyName();
        this.policyVersionRequested = apiRequest.getPolicyVersion();
        this.step = apiRequest.getStep();
        this.businessEvent = apiRequest.getBusinessEvent();
        this.clientId = apiRequest.getClientId();
        this.policyParametersAudit = convertObjectToAuditString(requestBody, systemParams.getFieldsToFilter());
        if (apiRequest.getXJourneyPolicy() == null) {
            this.xJourneyPolicy = apiRequest.getPolicyName() + ":" + apiRequest.getPolicyVersion() + ":" + this.processId;
        } else {
            this.xJourneyPolicy = apiRequest.getXJourneyPolicy();
        }
        this.dxHeaders = buildDXHeaders(apiRequest, this.xJourneyPolicy);
        this.policyState = null;
        this.policyInfo = Optional.empty();
        this.customerIdToken = apiRequest.getCustomerIdToken();
        this.childPolicy = false;
        this.mockModeFlag = apiRequest.getMockModeFlag();
    }

    @lombok.experimental.Tolerate
    public PolicyRequest(APIRequest apiRequest, @NotNull PolicyRequestType policyRequestType, @Nullable Map<String, Serializable> requestBody,
                         @NotNull String policyName, @NotNull String policyVersion,
                         @Nullable String step, SystemParams systemParams, @NotNull PolicyState policyState) {
        if (apiRequest.getProcessId() == null) {
            this.processId = UUID.randomUUID().toString();
        } else {
            this.processId = apiRequest.getProcessId();
        }
        this.policyRequestType = policyRequestType;
        this.requestBody = requestBody;
        this.policyName = PathParamUtil.cleanParameter(policyName);
        this.policyVersionRequested = PathParamUtil.cleanParameter(policyVersion);
        if (step != null) {
            this.step = PathParamUtil.cleanParameter(step);
        } else {
            this.step = null;
        }
        this.businessEvent = apiRequest.getBusinessEvent();
        this.clientId = apiRequest.getClientId();
        this.policyParametersAudit = convertObjectToAuditString(requestBody, systemParams.getFieldsToFilter());
        if (apiRequest.getXJourneyPolicy() == null) {
            this.xJourneyPolicy = policyName + ":" + policyVersion + ":" + this.processId;
        } else {
            this.xJourneyPolicy = apiRequest.getXJourneyPolicy();
        }
        this.dxHeaders = buildDXHeaders(apiRequest, this.xJourneyPolicy);
        this.policyState = policyState;
        this.policyInfo = Optional.empty();
        this.childPolicy = false;
        this.customerIdToken = apiRequest.getCustomerIdToken();
        this.mockModeFlag = apiRequest.getMockModeFlag();
    }

    @lombok.experimental.Tolerate
    public PolicyRequest(@Nullable Map<String, Serializable> requestBody, @NonNull PolicyInfo.Patch policyInfo, //NOSONAR
                         @Nullable String step, @Nullable String businessEvent,
                         @Nullable String xJourneyPolicy, @NonNull String clientId, SystemParams systemParams,
                         @NonNull String processId, @NonNull PolicyRequestType policyRequestType,
                         @Nullable PolicyState policyState, Map<String, String> dxHeaders, boolean childPolicy,
                         @Nullable String customerIdToken, @Nullable Boolean mockModeFlag) {
        this.requestBody = requestBody;
        this.policyName = policyInfo.getPolicyShortName();
        this.policyVersionRequested = policyInfo.getPolicyVersion();
        this.policyInfo = Optional.of(policyInfo);
        if (step != null) {
            this.step = PathParamUtil.cleanParameter(step);
        } else {
            this.step = null;
        }
        this.businessEvent = businessEvent;
        this.xJourneyPolicy = xJourneyPolicy;
        this.clientId = clientId;
        this.policyParametersAudit = convertObjectToAuditString(requestBody, systemParams.getFieldsToFilter());
        this.processId = processId;
        this.policyRequestType = policyRequestType;
        this.policyState = policyState;
        this.dxHeaders = dxHeaders;
        this.childPolicy = childPolicy;
        this.customerIdToken = customerIdToken;
        this.mockModeFlag = mockModeFlag;
    }

    private String convertObjectToAuditString(Object object, String fieldsToFilter) {
        try {
            String resultsString = object != null ? objectMapper.writeValueAsString(object) : "";
            return JsonUtil.getFilteredResponse(resultsString, fieldsToFilter, "policyService");
        } catch (Exception e) {
            throw new ChassisSystemException("Error converting the object to string", e);
        }
    }

    /**
     * Return an unmodifiable map of request headers that should be automatically
     * passed to downstream APIs invoked through DevExchange.
     *
     * @param apiRequest The request from the client
     * @return a map of Dev Exchange headers (may be empty)
     */
    private Map<String, String> buildDXHeaders(APIRequest apiRequest, String xJourneyPolicy) {
        Map<String, String> map = new HashMap<>();

        // DX headers required by DCRP API so they are always there
        map.put(API_KEY, apiRequest.getApiKey());
        map.put(CHANNEL_TYPE, apiRequest.getChannelType());
        map.put(CLIENT_API_KEY, apiRequest.getClientApiKey() != null ? apiRequest.getClientApiKey() : apiRequest.getApiKey());
        map.put(CLIENT_CORRELATION_ID, apiRequest.getClientCorrelationId());
        map.put(CUSTOMER_IP_ADDR, apiRequest.getCustomerIpAddress());

        // DX headers used by some downstream APIs.
        if (apiRequest.getCountryCode() != null) {
            map.put(COUNTRY_CODE, apiRequest.getCountryCode());
        }

        // If the caller supplied a journey policy header, make sure we propagate it.
        if (xJourneyPolicy != null) {
            map.put(X_JOURNEY_POLICY, xJourneyPolicy);
        }

        if(apiRequest.getSessionCorrelationId() != null) {
            map.put(SESSION_CORRELATION_ID, apiRequest.getSessionCorrelationId());
        }

        if(apiRequest.getBusinessEvent() != null){
            map.put("Business-Event", apiRequest.getBusinessEvent());
        }
        return Collections.unmodifiableMap(map);
    }

    @Value
    public static class SystemParams {
        @NonNull String fieldsToFilter;
        String appName;
        String applicationId;
    }
}
