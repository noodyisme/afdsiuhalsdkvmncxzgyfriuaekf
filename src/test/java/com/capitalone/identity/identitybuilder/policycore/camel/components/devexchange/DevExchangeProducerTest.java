package com.capitalone.identity.identitybuilder.policycore.camel.components.devexchange;

import com.capitalone.chassis.engine.model.context.RequestContext;
import com.capitalone.chassis.engine.model.context.RequestContextHolder;
import com.capitalone.chassis.engine.model.exception.ChassisBusinessException;
import com.capitalone.identity.identitybuilder.policycore.camel.PolicyConstants;
import com.capitalone.identity.identitybuilder.policycore.model.DevExchangeRequest;
import com.capitalone.identity.identitybuilder.policycore.model.DevExchangeResponse;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.model.policy.OperationalAuditExecutionData;
import com.capitalone.identity.identitybuilder.policycore.service.constants.ApplicationConstants;
import com.capitalone.identity.identitybuilder.policycore.service.dao.DevExchangeDAO;
import com.capitalone.identity.identitybuilder.policycore.utils.CamelSpringBootContextAwareTest;
import org.apache.camel.*;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.model.ModelCamelContext;
import org.apache.camel.model.dataformat.JsonDataFormat;
import org.apache.camel.support.DefaultExchange;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.annotation.DirtiesContext;

import java.util.*;

import static org.apache.camel.test.junit5.TestSupport.assertIsInstanceOf;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;
import static org.springframework.context.annotation.ComponentScan.Filter;

@CamelSpringBootContextAwareTest(
        contextConfigClasses = DevExchangeProducerTest.ContextConfigCamel.class,
        excludeFromComponentScan = @Filter(type = FilterType.ASPECTJ,
                pattern = "com.capitalone.identity.identitybuilder.policycore.camel.components.devexchange"))
public class DevExchangeProducerTest {

    @Autowired
    private ProducerTemplate template;

    @Autowired
    private CamelContext camelContext;

    @BeforeAll
    public static void setComponents() {
        // The component needs a DevExchangeDAO but we'll generate
        // a new mock for each test.
        devExchangeComponent = new DevExchangeComponent();
        devExchangeComponent.setEnvironmentName("test");
        devExchangeComponent.setMockBaseUrl("");
    }

	private static final String CUSTOMER_IP = "customer-ip-addr-value";
    private static final String REQUEST_BODY_STRING = "{\"foo\": \"bar\"}";
    private static final Map<String, Object> REQUEST_BODY_MAP = new HashMap<String, Object>() {{
    	put("foo", "bar");
    }};
    private static final String RESPONSE_BODY = "{\"key\": \"value\"}";

    private static final Predicate TEST_HASH_MAP =
    		exchange -> exchange.getIn().getBody().equals(Collections.singletonMap("key", "value"));

    private Map<String, Object> HEADERS;

    @EndpointInject("mock:result")
    private MockEndpoint mockEndpoint;

    private static DevExchangeComponent devExchangeComponent;

    @BeforeEach
    public void setup() {
    	// Use a new DevExchangeDAO mock for each test.
        DevExchangeDAO devExchangeDAO = mock(DevExchangeDAO.class);
        when(devExchangeDAO.invokeDxAPI(any(DevExchangeRequest.class))).thenAnswer(new Answer<DevExchangeResponse>() {
			@Override
			public DevExchangeResponse answer(InvocationOnMock invocation) {
				DevExchangeRequest dxRequest = invocation.getArgument(0);
				DevExchangeResponse dxResponse = new DevExchangeResponse();
		        dxResponse.setDxResponse(RESPONSE_BODY);
		        dxResponse.setHttpStatus(HttpStatus.OK.value());
		        HttpHeaders responseHeaders = new HttpHeaders();
		        responseHeaders.put("test.accept", dxRequest.getHttpHeaders().get("Accept"));
		        responseHeaders.put("test.contentType", dxRequest.getHttpHeaders().get("Content-Type"));
		        responseHeaders.put("test.auditContext", Collections.singletonList(RequestContextHolder.getRequestContextOrDefault().toString()));
		        dxResponse.setHttpHeaders(responseHeaders);
				return dxResponse;
			}
		});
        devExchangeComponent.setDevExchangeDAO(devExchangeDAO);
        camelContext.addComponent("dx", devExchangeComponent);

        // Reset the exchange headers between each test
        HEADERS = new HashMap<String, Object>() {{
            put(PolicyConstants.HEADER_DXHEADERS, new HashMap<String, String>() {{
                put(ApplicationConstants.CUSTOMER_IP_ADDR, CUSTOMER_IP);
                put(ApplicationConstants.CHANNEL_TYPE, "mobileweb");
                put("DXHeader1", "DXHeader1-value");
                put("DXHeader2", "DXHeader2-value");
            }});
            put(PolicyConstants.OPERATIONAL_AUDIT_EXECUTION_DATA, new OperationalAuditExecutionData());
        }};
        camelContext.start();
        mockEndpoint = camelContext.getEndpoint("mock:result", MockEndpoint.class);
    }

