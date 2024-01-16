package com.capitalone.identity.identitybuilder.policycore.rest.v1;

import io.swagger.annotations.ApiParam;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.ws.rs.HeaderParam;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExecutionContext {
    @ApiParam(value = "Token used to identify the calling application to which Capital One has granted permission to call this API")
    @HeaderParam("Api-Key")
    String apiKey;

    @ApiParam(value = "Token used to identify the client application to which Capital One has granted permission to call this API")
    @HeaderParam("Client-Api-Key")
    String clientApiKey;

    @ApiParam(value = "Business event for this request")
    @HeaderParam("Business-Event")
    String businessEvent;

    @ApiParam(value = "It is an optional field that clients can provide to any eAPI service to track traffic across different APIs. If this ID is provided, its value will be added to Audit. Otherwise it will be ignored. Clients who utilize this are responsible for generating the value, guaranteeing uniqueness, and providing it as a Header in every request.")
    @HeaderParam("Client-Correlation-Id")
    String clientCorrelationId;

    @ApiParam(value = "IP address of the customer system.")
    @HeaderParam("Customer-IP-Address")
    String customerIpAddress;

    @ApiParam(value = "Content Type of the System")
    @HeaderParam("Content-Type")
    String contentType;

    @ApiParam(value = "Through what channel is the request coming")
    @HeaderParam("Channel-Type")
    String channelType;

    @ApiParam(value = "Country code that customers has selected")
    @HeaderParam("Country-Code")
    String countryCode;

    @ApiParam(value = "List of acceptable human languages for response")
    @HeaderParam("Accept-Language")
    String language;

    @ApiParam(value = "The caller's Journey policy (format <policy>:<version>[:<process_id>")
    @HeaderParam("x-journey-policy")
    String xJourneyPolicy;

    @ApiParam(value = "Acceptable domain values for the service") @HeaderParam("Domain")
    String domain;

    @ApiParam(value = "Client Organization Group") @HeaderParam("Sub-Domain")
    String subDomain;

    @ApiParam(value = "User agent string that is captured from the customers browser / device") @HeaderParam("User-Agent")
    String userAgent;

    @ApiParam(value = "Informs which content type client can understand as part of response.") @HeaderParam("Accept")
    String accept;

    @HeaderParam("customerIdToken")
    String customerIdToken;

    @ApiParam(value = "The Session Correlation ID")
    @HeaderParam("session-correlation-id")
    String sessionCorrelationId;

    @ApiParam(value = "Client Id for the organization") @HeaderParam("client_id")
    String clientId;
}
