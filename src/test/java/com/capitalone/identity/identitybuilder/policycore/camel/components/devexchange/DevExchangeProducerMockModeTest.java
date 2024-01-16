package com.capitalone.identity.identitybuilder.policycore.camel.components.devexchange;

import com.capitalone.chassis.engine.model.context.RequestContextHolder;
import com.capitalone.identity.identitybuilder.policycore.camel.PolicyConstants;
import com.capitalone.identity.identitybuilder.policycore.model.DevExchangeRequest;
import com.capitalone.identity.identitybuilder.policycore.model.DevExchangeResponse;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.model.policy.OperationalAuditExecutionData;
import com.capitalone.identity.identitybuilder.policycore.service.constants.ApplicationConstants;
import com.capitalone.identity.identitybuilder.policycore.service.dao.DevExchangeDAO;
import com.capitalone.identity.identitybuilder.policycore.utils.CamelSpringBootContextAwareTest;
import lombok.SneakyThrows;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Predicate;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockComponent;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.model.ModelCamelContext;
import org.apache.camel.model.dataformat.JsonDataFormat;
import org.apache.camel.spring.SpringCamelContext;
import org.apache.camel.support.DefaultExchange;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.annotation.DirtiesContext;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Collections.singletonMap;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@CamelSpringBootContextAwareTest(
        contextConfigClasses = DevExchangeProducerMockModeTest.ContextConfigCamel.class,
        excludeFromComponentScan = @ComponentScan.Filter(type = FilterType.ASPECTJ,
                pattern = "com.capitalone.identity.identitybuilder.policycore.camel.components.devexchange"))
public class DevExchangeProducerMockModeTest {
    private static final String BASE_URL = "https://www.google.com";
    private static final String REAL_URL = String.format("%s/foo/bar", BASE_URL);
    private static final String MOCK_BASE_URL = "https://www.amazon.com";
    private static final String MOCK_URL = String.format("%s/foo/bar", MOCK_BASE_URL);

    @Autowired
    private ProducerTemplate template;

    @Autowired
    private CamelContext camelContext;

    @BeforeAll
    public static void setComponents() {
        // The component needs a DevExchangeDAO but we'll generate
        // a new mock for each test.
        devExchangeComponent = new DevExchangeComponent();
        devExchangeComponent.setMockBaseUrl(MOCK_BASE_URL);
        devExchangeComponent.setMockFeatureEnabled(true);
        devExchangeComponent.setEnvironmentName("test");
    }

    private static final String CUSTOMER_IP = "customer-ip-addr-value";
    private static final String REQUEST_BODY_STRING = "{\"foo\": \"bar\"}";
    private static final String RESPONSE_BODY = "{\"key\": \"value\"}";

    private static final Predicate TEST_HASH_MAP =
            exchange -> exchange.getIn().getBody().equals(Collections.singletonMap("key", "value"));

    private Map<String, Object> HEADERS;

    @Autowired
    @Qualifier("mockResultEndpoint")
    private MockEndpoint mockEndpoint;

    private static DevExchangeComponent devExchangeComponent;

    @SneakyThrows
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

    }

    @Test
    @DirtiesContext
    void testMockingEnabledHeader() throws InterruptedException {
        // Enable mocking via header
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
        expectedOperationalAuditHeaderData(ex, MOCK_URL, "1", "POST");
    }

    @Test
    @DirtiesContext
    void testMockingDisabledHeader() throws InterruptedException {
        // Disable mocking via header
        HEADERS.put(PolicyConstants.HEADER_MOCKMODE, false);
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
        expectedOperationalAuditHeaderData(ex, REAL_URL, "1", "POST");
    }

    @Configuration
    public static class ContextConfigCamel {

        @SneakyThrows
        @Bean
        protected CamelContext createCamelContext(ApplicationContext applicationContext) {
            CamelContext camelContext = new SpringCamelContext(applicationContext);
            camelContext.adapt(ModelCamelContext.class)
                    .setDataFormats(singletonMap("jackson", new JsonDataFormat()));
            camelContext.addComponent("dx", devExchangeComponent);
            camelContext.addRoutes(new RouteBuilder() {
                @Override
                public void configure() {
                    from("direct:post")
                            .to(String.format("dx:google-service:%s",REAL_URL)).to("mock:result");
                }
            });
            camelContext.build();
            return camelContext;
        }

        @Bean
        protected ProducerTemplate producerTemplate(CamelContext camelContext) throws Exception {
            return camelContext.createProducerTemplate();
        }

        @Bean
        MockComponent mockComponent(CamelContext camelContext) {
            return new MockComponent(camelContext);
        }

        @SneakyThrows
        @Bean("mockResultEndpoint")
        MockEndpoint mockResultEndpoint(MockComponent mockComponent, CamelContext camelContext) {
            MockEndpoint mockResultEndpoint = (MockEndpoint) mockComponent.createEndpoint("mock:result");
            camelContext.addEndpoint("mock:result", mockResultEndpoint);
            return mockResultEndpoint;
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
}