    @Test
    @DirtiesContext
    public void testDefaultVersioning() throws InterruptedException {
        mockEndpoint.expectedMessagesMatches(TEST_HASH_MAP);
        mockEndpoint.expectedHeaderReceived(PolicyConstants.HEADER_HTTPSTATUS, HttpStatus.OK.value());
        Exchange ex = new DefaultExchange(camelContext);
        ex.getIn().setBody(REQUEST_BODY_STRING);
        ex.getIn().setHeaders(HEADERS);
        template.send("direct:post", ex);
        mockEndpoint.assertIsSatisfied();
        Map<String, String> expected = new HashMap<>();
        expected.put("test.accept", "application/json;v=1");
        expected.put("test.contentType", "application/json");
        expected.put("test.auditContext", RequestContextHolder.getRequestContextOrDefault().toString());
        assertEquals(expected, ex.getIn().getHeader(PolicyConstants.HEADER_DXRESPONSE_HEADERS));
        expectedOperationalAuditHeaderData(ex, "https://www.google.com/foo/bar", "1", "POST");
    }

    @Test
    @DirtiesContext
    public void testDefaultVersioningForPatchMethod() throws InterruptedException {
        mockEndpoint.expectedMessagesMatches(TEST_HASH_MAP);
        mockEndpoint.expectedHeaderReceived(PolicyConstants.HEADER_HTTPSTATUS, HttpStatus.OK.value());
        Exchange ex = new DefaultExchange(camelContext);
        ex.getIn().setBody(REQUEST_BODY_STRING);
        ex.getIn().setHeaders(HEADERS);
        template.send("direct:patch", ex);
        mockEndpoint.assertIsSatisfied();
        Map<String, String> expected = new HashMap<>();
        expected.put("test.accept", "application/json;v=1");
        expected.put("test.contentType", "application/json");
        expected.put("test.auditContext", RequestContextHolder.getRequestContextOrDefault().toString());
        assertEquals(expected, ex.getIn().getHeader(PolicyConstants.HEADER_DXRESPONSE_HEADERS));
        expectedOperationalAuditHeaderData(ex, "https://www.google.com/foo/bar", "1", "PATCH");
    }

    @Test
    @DirtiesContext
    public void testGetVersion0() throws InterruptedException {
        mockEndpoint.expectedMessagesMatches(TEST_HASH_MAP);
        mockEndpoint.expectedHeaderReceived(PolicyConstants.HEADER_HTTPSTATUS, HttpStatus.OK.value());
        Exchange exchange = new DefaultExchange(camelContext);
        exchange.setPattern(ExchangePattern.InOut);
        exchange.getIn().setHeaders(HEADERS);
        template.send("direct:getVersion0", exchange);

        mockEndpoint.assertIsSatisfied();
        Map<String, String> expected = new HashMap<>();
        expected.put("test.accept", "application/json");
        expected.put("test.contentType", "application/json");
		expected.put("test.auditContext", RequestContextHolder.getRequestContextOrDefault().toString());
        assertEquals(expected, exchange.getIn().getHeader(PolicyConstants.HEADER_DXRESPONSE_HEADERS));
        expectedOperationalAuditHeaderData(exchange, "https://www.google.com/foo/bar", "0", "GET");
    }

    @Test
    @DirtiesContext
    public void testPostVersion2() throws InterruptedException {
        mockEndpoint.expectedMessagesMatches(TEST_HASH_MAP);
        mockEndpoint.expectedHeaderReceived(PolicyConstants.HEADER_HTTPSTATUS, HttpStatus.OK.value());
        Exchange exchange = new DefaultExchange(camelContext);
        exchange.setPattern(ExchangePattern.InOut);
        exchange.getIn().setBody(REQUEST_BODY_STRING);
        exchange.getIn().setHeaders(HEADERS);
        template.send("direct:postVersion2", exchange);

        mockEndpoint.assertIsSatisfied();
        Map<String, String> expected = new HashMap<>();
        expected.put("test.accept", "application/json;v=2");
        expected.put("test.contentType", "application/json");
		expected.put("test.auditContext", RequestContextHolder.getRequestContextOrDefault().toString());
        assertEquals(expected, exchange.getIn().getHeader(PolicyConstants.HEADER_DXRESPONSE_HEADERS));
        expectedOperationalAuditHeaderData(exchange, "https://www.google.com/foo/bar", "2", "POST");
    }

