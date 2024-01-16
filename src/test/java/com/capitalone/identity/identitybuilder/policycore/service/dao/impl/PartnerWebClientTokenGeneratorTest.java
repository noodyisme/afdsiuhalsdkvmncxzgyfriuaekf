package com.capitalone.identity.identitybuilder.policycore.service.dao.impl;

import com.capitalone.identity.identitybuilder.policycore.model.DevExchangeRequest;
import com.capitalone.identity.identitybuilder.policycore.model.PartnerTokenResponse;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.net.URISyntaxException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PartnerWebClientTokenGeneratorTest {

    @Mock
    private DevExchangeWebClientFactory devExchangeWebClientFactory;

    @Mock
    private WebClient webClient;

    @Mock
    private PartnerTokenResponse vendorTokenResponse;

    @Mock
    private HttpHeaders httpHeaders;

    @Mock
    private DevExchangeRequest request;

    @Mock
    private WebClient.RequestHeadersSpec requestHeadersMock;
    @Mock
    private WebClient.RequestBodySpec requestBodyMock;
    @Mock
    private WebClient.RequestBodyUriSpec requestBodyUriMock;
    @Mock
    private WebClient.ResponseSpec responseMock;


    @InjectMocks
    private PartnerWebClientTokenGenerator generator;

    @BeforeEach
    public void setup() {
        lenient().when(devExchangeWebClientFactory.getDevExchangeWebClient(any(), any())).thenReturn(webClient);
        when(webClient.post()).thenReturn(requestBodyUriMock);
        when(requestBodyUriMock.uri(anyString())).thenReturn(requestBodyMock);
        when(requestBodyMock.headers(any())).thenReturn(requestBodyMock);
        when(requestBodyMock.bodyValue(any())).thenReturn(requestHeadersMock);
        when(requestHeadersMock.retrieve()).thenReturn(responseMock);
        lenient().when(responseMock.toEntity(eq(PartnerTokenResponse.class))).thenReturn(Mono.just(new ResponseEntity<>(vendorTokenResponse, HttpStatus.OK)));
    }

    @Test
    public void testGenerateToken() throws URISyntaxException {
        when(request.getHttpHeaders()).thenReturn(httpHeaders);
        when(request.getUri()).thenReturn(new URI("https","", "partner-apis-it.cloud.capitalone.com", 443, "/third-party/payfone/services/token", "accessTokenUri=accessTokenUri&accept=accept",""));
        generator.generateToken(request);
        verify(request, times(1)).setUri(any());
        verify(request.getHttpHeaders(), times(1)).add(any(), any());
    }

    @Test
    public void testGenerateTokenWithoutPassingCredentialsAsQueryParams() throws URISyntaxException {
        DevExchangeRequest request = new DevExchangeRequest();
        request.setHttpHeaders(new HttpHeaders());
        request.setUri(new URI("https","", "partner-apis-it.cloud.capitalone.com", 443, "/third-party/payfone/services/token", "accessTokenUri=accessTokenUri&accept=accept&username=username&password=password&grant_type=grant_type&client_id=client_id&sub_client_id=sub_client_id",""));
        generator.generateToken(request);
        URI uri = request.getUri();
        Assertions.assertNull(uri.getQuery());
    }

    @Test
    public void invalidPartnerToken() throws URISyntaxException {
        DevExchangeRequest request = new DevExchangeRequest();
        request.setHttpHeaders(new HttpHeaders());
        request.setUri(new URI("https", "", "partner-apis-it.cloud.capitalone.com", 443, "/third-party/payfone/services/token", "accessTokenUri=accessTokenUri&accept=accept&username=username&password=password&grant_type=grant_type&client_id=client_id&sub_client_id=sub_client_id", ""));
        when(requestHeadersMock.retrieve()).thenThrow(WebClientResponseException.create(HttpStatus.UNAUTHORIZED.value(), "Invalid Token", null, null, null));
        String expMessage = assertThrows(WebClientResponseException.class, () -> generator.generateToken(request)).getMessage();
        assertEquals("401 Invalid Token", expMessage);
    }

    @Test
    public void expiredPartnerToken() throws URISyntaxException {
        DevExchangeRequest request = new DevExchangeRequest();
        request.setHttpHeaders(new HttpHeaders());
        request.setUri(new URI("https", "", "partner-apis-it.cloud.capitalone.com", 443, "/third-party/payfone/services/token", "accessTokenUri=accessTokenUri&accept=accept&username=username&password=password&grant_type=grant_type&client_id=client_id&sub_client_id=sub_client_id", ""));
        //Initial Request sets the expiresAt to currentTime minus one minute as the default values of expires_in is 0
        generator.generateToken(request);
        request.setHttpHeaders(new HttpHeaders());
        request.setUri(new URI("https", "", "partner-apis-it.cloud.capitalone.com", 443, "/third-party/payfone/services/token", "accessTokenUri=accessTokenUri&accept=accept&username=username&password=password&grant_type=grant_type&client_id=client_id&sub_client_id=sub_client_id", ""));
        //Make a second request to generate token. Since the currentTime is greater than the expiresAt, Token will be generated again.
        generator.generateToken(request);
        //Verify Token api is called twice.
        //verify(oAuth2RestTemplate, times(2)).exchange(Mockito.nullable(String.class), any(HttpMethod.class), any(), eq(PartnerTokenResponse.class));
        verify(requestHeadersMock, times(2)).retrieve();
        verify(devExchangeWebClientFactory, times(2)).getDevExchangeWebClient(any(), any());
    }
}