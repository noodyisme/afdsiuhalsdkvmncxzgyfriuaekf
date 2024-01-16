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
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;
import org.springframework.http.*;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@SuppressWarnings("deprecation")
@ExtendWith(MockitoExtension.class)
public class DevExchangeDAOImplTest {
    @Mock
    private OAuth2RestTemplate oAuth2RestTemplate;

    @Mock
    private DevExchangeRestTemplateFactory devExchangeRestTemplateFactory;

    @Mock
    private Environment env;

    @InjectMocks
    private DevExchangeDAOImpl devExchangeDao;

    private DevExchangeRequest dxRequest;

    static private final String RESPONSE_BODY = "{\"key\":\"value\"}";
    static private final String REQUEST_BODY = "{\"foo\":\"bar\"}";
    static private final String REQUEST_BODY_AUDIT = "{\"foo\":\"XXXXX\"}";
    static private final String INVALIDRS = "{\"id\":\"200099\",\"text\":\"Invalid response from API: foobar\",\"developerText\":\"HttpClientErrorException: 404 status\"}";
    static private final String SERVICE_UNAVAILABLE = "{\"id\":\"200003\",\"text\":\"System is not available: foobar Exception Cause: \",\"developerText\":\"RestClientException: foo occurred\"}";

    private Level oldLogLevel = null;

    @Captor
    ArgumentCaptor<URI> uriCaptor;

    @BeforeEach
    public void setup() throws URISyntaxException {
        dxRequest = new DevExchangeRequest();
        dxRequest.setServiceName("foobar");
        dxRequest.setRequestBody(REQUEST_BODY);
        dxRequest.setRequestBodyAudit(REQUEST_BODY_AUDIT);
        dxRequest.setUri(new URI("https://foobar?test=foo%20bar"));
        dxRequest.setHttpMethod(HttpMethod.GET);

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add(ApplicationConstants.CLIENT_CORRELATION_ID, "test-correlation-id");
        dxRequest.setHttpHeaders(httpHeaders);

        lenient().when(devExchangeRestTemplateFactory.getDevExchangeRestTemplate(any(), any())).thenReturn(oAuth2RestTemplate);
        lenient().when(oAuth2RestTemplate.exchange(any(URI.class), any(HttpMethod.class), any(), eq(String.class))).thenReturn(new ResponseEntity<>(RESPONSE_BODY, HttpStatus.OK));
        lenient().when(env.getActiveProfiles()).thenReturn(new String[]{"devint-test"});
    }