	@Test
    @DirtiesContext
	public void testVersionContentType() throws InterruptedException {
		mockEndpoint.expectedMessagesMatches(TEST_HASH_MAP);
		mockEndpoint.expectedHeaderReceived(PolicyConstants.HEADER_HTTPSTATUS, HttpStatus.OK.value());
		Exchange exchange = new DefaultExchange(camelContext);
		exchange.setPattern(ExchangePattern.InOut);
		exchange.getIn().setBody(REQUEST_BODY_STRING);
		exchange.getIn().setHeaders(HEADERS);
		template.send("direct:postVersionContentType", exchange);

        mockEndpoint.assertIsSatisfied();
		Map<String, String> expected = new HashMap<>();
		expected.put("test.accept", "application/json;v=3");
		expected.put("test.contentType", "application/json;v=3");
		expected.put("test.auditContext", RequestContextHolder.getRequestContextOrDefault().toString());
		assertEquals(expected, exchange.getIn().getHeader(PolicyConstants.HEADER_DXRESPONSE_HEADERS));
        expectedOperationalAuditHeaderData(exchange, "https://www.google.com/foo/bar", "3", "POST");
    }

    @Test
    @DirtiesContext
    public void testNoBody() throws InterruptedException {
        mockEndpoint.expectedMessageCount(0);
        try {
            template.sendBodyAndHeaders("direct:post", null, HEADERS);
            fail("Should have thrown CamelExecutionException!");
        } catch (CamelExecutionException ex) {
            assertIsInstanceOf(ChassisBusinessException.class, ex.getCause());
            assertEquals("No body provided for DevExchange POST or PUT request", ex.getCause().getMessage());
        }

        mockEndpoint.assertIsSatisfied();
    }

    @Test
    @DirtiesContext
    public void testWithStringBody() throws InterruptedException {
        mockEndpoint.expectedMessagesMatches(TEST_HASH_MAP);
        mockEndpoint.expectedHeaderReceived(PolicyConstants.HEADER_HTTPSTATUS, HttpStatus.OK.value());
        template.sendBodyAndHeaders("direct:post", REQUEST_BODY_STRING, HEADERS);
        mockEndpoint.assertIsSatisfied();
        expectedOperationalAuditHeaderData(mockEndpoint, "https://www.google.com/foo/bar", "1", "POST");
    }

    @Test
    @DirtiesContext
    public void testWithMapBody() throws InterruptedException {
        mockEndpoint.expectedMessagesMatches(TEST_HASH_MAP);
        mockEndpoint.expectedHeaderReceived(PolicyConstants.HEADER_HTTPSTATUS, HttpStatus.OK.value());
        template.sendBodyAndHeaders("direct:post", REQUEST_BODY_MAP, HEADERS);
        mockEndpoint.assertIsSatisfied();
        expectedOperationalAuditHeaderData(mockEndpoint, "https://www.google.com/foo/bar", "1", "POST");
    }

    /**
     * The DX component is supposed to override the auditing request context
     * in the thread with whatever is supplied in the reserved header, and then
     * set it back to its previous value.
     *
     * @throws InterruptedException from assertIsSatisfied
     */
    @SuppressWarnings("unchecked")
	@Test
    @DirtiesContext
    public void testWithMapBodyThreadLocal() throws InterruptedException {
    	RequestContext overrideContext = new RequestContext();
    	RequestContext initialContext = RequestContextHolder.getRequestContextOrDefault();
		Map<String, Object> headers = new HashMap<>(HEADERS);
		headers.put(PolicyConstants.HEADER_AUDIT_CONTEXT, overrideContext);

        mockEndpoint.expectedMessagesMatches(TEST_HASH_MAP);
        mockEndpoint.expectedHeaderReceived(PolicyConstants.HEADER_HTTPSTATUS, HttpStatus.OK.value());

        Exchange exchange = new DefaultExchange(camelContext);
        exchange.getIn().setBody(REQUEST_BODY_MAP);
        exchange.getIn().setHeaders(headers);
        template.send("direct:post", exchange);
        mockEndpoint.assertIsSatisfied();

        Map<String, List<String>> respHeaders = (Map<String, List<String>>) exchange.getIn().getHeader(PolicyConstants.HEADER_DXRESPONSE_HEADERS, Map.class);
        assertEquals(overrideContext.toString(), respHeaders.get("test.auditContext"));
        assertEquals(initialContext.toString(), RequestContextHolder.getRequestContextOrDefault().toString());
        expectedOperationalAuditHeaderData(exchange, "https://www.google.com/foo/bar", "1", "POST");

    }

