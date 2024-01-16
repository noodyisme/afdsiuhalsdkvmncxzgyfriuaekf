package com.capitalone.identity.identitybuilder.policycore.service.dao.impl;

import com.capitalone.identity.identitybuilder.policycore.model.DevExchangeRequest;
import com.capitalone.identity.identitybuilder.policycore.model.PartnerTokenResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.*;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

/**
 * All calls made through partner gateway also require a partner token to be able to authenticate with the partner Authorization server.
 * This class generates the partner token and set the token to http header 'x-authorization'
 */
@ConditionalOnProperty({"identity.identitybuilder.policycore.service.webclient.enabled"})
public class PartnerWebClientTokenGenerator {
    private DevExchangeWebClientFactory devExchangeWebClientFactory;

    private String vendorAccessToken;

    private long expiresAt;

    public static final String ACCESS_TOKEN_URI = "accessTokenUri";

    public static final String ACCEPT_HEADER = "accept";

    public static final long ONE_MINUTE_IN_MILLIS = 60000;


    public PartnerWebClientTokenGenerator(DevExchangeWebClientFactory devExchangeWebClientFactory){
        this.devExchangeWebClientFactory = devExchangeWebClientFactory;
    }

    public void generateToken(DevExchangeRequest devexchangeRequest)  {
        if(System.currentTimeMillis() >= expiresAt) {
            MultiValueMap<String, String> parameters = UriComponentsBuilder.fromUri(devexchangeRequest.getUri()).build().getQueryParams();
            String accessTokenUri = parameters.getFirst(ACCESS_TOKEN_URI);
            String acceptHeader = parameters.getFirst(ACCEPT_HEADER);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            headers.set(ACCEPT_HEADER, acceptHeader);
            WebClient webClient = devExchangeWebClientFactory.getDevExchangeWebClient(devexchangeRequest.getServiceName()+"_partner_token", devexchangeRequest.getScheme());
            ResponseEntity<PartnerTokenResponse> vendorTokenResponse = webClient
                    .post()
                    .uri(accessTokenUri)
                    .headers(httpHeaders -> httpHeaders.addAll(headers))
                    .bodyValue(parameters)
                    .retrieve()
                    .toEntity(PartnerTokenResponse.class)
                    .block();
            vendorAccessToken = vendorTokenResponse.getBody().getAccessToken();
            expiresAt = System.currentTimeMillis() + (vendorTokenResponse.getBody().getExpiresIn() * 1000) - ONE_MINUTE_IN_MILLIS;
        }
        URI uri = UriComponentsBuilder.fromUri(devexchangeRequest.getUri()).replaceQueryParam(ACCESS_TOKEN_URI).replaceQueryParam(ACCEPT_HEADER)
                .replaceQueryParam("username").replaceQueryParam("password")
                .replaceQueryParam("grant_type").replaceQueryParam("client_id").replaceQueryParam("sub_client_id").build().toUri();
        devexchangeRequest.setUri(uri);
        devexchangeRequest.getHttpHeaders().add("x-authorization", "Bearer "+ vendorAccessToken);
    }
}