    @AfterEach
    public void cleanup() {
        System.clearProperty("core.dxtrace");
        if (oldLogLevel != null) {
            setTargetLogLevel(oldLogLevel);
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testDevExchangeSuccess() {
        DevExchangeResponse dxResponse = devExchangeDao.invokeDxAPI(dxRequest);
        assertEquals(RESPONSE_BODY, dxResponse.getDxResponse(), "We get response body ");
        assertEquals(RESPONSE_BODY, dxResponse.getDxResponseAudit(), "We get audit response body ");
        assertEquals(HttpStatus.OK.value(), dxResponse.getHttpStatus(), "We get ok ");

        verify(oAuth2RestTemplate).exchange(uriCaptor.capture(), any(HttpMethod.class), any(HttpEntity.class), any(Class.class));
        URI uri = uriCaptor.getValue();
        assertNotNull(uri);
        assertEquals("test=foo%20bar", uri.getRawQuery());
        assertEquals("test=foo bar", uri.getQuery());
    }

    @Test
    public void testDevExchangePostSuccessRaw() {
        dxRequest.setHttpMethod(HttpMethod.POST);
        oldLogLevel = setTargetLogLevel(Level.INFO);

        DevExchangeResponse dxResponse = devExchangeDao.invokeDxAPI(dxRequest);
        assertEquals(RESPONSE_BODY, dxResponse.getDxResponse(), "We get response body ");
        assertEquals(RESPONSE_BODY, dxResponse.getDxResponseAudit(), "We get audit response body ");
        assertEquals(HttpStatus.OK.value(), dxResponse.getHttpStatus(), "We get ok ");
    }

    @Test
    public void testDevExchangePostSuccessPretty() {
        dxRequest.setHttpMethod(HttpMethod.POST);
        oldLogLevel = setTargetLogLevel(Level.INFO);
        System.setProperty("core.dxtrace", "pretty");
        devExchangeDao = new DevExchangeDAOImpl(devExchangeRestTemplateFactory, env);

        DevExchangeResponse dxResponse = devExchangeDao.invokeDxAPI(dxRequest);
        assertEquals(RESPONSE_BODY, dxResponse.getDxResponse(), "We get response body ");
        assertEquals(RESPONSE_BODY, dxResponse.getDxResponseAudit(), "We get audit response body ");
        assertEquals(HttpStatus.OK.value(), dxResponse.getHttpStatus(), "We get ok ");
    }

    @Test
    public void testDevExchangeSuccessRawLogging() {
        oldLogLevel = setTargetLogLevel(Level.INFO);
        System.setProperty("core.dxtrace", "");
        devExchangeDao = new DevExchangeDAOImpl(devExchangeRestTemplateFactory, env);

        DevExchangeResponse dxResponse = devExchangeDao.invokeDxAPI(dxRequest);
        assertEquals(RESPONSE_BODY, dxResponse.getDxResponse(), "We get response body ");
        assertEquals(RESPONSE_BODY, dxResponse.getDxResponseAudit(), "We get audit response body ");
        assertEquals(HttpStatus.OK.value(), dxResponse.getHttpStatus(), "We get ok ");
    }

    @Test
    public void testDevExchangeSuccessPrettyLogging() {
        oldLogLevel = setTargetLogLevel(Level.INFO);
        System.setProperty("core.dxtrace", "pretty");
        devExchangeDao = new DevExchangeDAOImpl(devExchangeRestTemplateFactory, env);

        DevExchangeResponse dxResponse = devExchangeDao.invokeDxAPI(dxRequest);
        assertEquals(RESPONSE_BODY, dxResponse.getDxResponse(), "We get response body ");
        assertEquals(RESPONSE_BODY, dxResponse.getDxResponseAudit(), "We get audit response body ");
        assertEquals(HttpStatus.OK.value(), dxResponse.getHttpStatus(), "We get ok ");
    }

    @Test
    public void testDevExchangeSuccessPrettyRespLogging() {
        oldLogLevel = setTargetLogLevel(Level.INFO);
        System.setProperty("core.dxtrace", "prettyresp");
        devExchangeDao = new DevExchangeDAOImpl(devExchangeRestTemplateFactory, env);

        DevExchangeResponse dxResponse = devExchangeDao.invokeDxAPI(dxRequest);
        assertEquals(RESPONSE_BODY, dxResponse.getDxResponse(), "We get response body ");
        assertEquals(RESPONSE_BODY, dxResponse.getDxResponseAudit(), "We get audit response body ");
        assertEquals(HttpStatus.OK.value(), dxResponse.getHttpStatus(), "We get ok ");
    }

    @Test
    public void testDevExchange4xx() {
        HttpHeaders reponseHeader = new HttpHeaders();
        reponseHeader.add("Content-Type", "application/json");
        HttpClientErrorException httpStatusEx = new HttpClientErrorException(HttpStatus.NOT_FOUND, "status", reponseHeader, RESPONSE_BODY.getBytes(), Charset.defaultCharset());

        when(oAuth2RestTemplate.exchange(any(URI.class), any(HttpMethod.class), any(), eq(String.class)))
                .thenThrow(httpStatusEx);

        DevExchangeResponse dxResponse = devExchangeDao.invokeDxAPI(dxRequest);
        assertEquals(RESPONSE_BODY, dxResponse.getDxResponse(), "We get response body ");
        assertEquals(RESPONSE_BODY, dxResponse.getDxResponseAudit(), "We get audit response body ");
        assertEquals(HttpStatus.NOT_FOUND.value(), dxResponse.getHttpStatus(), "We get not found ");
    }

    @Test
    public void testDevExchangeInvalidResponse() {
        HttpClientErrorException httpStatusEx = new HttpClientErrorException(HttpStatus.NOT_FOUND, "status", null, "foobar".getBytes(), Charset.defaultCharset());

        when(oAuth2RestTemplate.exchange(any(URI.class), any(HttpMethod.class), any(), eq(String.class)))
                .thenThrow(httpStatusEx);

        DevExchangeResponse dxResponse = devExchangeDao.invokeDxAPI(dxRequest);
        assertEquals(INVALIDRS, dxResponse.getDxResponse(), "We get response body ");
        assertEquals(INVALIDRS, dxResponse.getDxResponseAudit(), "We get audit response body ");
        assertEquals(HttpStatus.NOT_FOUND.value(), dxResponse.getHttpStatus(), "We get not found ");
    }

    @Test
    public void testDevExchangeClientException() {
        RestClientException httpStatusEx = new RestClientException("foo occurred");

        when(oAuth2RestTemplate.exchange(any(URI.class), any(HttpMethod.class), any(), eq(String.class)))
                .thenThrow(httpStatusEx);

        DevExchangeResponse dxResponse = devExchangeDao.invokeDxAPI(dxRequest);
        assertEquals(SERVICE_UNAVAILABLE, dxResponse.getDxResponse(), "We get response body ");
        assertEquals(SERVICE_UNAVAILABLE, dxResponse.getDxResponseAudit(), "We get audit response body ");
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE.value(), dxResponse.getHttpStatus(), "We get not found ");
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
        LoggerConfig config = context.getConfiguration().getLoggerConfig(DevExchangeDAOImpl.class.getName());
        Level oldLevel = config.getLevel();
        config.setLevel(level);
        context.updateLoggers();
        return oldLevel;
    }

}