    @Test
    @DirtiesContext
    public void testWithMultipartBody() throws InterruptedException {
        mockEndpoint.expectedMessagesMatches(TEST_HASH_MAP);
        mockEndpoint.expectedHeaderReceived(PolicyConstants.HEADER_HTTPSTATUS, HttpStatus.OK.value());
        template.sendBodyAndHeaders("direct:postMultipart", REQUEST_BODY_MAP, HEADERS);
        mockEndpoint.assertIsSatisfied();
        expectedOperationalAuditHeaderData(mockEndpoint, "https://www.google.com/foo/bar", "1", "POST");
    }

    @Test
    @DirtiesContext
    void testWithMultipartFileEncodedString() throws InterruptedException {
        Map<String, String> customHeadersMap = new HashMap<String, String>() {{
            put("filename", "testimage1");
            put("name", "front");
            put("mediatype", "application/json");
        }};
        List<Map<String, String>> customHeaders = new ArrayList<Map<String, String>>() {{
            add(customHeadersMap);
        }};
        List<Object> multipartEncodedString = new ArrayList<Object>() {{
            add(Base64.getEncoder().encodeToString(new byte[0]));
        }};
        Map<String, Object> headers = new HashMap<>(HEADERS);
        headers.put(DevExchangeProducer.CUSTOM_MULTIPART_HEADER_KEY, customHeaders);
        mockEndpoint.expectedMessagesMatches(TEST_HASH_MAP);
        mockEndpoint.expectedHeaderReceived(PolicyConstants.HEADER_HTTPSTATUS, HttpStatus.OK.value());
        template.sendBodyAndHeaders("direct:postMultipartCustom", multipartEncodedString, headers);
        mockEndpoint.assertIsSatisfied();
        expectedOperationalAuditHeaderData(mockEndpoint, "https://www.google.com/foo/bar", "1", "POST");
    }

    @Test
    @DirtiesContext
    void testWithMultipartFileByteArray() throws InterruptedException {
        Map<String, String> customHeadersMap = new HashMap<String, String>() {{
            put("filename", "testimage1");
            put("name", "front");
            put("mediatype", "image/jpeg");
        }};
        List<Map<String, String>> customHeaders = new ArrayList<Map<String, String>>() {{
            add(customHeadersMap);
        }};
        List<Object> multipartByteArray = new ArrayList<Object>() {{
            add(new byte[0]);
        }};
        Map<String, Object> headers = new HashMap<>(HEADERS);
        headers.put(DevExchangeProducer.CUSTOM_MULTIPART_HEADER_KEY, customHeaders);
        mockEndpoint.expectedMessagesMatches(TEST_HASH_MAP);
        mockEndpoint.expectedHeaderReceived(PolicyConstants.HEADER_HTTPSTATUS, HttpStatus.OK.value());
        template.sendBodyAndHeaders("direct:postMultipartCustom", multipartByteArray, headers);
        mockEndpoint.assertIsSatisfied();
        expectedOperationalAuditHeaderData(mockEndpoint, "https://www.google.com/foo/bar", "1", "POST");
    }

    @Test
    @DirtiesContext
    void testWithMultipartFileIllegalBody() {
        Map<String, String> customHeadersMap = new HashMap<String, String>() {{
            put("filename", "testimage1");
            put("name", "front");
            put("mediatype", null);
        }};
        List<Map<String, String>> customHeaders = new ArrayList<Map<String, String>>() {{
            add(customHeadersMap);
        }};
        Map<String, Object> headers = new HashMap<>(HEADERS);
        headers.put(DevExchangeProducer.CUSTOM_MULTIPART_HEADER_KEY, customHeaders);
        List<Object> multipartByteArray = new ArrayList<Object>() {{
            add(null);
        }};
        Throwable argumentException = assertThrows(CamelExecutionException.class, () -> template.sendBodyAndHeaders("direct:postMultipartCustom", multipartByteArray, headers)).getCause();
        assertTrue(argumentException instanceof IllegalArgumentException);
    }

