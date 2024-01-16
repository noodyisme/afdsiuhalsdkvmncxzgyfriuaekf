package com.capitalone.identity.identitybuilder.policycore.service.dao.impl;

import com.capitalone.identity.identitybuilder.policycore.model.DevExchangeRequest;
import com.capitalone.identity.identitybuilder.policycore.model.DevExchangeResponse;
import com.capitalone.identity.identitybuilder.policycore.service.constants.ApplicationConstants;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import javax.net.ssl.SSLException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class DevExchangeDAOWebImplTest {
    @Mock
    private WebClient webClient;
    @Mock
    private DevExchangeWebClientFactory devExchangeWebClientFactory;
    @Mock
    private PartnerWebClientTokenGenerator tokenGenerator;
    @Mock
    private PartnerTokenWebClientGeneratorFactory tokenGeneratorFactory;
    @Mock
    private WebClient.RequestHeadersSpec requestHeadersMock;
    @Mock
    private WebClient.RequestBodySpec requestBodyMock;
    @Mock
    private WebClient.RequestBodyUriSpec requestBodyUriMock;
    @Mock
    private WebClient.ResponseSpec responseMock;

    @Mock
    private Environment env;
    @InjectMocks
    private DevExchangeDAOWebImpl devExchangeDao;

    private DevExchangeRequest dxRequest;

    static private final String RESPONSE_BODY = "{\"key\":\"value\"}";
    static private final String REQUEST_BODY = "{\"foo\":\"bar\"}";
    static private final String REQUEST_BODY_AUDIT = "{\"foo\":\"XXXXX\"}";
    static private final String INVALIDRSAUDIT = "{\"id\":\"200099\",\"text\":\"Invalid response from API: foobar\"}";
    static private final String INVALIDRS = "{\"id\":\"200099\",\"text\":\"Invalid response from API: foobar\"}";
    static private final String SERVICE_UNAVAILABLE = "{\"id\":\"200003\",\"text\":\"System is not available: foobar Exception Cause: \"}";
    static private final String SERVICE_UNAVAILABLE_SOCKET_TIMEOUT = "{\"id\":\"200003\",\"text\":\"System is not available: foobar Exception Cause: socketTimedOut\"}";
    static private final String SERVICE_UNAVAILABLE_CONNECT_EXCEPTION = "{\"id\":\"200003\",\"text\":\"System is not available: foobar Exception Cause: connectionTimedOut\"}";
    static private final String SERVICE_UNAVAILABLE_SSL_EXCEPTION = "{\"id\":\"200003\",\"text\":\"System is not available: foobar Exception Cause: sslException\"}";


    static private final String SERVICE_UNAVAILABLE_AUDIT = "{\"id\":\"200003\",\"text\":\"System is not available: foobar Exception Cause: \"}";

    private Level oldLogLevel = null;

    @Captor
    ArgumentCaptor<URI> uriCaptor;

    @BeforeEach
    public void setup() throws URISyntaxException {
        dxRequest = new DevExchangeRequest();
        dxRequest.setServiceName("foobar");
        dxRequest.setScheme("dx");
        dxRequest.setRequestBody(REQUEST_BODY);
        dxRequest.setRequestBodyAudit(REQUEST_BODY_AUDIT);
        dxRequest.setUri(new URI("https://foobar?test=foo%20bar"));
        dxRequest.setHttpMethod(HttpMethod.GET);

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add(ApplicationConstants.CLIENT_CORRELATION_ID, "test-correlation-id");
        dxRequest.setHttpHeaders(httpHeaders);

        when(devExchangeWebClientFactory.getDevExchangeWebClient(any(), any())).thenReturn(webClient);
        when(webClient.method(any(HttpMethod.class))).thenReturn(requestBodyUriMock);
        when(requestBodyUriMock.uri(any(URI.class))).thenReturn(requestBodyMock);
        when(requestBodyMock.headers(any())).thenReturn(requestBodyMock);
        when(requestBodyMock.bodyValue(any())).thenReturn(requestHeadersMock);
        when(requestHeadersMock.retrieve()).thenReturn(responseMock);
        lenient().when(env.getActiveProfiles()).thenReturn(new String[]{"devint-test"});
        lenient().when(responseMock.toEntity(eq(String.class))).thenReturn(Mono.just(new ResponseEntity<>(RESPONSE_BODY, HttpStatus.OK)));
    }

    @AfterEach
    public void cleanup() {
        System.clearProperty("core.dxtrace");
        if (oldLogLevel != null) {
            setTargetLogLevel(oldLogLevel);
        }
    }

    @Test
    public void testDevExchangeSuccess() {
        DevExchangeResponse dxResponse = devExchangeDao.invokeDxAPI(dxRequest);
        assertEquals(RESPONSE_BODY, dxResponse.getDxResponse(), "We get response body ");
        assertEquals(RESPONSE_BODY, dxResponse.getDxResponseAudit(), "We get audit response body ");
        assertEquals(HttpStatus.OK.value(), dxResponse.getHttpStatus(), "We get ok ");

        verify(webClient, times(1)).method(any(HttpMethod.class));
        verify(requestBodyUriMock, times(1)).uri(uriCaptor.capture());
        verify(requestBodyMock, times(1)).bodyValue(any());

        URI uri = uriCaptor.getValue();
        assertNotNull(uri);
        assertEquals("test=foo%20bar", uri.getRawQuery());
        assertEquals("test=foo bar", uri.getQuery());
    }


    @ParameterizedTest
    @ValueSource(strings = {"", "pretty", "prettyresp"})
    public void testDevExchangePostSuccessLogging() {
        dxRequest.setHttpMethod(HttpMethod.POST);
        oldLogLevel = setTargetLogLevel(Level.INFO);
        System.setProperty("core.dxtrace", "pretty");
        devExchangeDao = new DevExchangeDAOWebImpl(devExchangeWebClientFactory, env);

        DevExchangeResponse dxResponse = devExchangeDao.invokeDxAPI(dxRequest);
        assertEquals(RESPONSE_BODY, dxResponse.getDxResponse(), "We get response body ");
        assertEquals(RESPONSE_BODY, dxResponse.getDxResponseAudit(), "We get audit response body ");
        assertEquals(HttpStatus.OK.value(), dxResponse.getHttpStatus(), "We get ok ");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "pretty", "prettyresp"})
    public void testDevExchangeSuccessLogging(String propertyInfo) {
        oldLogLevel = setTargetLogLevel(Level.INFO);
        System.setProperty("core.dxtrace", propertyInfo);
        devExchangeDao = new DevExchangeDAOWebImpl(devExchangeWebClientFactory, env);

        DevExchangeResponse dxResponse = devExchangeDao.invokeDxAPI(dxRequest);
        assertEquals(RESPONSE_BODY, dxResponse.getDxResponse(), "We get response body ");
        assertEquals(RESPONSE_BODY, dxResponse.getDxResponseAudit(), "We get audit response body ");
        assertEquals(HttpStatus.OK.value(), dxResponse.getHttpStatus(), "We get ok ");
    }

    @Test
    public void testDevExchange4xx() {
        HttpHeaders responseHeader = new HttpHeaders();
        responseHeader.add("Content-Type", "application/json");
        WebClientResponseException webClientResponseException = new WebClientResponseException(HttpStatus.NOT_FOUND.value(), "status", responseHeader, RESPONSE_BODY.getBytes(), Charset.defaultCharset());

        when(requestHeadersMock.retrieve()).thenThrow(webClientResponseException);
        DevExchangeResponse dxResponse = devExchangeDao.invokeDxAPI(dxRequest);
        assertEquals(RESPONSE_BODY, dxResponse.getDxResponse(), "We get response body ");
        assertEquals(RESPONSE_BODY, dxResponse.getDxResponseAudit(), "We get audit response body ");
        assertEquals(HttpStatus.NOT_FOUND.value(), dxResponse.getHttpStatus(), "We get not found ");
    }

    @Test
    public void testGeneratePartnerToken() {
        dxRequest.setScheme("pdx");
        ReflectionTestUtils.setField(devExchangeDao, "tokenGeneratorFactory", tokenGeneratorFactory);
        when(tokenGeneratorFactory.getPartnerTokenGenerator(any())).thenReturn(tokenGenerator);
        devExchangeDao.invokeDxAPI(dxRequest);
        verify(tokenGenerator, times(1)).generateToken(any());

    }

    @Test
    public void testDevExchangeInvalidResponse() {
        WebClientResponseException webClientResponseException = new WebClientResponseException(HttpStatus.NOT_FOUND.value(), "status", null, "foobar".getBytes(), Charset.defaultCharset());

        when(requestHeadersMock.retrieve()).thenThrow(webClientResponseException);

        DevExchangeResponse dxResponse = devExchangeDao.invokeDxAPI(dxRequest);
        assertEquals(INVALIDRS, dxResponse.getDxResponse(), "We get response body ");
        assertEquals(INVALIDRSAUDIT, dxResponse.getDxResponseAudit(), "We get audit response body ");
        assertEquals(HttpStatus.NOT_FOUND.value(), dxResponse.getHttpStatus(), "We get not found ");
    }

    @Test
    public void testDevExchangeClientException() {
        WebClientException webClientException = Mockito.mock(WebClientException.class);
        when(requestHeadersMock.retrieve()).thenThrow(webClientException);

        DevExchangeResponse dxResponse = devExchangeDao.invokeDxAPI(dxRequest);
        assertEquals(SERVICE_UNAVAILABLE, dxResponse.getDxResponse(), "We get response body ");
        assertEquals(SERVICE_UNAVAILABLE_AUDIT, dxResponse.getDxResponseAudit(), "We get audit response body ");
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE.value(), dxResponse.getHttpStatus(), "We get not found ");

        when(webClientException.getCause()).thenReturn(new SocketTimeoutException());
        dxResponse = devExchangeDao.invokeDxAPI(dxRequest);
        assertEquals((SERVICE_UNAVAILABLE_SOCKET_TIMEOUT), dxResponse.getDxResponse());

        when(webClientException.getCause()).thenReturn(new ConnectException());
        dxResponse = devExchangeDao.invokeDxAPI(dxRequest);
        assertEquals((SERVICE_UNAVAILABLE_CONNECT_EXCEPTION), dxResponse.getDxResponse());

        when(webClientException.getCause()).thenReturn(new SSLException("test"));
        dxResponse = devExchangeDao.invokeDxAPI(dxRequest);
        assertEquals((SERVICE_UNAVAILABLE_SSL_EXCEPTION), dxResponse.getDxResponse());

    }

    /**
     * Sets the target log level and returns the old log level so it
     * can be reset at the end of the tests.
     *
     * @param level the new log level to set
     * @return the old log level
     */
    private Level setTargetLogLevel(Level level) {
        LoggerContext context = ((LoggerContext) LogManager.getContext(false));
        LoggerConfig config = context.getConfiguration().getLoggerConfig(DevExchangeDAOWebImpl.class.getName());
        Level oldLevel = config.getLevel();
        config.setLevel(level);
        context.updateLoggers();
        return oldLevel;
    }
}

