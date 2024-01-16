package com.capitalone.identity.identitybuilder.policycore.model;

import com.capitalone.identity.identitybuilder.policycore.service.util.PathParamUtil;
import io.swagger.annotations.ApiParam;
import lombok.Builder;
import lombok.Value;

import javax.ws.rs.HeaderParam;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;

@Value
public class PublicAPIRequest {
    @ApiParam(value = "Token used to identify the calling application to which Capital One has granted permission to call this API")
    String apiKey;

    @ApiParam(value = "Token used to identify the client application to which Capital One has granted permission to call this API")
    String clientApiKey;

    @ApiParam(value = "It is an optional field that clients can provide to any eAPI service to track traffic across different APIs. If this ID is provided, its value will be added to Audit. Otherwise it will be ignored. Clients who utilize this are responsible for generating the value, guaranteeing uniqueness, and providing it as a Header in every request.")
    String clientCorrelationId;

    @ApiParam(value = "IP address of the customer system.")
    String customerIpAddress;

    @ApiParam(value = "Content Type of the System")
    String contentType;

    @ApiParam(value = "Through what channel is the request coming")
    String channelType;

    @ApiParam(value = "Country code that customers has selected")
    String countryCode;

    @ApiParam(value = "List of acceptable human languages for response")
    String language;

    @ApiParam(value = "Acceptable domain values for the service")
    String domain;

    @ApiParam(value = "Client Organization Group")
    String subDomain;

    @ApiParam(value = "User agent string that is captured from the customers browser / device")
    String userAgent;

    @ApiParam(value = "Informs which content type client can understand as part of response.")
    String accept;

    @ApiParam(value = "Client Id for the organization")
    String clientId;

    @ApiParam(value = "Business event for this request")
    String businessEvent;

    @ApiParam(value = "The caller's Journey policy (format <policy>:<version>[:<process_id>")
    String xJourneyPolicy;

    String policyName;

    String policyVersion;

    String processId;

    String step;

    String customerIdToken;
    @ApiParam(value = "Session Correlation ID")
    String sessionCorrelationId;
    @ApiParam(value = "Override Flag for Mock execution")
    Boolean mockModeFlag;

    @Builder(toBuilder = true)
    public PublicAPIRequest(@HeaderParam("Api-Key") String apiKey, @HeaderParam("Client-Api-Key") String clientApiKey, @HeaderParam("Client-Correlation-Id") String clientCorrelationId, //NOSONAR
                            @HeaderParam("Customer-IP-Address") String customerIpAddress, @HeaderParam("Content-Type") String contentType, @HeaderParam("Channel-Type") String channelType,
                            @HeaderParam("Country-Code") String countryCode, @HeaderParam("Accept-Language") String language, @HeaderParam("Domain") String domain, @HeaderParam("Sub-Domain") String subDomain,
                            @HeaderParam("User-Agent") String userAgent, @HeaderParam("Accept") String accept, @HeaderParam("client_id") String clientId, @HeaderParam("Business-Event") String businessEvent,
                            @HeaderParam("x-journey-policy") String xJourneyPolicy, @HeaderParam("customerIdToken") String customerIdToken, @HeaderParam("session-correlation-id") String sessionCorrelationId,
                            @QueryParam("policyName") String policyName, @QueryParam("policyVersion") String policyVersion, @PathParam("resourceId") String processId, @QueryParam("step") String step,
                            @HeaderParam("policy-core.mockMode") Boolean mockModeFlag) {
        this.apiKey = apiKey;
        this.clientApiKey = clientApiKey;
        this.clientCorrelationId = clientCorrelationId;
        this.customerIpAddress = customerIpAddress;
        this.contentType = contentType;
        this.channelType = channelType;
        this.countryCode = countryCode;
        this.language = language;
        this.domain = domain;
        this.subDomain = subDomain;
        this.userAgent = userAgent;
        this.accept = accept;
        this.clientId = clientId;
        this.businessEvent = businessEvent;
        this.xJourneyPolicy = xJourneyPolicy;
        this.customerIdToken = customerIdToken;
        this.sessionCorrelationId = sessionCorrelationId;
        this.policyName = PathParamUtil.cleanParameter(policyName);
        this.policyVersion = PathParamUtil.cleanParameter(policyVersion);
        this.processId = PathParamUtil.cleanParameter(processId);
        this.step = PathParamUtil.cleanParameter(step);
        this.mockModeFlag = mockModeFlag;
    }

    public static APIRequest toAPIRequest(PublicAPIRequest publicApiRequest){
        return new APIRequest(publicApiRequest.getApiKey(),
            publicApiRequest.getClientApiKey(),
            publicApiRequest.getClientCorrelationId(),
            publicApiRequest.getCustomerIpAddress(),
            publicApiRequest.getContentType(),
            publicApiRequest.getChannelType(),
            publicApiRequest.getCountryCode(),
            publicApiRequest.getLanguage(),
            publicApiRequest.getDomain(),
            publicApiRequest.getSubDomain(),
            publicApiRequest.getUserAgent(),
            publicApiRequest.getAccept(),
            publicApiRequest.getClientId(),
            publicApiRequest.getBusinessEvent(),
            publicApiRequest.getXJourneyPolicy(),
            publicApiRequest.getCustomerIdToken(),
            publicApiRequest.getSessionCorrelationId(),
            publicApiRequest.getPolicyName(),
            publicApiRequest.getPolicyVersion(),
            publicApiRequest.getProcessId(),
            publicApiRequest.getStep(),
            publicApiRequest.getMockModeFlag());
    }
}