    @Test
    @DirtiesContext
    void testWithMultipartFileMissingArgumentHeader() {
        mockEndpoint.expectedMessagesMatches(TEST_HASH_MAP);
        mockEndpoint.expectedHeaderReceived(PolicyConstants.HEADER_HTTPSTATUS, HttpStatus.OK.value());
        Map<String, Object> headers = new HashMap<>(HEADERS);

        Throwable argumentException = assertThrows(CamelExecutionException.class, () -> template.sendBodyAndHeaders("direct:postMultipartCustom", REQUEST_BODY_MAP, headers)).getCause();
        assertTrue(argumentException instanceof IllegalArgumentException);
    }

    @DirtiesContext
    @ParameterizedTest
    @NullAndEmptySource
    void testWithMultipartFileInvalidFilenameHeader(String param) {
        Map<String, Object> headers = new HashMap<>(HEADERS);
        // filename
        Map<String, String> customHeadersMap = new HashMap<String, String>() {{
            put("filename", param);
            put("name", "Front");
        }};
        List<Map<String, String>> customHeaders = new ArrayList<Map<String, String>>() {{
            add(customHeadersMap);
        }};
        headers.put(DevExchangeProducer.CUSTOM_MULTIPART_HEADER_KEY, customHeaders);
        Throwable argumentException = assertThrows(CamelExecutionException.class, () -> template.sendBodyAndHeaders("direct:postMultipartCustom", REQUEST_BODY_MAP, headers)).getCause();
        assertTrue(argumentException instanceof IllegalArgumentException);
        assertTrue(argumentException.getMessage().contains("name="));
    }

    @DirtiesContext
    @ParameterizedTest
    @NullAndEmptySource
    void testWithMultipartFileInvalidNameHeader(String param) {
        Map<String, Object> headers = new HashMap<>(HEADERS);
        Map<String, String> customHeadersMap = new HashMap<String, String>() {{
            put("filename", "test.jpeg");
            put("name", param);
        }};
        List<Map<String, String>> customHeaders = new ArrayList<Map<String, String>>() {{
            add(customHeadersMap);
        }};
        headers.put(DevExchangeProducer.CUSTOM_MULTIPART_HEADER_KEY, customHeaders);
        Throwable argumentException = assertThrows(CamelExecutionException.class, () -> template.sendBodyAndHeaders("direct:postMultipartCustom", REQUEST_BODY_MAP, headers)).getCause();
        assertTrue(argumentException instanceof IllegalArgumentException);
        assertTrue(argumentException.getMessage().contains("filename="));
    }


    @Test
    @DirtiesContext
    public void testPut() throws InterruptedException {
        mockEndpoint.expectedMessagesMatches(TEST_HASH_MAP);
        mockEndpoint.expectedHeaderReceived(PolicyConstants.HEADER_HTTPSTATUS, HttpStatus.OK.value());
        template.sendBodyAndHeaders("direct:put", REQUEST_BODY_STRING, HEADERS);
        mockEndpoint.assertIsSatisfied();
        expectedOperationalAuditHeaderData(mockEndpoint, "https://www.google.com/foo/bar", "1", "PUT");
    }

    @Test
    @DirtiesContext
    public void testPatch() throws InterruptedException {
        mockEndpoint.expectedMessagesMatches(TEST_HASH_MAP);
        mockEndpoint.expectedHeaderReceived(PolicyConstants.HEADER_HTTPSTATUS, HttpStatus.OK.value());
        template.sendBodyAndHeaders("direct:patch", REQUEST_BODY_STRING, HEADERS);
        mockEndpoint.assertIsSatisfied();
        expectedOperationalAuditHeaderData(mockEndpoint, "https://www.google.com/foo/bar", "1", "PATCH");
    }

    @Test
    @DirtiesContext
    public void testClientIPDefaultTrue() throws InterruptedException {
        mockEndpoint.expectedMessagesMatches(TEST_HASH_MAP);
        mockEndpoint.expectedHeaderReceived(PolicyConstants.HEADER_HTTPSTATUS, HttpStatus.OK.value());
        template.sendBodyAndHeaders("direct:post", REQUEST_BODY_STRING, HEADERS);
        mockEndpoint.assertIsSatisfied();

        // Make sure the Customer-IP-Address header was copied to Client-IP.
        verify(devExchangeComponent.getDevExchangeDAO()).invokeDxAPI(argThat(req -> CUSTOMER_IP.equals(req.getHttpHeaders().getFirst(DevExchangeProducer.HEADER_CLIENT_IP))));
        expectedOperationalAuditHeaderData(mockEndpoint, "https://www.google.com/foo/bar", "1", "POST");
    }

