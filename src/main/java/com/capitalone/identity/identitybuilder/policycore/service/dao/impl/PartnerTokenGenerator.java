package com.capitalone.identity.identitybuilder.policycore.service.dao.impl;

import com.capitalone.identity.identitybuilder.policycore.model.DevExchangeRequest;
import com.capitalone.identity.identitybuilder.policycore.model.PartnerTokenResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.*;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;


/**
 * All calls made through partner gateway also require a partner token to be able to authenticate with the partner Authorization server.
 * This class generates the partner token and set the token to http header 'x-authorization'
 *
 * @deprecated
 */
@Deprecated
public class PartnerTokenGenerator {

    /**
     * Logger.
     */
    private final Logger logger = LogManager.getLogger(getClass());

    private DevExchangeRestTemplateFactory devExchangeRestTemplateFactory;

    private String vendorAccessToken;

    private long expiresAt;

    public static final String ACCESS_TOKEN_URI = "accessTokenUri";

    public static final String ACCEPT_HEADER = "accept";

    public static final long ONE_MINUTE_IN_MILLIS = 60000;


    public PartnerTokenGenerator(DevExchangeRestTemplateFactory devExchangeRestTemplateFactory){
        this.devExchangeRestTemplateFactory = devExchangeRestTemplateFactory;
    }

    public void generateToken(DevExchangeRequest devexchangeRequest)  {
        if(System.currentTimeMillis() >= expiresAt) {
            MultiValueMap<String, String> parameters = UriComponentsBuilder.fromUri(devexchangeRequest.getUri()).build().getQueryParams();
            String accessTokenUri = parameters.getFirst(ACCESS_TOKEN_URI);
            String acceptHeader = parameters.getFirst(ACCEPT_HEADER);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            headers.set(ACCEPT_HEADER, acceptHeader);
            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(parameters, headers);
            OAuth2RestTemplate template = devExchangeRestTemplateFactory.getDevExchangeRestTemplate(devexchangeRequest.getServiceName()+"_partner_token", devexchangeRequest.getScheme());
            ResponseEntity<PartnerTokenResponse> vendorTokenResponse = template.exchange(accessTokenUri, HttpMethod.POST, request, PartnerTokenResponse.class);
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

