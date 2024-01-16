package com.capitalone.identity.identitybuilder.policycore.service.dao.impl;

import com.capitalone.chassis.engine.consumerservices.rest.oauth2.ChassisOAuth2RestTemplate;
import com.capitalone.identity.identitybuilder.policycore.model.DevExchangeRequest;
import com.capitalone.identity.identitybuilder.policycore.model.PartnerTokenResponse;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PartnerTokenGeneratorTest {

    @Mock
    private DevExchangeRestTemplateFactory devExchangeRestTemplateFactory;

    @Mock
    private ChassisOAuth2RestTemplate oAuth2RestTemplate;

    @Mock
    private PartnerTokenResponse vendorTokenResponse;

    @Mock
    private HttpHeaders httpHeaders;

    @Mock
    private DevExchangeRequest request;


    @InjectMocks
    private PartnerTokenGenerator generator;

    @BeforeEach
    public void setup() {
        lenient().when(devExchangeRestTemplateFactory.getDevExchangeRestTemplate(any(), any())).thenReturn(oAuth2RestTemplate);
        lenient().when(oAuth2RestTemplate.exchange(Mockito.nullable(String.class), any(HttpMethod.class), any(), eq(PartnerTokenResponse.class))).thenReturn(new ResponseEntity<>(vendorTokenResponse, HttpStatus.OK));
    }

    @Test
    public void testGenerateToken() throws IOException, URISyntaxException {
        when(request.getHttpHeaders()).thenReturn(httpHeaders);
        when(request.getUri()).thenReturn(new URI("https","", "partner-apis-it.cloud.capitalone.com", 443, "/third-party/payfone/services/token", "accessTokenUri=accessTokenUri&accept=accept",""));
        generator.generateToken(request);
        verify(request, times(1)).setUri(any());
        verify(request.getHttpHeaders(), times(1)).add(any(), any());
    }

    @Test
    public void testGenerateTokenWithoutPassingCredentialsAsQueryParams() throws IOException, URISyntaxException {
        DevExchangeRequest request = new DevExchangeRequest();
        request.setHttpHeaders(new HttpHeaders());
        request.setUri(new URI("https","", "partner-apis-it.cloud.capitalone.com", 443, "/third-party/payfone/services/token", "accessTokenUri=accessTokenUri&accept=accept&username=username&password=password&grant_type=grant_type&client_id=client_id&sub_client_id=sub_client_id",""));
        generator.generateToken(request);
        URI uri = request.getUri();
        Assertions.assertNull(request.getUri().getQuery());
    }

    @Test
    public void invalidPartnerToken() throws IOException, URISyntaxException {
        DevExchangeRequest request = new DevExchangeRequest();
        request.setHttpHeaders(new HttpHeaders());
        request.setUri(new URI("https", "", "partner-apis-it.cloud.capitalone.com", 443, "/third-party/payfone/services/token", "accessTokenUri=accessTokenUri&accept=accept&username=username&password=password&grant_type=grant_type&client_id=client_id&sub_client_id=sub_client_id", ""));
        lenient().when(oAuth2RestTemplate.exchange(Mockito.nullable(String.class), any(HttpMethod.class), any(), eq(PartnerTokenResponse.class))).thenThrow(new RestClientException("Invalid Token"));
        String expMessage = assertThrows(RestClientException.class, () -> generator.generateToken(request)).getMessage();
        assertEquals(expMessage, "Invalid Token");
    }

    @Test
    public void expiredPartnerToken() throws IOException, URISyntaxException {
        DevExchangeRequest request = new DevExchangeRequest();
        request.setHttpHeaders(new HttpHeaders());
        request.setUri(new URI("https", "", "partner-apis-it.cloud.capitalone.com", 443, "/third-party/payfone/services/token", "accessTokenUri=accessTokenUri&accept=accept&username=username&password=password&grant_type=grant_type&client_id=client_id&sub_client_id=sub_client_id", ""));
        //Initial Request sets the expiresAt to currentTime minus one minute as the default values of expires_in is 0
        generator.generateToken(request);
        //Make a second request to generate token. Since the currentTime is greater than the expiresAt, Token will be generated again.
        generator.generateToken(request);
        //Verify Token api is called twice.
        verify(oAuth2RestTemplate, times(2)).exchange(Mockito.nullable(String.class), any(HttpMethod.class), any(), eq(PartnerTokenResponse.class));
        verify(devExchangeRestTemplateFactory, times(2)).getDevExchangeRestTemplate(any(), any());
    }
}