    @Test
    @DirtiesContext
    public void testClientIPFalse() throws InterruptedException {
        mockEndpoint.expectedMessagesMatches(TEST_HASH_MAP);
        mockEndpoint.expectedHeaderReceived(PolicyConstants.HEADER_HTTPSTATUS, HttpStatus.OK.value());
        template.sendBodyAndHeaders("direct:postNoCopyClientIP", REQUEST_BODY_STRING, HEADERS);
        mockEndpoint.assertIsSatisfied();

        // Make sure the Customer-IP-Address header was NOT copied to Client-IP.
        verify(devExchangeComponent.getDevExchangeDAO()).invokeDxAPI(argThat(req -> !req.getHttpHeaders().containsKey(DevExchangeProducer.HEADER_CLIENT_IP)));
        expectedOperationalAuditHeaderData(mockEndpoint, "https://www.google.com/foo/bar", "1", "POST");
    }

    @Test
    @DirtiesContext
    public void testMobileWebDefaultFalse() throws InterruptedException {
        mockEndpoint.expectedMessagesMatches(TEST_HASH_MAP);
        mockEndpoint.expectedHeaderReceived(PolicyConstants.HEADER_HTTPSTATUS, HttpStatus.OK.value());
        template.sendBodyAndHeaders("direct:post", REQUEST_BODY_STRING, HEADERS);
        mockEndpoint.assertIsSatisfied();

        // Make sure the Channel-Type wasn't modified.
        verify(devExchangeComponent.getDevExchangeDAO()).invokeDxAPI(argThat(req -> "mobileweb".equals(req.getHttpHeaders().getFirst(ApplicationConstants.CHANNEL_TYPE))));
        expectedOperationalAuditHeaderData(mockEndpoint, "https://www.google.com/foo/bar", "1", "POST");
    }

    @Test
    @DirtiesContext
    public void testMobileWebTrue() throws InterruptedException {
        mockEndpoint.expectedMessagesMatches(TEST_HASH_MAP);
        mockEndpoint.expectedHeaderReceived(PolicyConstants.HEADER_HTTPSTATUS, HttpStatus.OK.value());
        template.sendBodyAndHeaders("direct:postNoMobileWeb", REQUEST_BODY_STRING, HEADERS);
        mockEndpoint.assertIsSatisfied();

        // Make sure the Customer-IP-Address header was NOT copied to Client-IP.
        verify(devExchangeComponent.getDevExchangeDAO()).invokeDxAPI(argThat(req -> "mobile".equalsIgnoreCase(req.getHttpHeaders().getFirst(ApplicationConstants.CHANNEL_TYPE))));
        expectedOperationalAuditHeaderData(mockEndpoint, "https://www.google.com/foo/bar", "1", "POST");
    }

    @Test
    @DirtiesContext
    public void testGetWithTemplateUriHeaderNotFound() throws InterruptedException {
        mockEndpoint.expectedMessageCount(0);
        try {
            template.sendBodyAndHeaders("direct:getTemplate", null, HEADERS);
            fail("Should have thrown CamelExecutionException!");
        } catch (CamelExecutionException ex) {
            assertIsInstanceOf(ChassisBusinessException.class, ex.getCause());
            assertEquals("Map has no value for 'templateVar' in URI: https://www.google.com/foo/bar/{templateVar}", ex.getCause().getMessage());
        }

        mockEndpoint.assertIsSatisfied();
    }

    @Test
    @DirtiesContext
    public void testGetWithTemplateUriHeaderFound() throws InterruptedException {
        // Add the value to be replaced in the Headers
        HEADERS.put("templateVar", "templateValue");

        mockEndpoint.expectedMessagesMatches(TEST_HASH_MAP);
        mockEndpoint.expectedHeaderReceived(PolicyConstants.HEADER_HTTPSTATUS, HttpStatus.OK.value());
        template.sendBodyAndHeaders("direct:getTemplate", null, HEADERS);

        mockEndpoint.assertIsSatisfied();
    }

    @Test
    @DirtiesContext
    public void testGetWithExtraQueryParameters() throws InterruptedException {
        // Add the value to be replaced in the Headers
        HEADERS.put("foo", "fooValue");
        HEADERS.put("bar", "bar Value");

        mockEndpoint.expectedMessagesMatches(TEST_HASH_MAP);
        mockEndpoint.expectedHeaderReceived(PolicyConstants.HEADER_HTTPSTATUS, HttpStatus.OK.value());
        template.sendBodyAndHeaders("direct:getWithExtraParams", null, HEADERS);

        mockEndpoint.assertIsSatisfied();

        // Make sure the query parameters were formatted and passed correctly.
        verify(devExchangeComponent.getDevExchangeDAO()).invokeDxAPI(argThat(req -> "bar=bar%20Value&foo=fooValue".equals(req.getUri().getRawQuery())));
        expectedOperationalAuditHeaderData(mockEndpoint, "https://www.google.com/foo/bar?bar=bar%20Value&foo=fooValue", "1", "GET");
    }

   @Test
   @DirtiesContext
    public void testInvalidEndpointMethod() {
        RouteBuilder testRouteBuilder = new RouteBuilder() {
            @Override
            public void configure() {
                from("direct:badDefinition")
                        .to("dx:google-service:https://www.google.com/foo/bar/{templateVar}?method=trace").to("mock:result");
            }
        };
       // Original test was FailedToCreateRouteException.class
       assertThrows(FailedToStartRouteException.class, () -> camelContext.addRoutes(testRouteBuilder));

    }

    @Test
    @DirtiesContext
    public void testInvalidEndpointURIScheme() {
        RouteBuilder testRouteBuilder = new RouteBuilder() {
            @Override
            public void configure() {
                from("direct:badDefinition")
                        .to("dx:google-service:ftp://www.google.com/foo/bar/{templateVar}?method=trace").to("mock:result");
            }
        };
        assertThrows(FailedToCreateRouteException.class,  () -> camelContext.addRoutes(testRouteBuilder));
    }

    @Test
    @DirtiesContext
    public void testInvalidUseAsConsumer() {
        RouteBuilder testRouteBuilder = new RouteBuilder() {
            @Override
            public void configure() {
                from("dx:foo:https://www.google.com/").to("mock:result");
            }
        };
        // Original test was FailedToCreateRouteException.class
        assertThrows(FailedToStartRouteException.class,  () -> camelContext.addRoutes(testRouteBuilder));
    }

    @Test
    @DirtiesContext
    public void testAdditionalHeaders() throws InterruptedException {
    	Map<String, String> customHeaders = new HashMap<String, String>() {{
    		put("DXHeader1", "custom_value1");
    		put("DXHeader2", "custom_value2");
    	}};
    	Map<String, Object> headers = new HashMap<>(HEADERS);
    	headers.put(PolicyConstants.HEADER_CUSTOMHEADERS, customHeaders);

        mockEndpoint.expectedMessageCount(1);
        template.sendBodyAndHeaders("direct:post", REQUEST_BODY_STRING, headers);

        mockEndpoint.assertIsSatisfied();
        verify(devExchangeComponent.getDevExchangeDAO(), atLeastOnce())
                .invokeDxAPI(any(DevExchangeRequest.class));
        expectedOperationalAuditHeaderData(mockEndpoint, "https://www.google.com/foo/bar", "1", "POST");
    }

    @Test
    @DirtiesContext
    public void testWithInOutExchange() throws InterruptedException {
        mockEndpoint.expectedMessagesMatches(TEST_HASH_MAP);
        mockEndpoint.expectedHeaderReceived(PolicyConstants.HEADER_HTTPSTATUS, HttpStatus.OK.value());
        Exchange exchange = new DefaultExchange(camelContext);
        exchange.setPattern(ExchangePattern.InOut);
        exchange.getIn().setBody(REQUEST_BODY_STRING);
        exchange.getIn().setHeaders(HEADERS);
        template.send("direct:post", exchange);
        mockEndpoint.assertIsSatisfied();
        assertNotNull(exchange.getMessage());
        expectedOperationalAuditHeaderData(mockEndpoint, "https://www.google.com/foo/bar", "1", "POST");
    }

    @Test
    @DirtiesContext
    public void testEndpointGetParameters() {
    	DevExchangeEndpoint x = camelContext.getEndpoint("dx:google-service:https://www.google.com/foo/bar?copyClientIP=false", DevExchangeEndpoint.class);
    	assertTrue(x.getParameters().isEmpty());
    }

    @Test
    @DirtiesContext
    void testMockingDisabled() throws InterruptedException {
        // provide header to mock downstream
        HEADERS.put(PolicyConstants.HEADER_MOCKMODE, true);
        mockEndpoint.expectedMessagesMatches(TEST_HASH_MAP);
        mockEndpoint.expectedHeaderReceived(PolicyConstants.HEADER_HTTPSTATUS, HttpStatus.OK.value());
        Exchange ex = new DefaultExchange(camelContext);
        ex.getIn().setBody(REQUEST_BODY_STRING);
        ex.getIn().setHeaders(HEADERS);
        template.send("direct:post", ex);
        mockEndpoint.assertIsSatisfied();
        Map<String, String> expected = new HashMap<>();
        expected.put("test.accept", "application/json;v=1");
        expected.put("test.contentType", "application/json");
        expected.put("test.auditContext", RequestContextHolder.getRequestContextOrDefault().toString());
        assertEquals(expected, ex.getIn().getHeader(PolicyConstants.HEADER_DXRESPONSE_HEADERS));
        // mock header is ignored because mocking is disabled for this env (default)
        expectedOperationalAuditHeaderData(ex, "https://www.google.com/foo/bar", "1", "POST");
    }

    @Configuration
    public static class ContextConfigCamel {
        @Bean
        protected CamelContext createCamelContext() throws Exception {
            CamelContext camelContext = new DefaultCamelContext();
            camelContext.adapt(ModelCamelContext.class)
                    .setDataFormats(Collections.singletonMap("jackson", new JsonDataFormat()));
            camelContext.addRoutes(route());
            camelContext.build();
            return camelContext;
        }

        @Bean
        protected ProducerTemplate producerTemplate(CamelContext context) throws Exception {
            return context.createProducerTemplate();
        }

        public RouteBuilder route() {
            return new RouteBuilder() {
                @Override
                public void configure() {
                    from("direct:getVersion0")
                            .to("dx:google-service:https://www.google.com/foo/bar?dxVersion=0&method=get").to("mock:result");
                    from("direct:postVersion2")
                            .to("dx:google-service:https://www.google.com/foo/bar?dxVersion=2").to("mock:result");
                    from("direct:post")
                            .to("dx:google-service:https://www.google.com/foo/bar").to("mock:result");
                    from("direct:postMultipart")
                            .to("dx:google-service:https://www.google.com/foo/bar?multipartId=partId").to("mock:result");
                    from("direct:postMultipartCustom")
                            .to("dx:google-service:https://www.google.com/foo/bar?customMultipart=true").to("mock:result");
                    from("direct:postNoCopyClientIP")
                            .to("dx:google-service:https://www.google.com/foo/bar?copyClientIP=false").to("mock:result");
                    from("direct:postNoMobileWeb")
                            .to("dx:google-service:https://www.google.com/foo/bar?mobileWebToMobile=true").to("mock:result");
                    from("direct:getTemplate")
                            .to("dx:google-service:https://www.google.com/foo/bar/{templateVar}?method=get").to("mock:result");
                    from("direct:getWithExtraParams")
                            .to("dx:google-service:https://www.google.com/foo/bar?method=get&foo={foo}&bar={bar}").to("mock:result");
                    from("direct:put")
                            .to("dx:google-service:https://www.google.com/foo/bar?method=put").to("mock:result");
                    from("direct:patch")
                            .to("dx:google-service:https://www.google.com/foo/bar?method=patch").to("mock:result");
                    from("direct:postVersionContentType")
                            .to("dx:google-service:https://www.google.com/foo/bar?dxVersion=3&versionContentType=true").to("mock:result");
                }
            };
        }
    }

    private static void expectedOperationalAuditHeaderData(final Exchange exchange, final String targetUri,
            final String dxVersion, final String httpMethod) {
        final OperationalAuditExecutionData operationalAuditExecutionData = (OperationalAuditExecutionData) exchange.getIn()
                .getHeader(PolicyConstants.OPERATIONAL_AUDIT_EXECUTION_DATA);
        final List<String> pipsEvaluated = operationalAuditExecutionData.getPipsEvaluated();
        assertEquals(1, pipsEvaluated.size());
        assertEquals(String.format("{ scheme: dx, service_name: google-service, target_uri: %s, dx_version: %s, http_method: %s }",
                targetUri, dxVersion, httpMethod), pipsEvaluated.get(0));
    }

    private static void expectedOperationalAuditHeaderData(final MockEndpoint mockEndpoint, final String targetUri,
            final String dxVersion, final String httpMethod) {
        assertEquals(1, mockEndpoint.getExchanges().size()); // sanity check
        expectedOperationalAuditHeaderData(mockEndpoint.getExchanges().get(0), targetUri, dxVersion, httpMethod);
    }
}
