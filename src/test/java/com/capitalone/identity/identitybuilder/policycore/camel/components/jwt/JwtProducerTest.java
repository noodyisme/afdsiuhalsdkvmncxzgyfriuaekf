package com.capitalone.identity.identitybuilder.policycore.camel.components.jwt;

import com.capitalone.identity.identitybuilder.policycore.operational_audit.mapper.JwkValidatedEventMapper;
import com.capitalone.identity.identitybuilder.policycore.service.jws.JWTExtractionException;
import com.capitalone.identity.identitybuilder.policycore.service.jws.JwsService;
import com.capitalone.identity.identitybuilder.policycore.service.jws.JwsServiceValidationResponse;
import com.capitalone.identity.identitybuilder.policycore.service.jws.JwtPublicKeyRetrievalService;
import com.capitalone.identity.identitybuilder.policycore.utils.CamelSpringBootContextAwareTest;
import lombok.SneakyThrows;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.AdviceWith;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.support.DefaultExchange;
import org.apache.camel.test.spring.junit5.UseAdviceWith;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.core.env.Environment;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.Serializable;
import java.util.*;

import static com.capitalone.identity.identitybuilder.policycore.camel.PolicyConstants.*;
import static com.capitalone.identity.identitybuilder.policycore.utils.TestUtils.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@CamelSpringBootContextAwareTest(
        contextConfigClasses = JwtProducerTest.ContextConfig.class,
        excludeFromComponentScan = @ComponentScan.Filter(type = FilterType.ASPECTJ,
                pattern = "com.capitalone.identity.identitybuilder.policycore.camel.components.crypto-jwt.*"))
@UseAdviceWith
class JwtProducerTest {

    public static final String JWT_GENERATE_ROUTE = "crypto-jwt:generate?srcTokenHeader=customerIDToken";
    public static final String JWT_VALIDATE_JWT_TOKEN_FROM_SRC_TOKEN_HEADER_BAD_TOKEN = "crypto-jwt:validate?srcTokenHeader=UNKNOWN_TYPE";
    public static final String JWT_VALIDATE_JWT_TOKEN_FROM_SRC_TOKEN_HEADER = "crypto-jwt:validate?srcTokenHeader=customerIDToken";
    public static final String JWT_EXTRACT_SOME_FIELDS_FROM_SRC_TOKEN_HEADER = "crypto-jwt:extract?srcTokenHeader=customerIDToken&fieldsToExtract=iss,customerAppDataRefKey";
    public static final String JWT_EXTRACT_ALL_FIELDS_FROM_SRC_TOKEN_HEADER = "crypto-jwt:extract?srcTokenHeader=customerIDToken&fieldsToExtract=";
    public static final String JWT_EXTRACT_ALL_FIELDS_FROM_SRC_TOKEN_HEADER_WITH_SRC_TOKEN_PATH = "crypto-jwt:extract?srcTokenHeader=customerIDToken&srcTokenPath=$.level.token&fieldsToExtract=";
    public static final String JWT_EXTRACT_ALL_FIELDS_FROM_SRC_TOKEN_PATH= "crypto-jwt:extract?srcTokenPath=$.level.token&fieldsToExtract=";
    public static final String JWT_VALIDATE_JWT_TOKEN_PATH = "crypto-jwt:validate?srcTokenPath=$.customerIdToken";
    public static final String JWT_VALIDATE_JWT_TOKEN_PATH_BAD = "crypto-jwt:validate?srcTokenPath=$.abc";
    public static final String JWT_EXTRACT_ALL_NO_SRC = "crypto-jwt:extract?fieldsToExtract=";
    public static final String JWT_TOKEN_DATA = "JWT_TOKEN_DATA";
    public static final String TRUE = "true";
    @Autowired
    private CamelContext camelContext;

    @Autowired
    private ProducerTemplate producerTemplate;

    @Mock
    Environment environment;

    @Test
    @DirtiesContext
    public void testRouteWithJwtTokenOfUnknownTypeShouldThrowException() throws Exception {
        AdviceWith.adviceWith(camelContext, TEST_ROUTE,
                a -> a.weaveByToUri(DIRECT_END)
                        .replace().to(JWT_VALIDATE_JWT_TOKEN_FROM_SRC_TOKEN_HEADER_BAD_TOKEN)
        );

        Exchange exchange = new DefaultExchange(camelContext);

        camelContext.start();

        Exchange processedExchange = producerTemplate.send(DIRECT_START, exchange);
        JWTExtractionException jwtException = processedExchange.getProperty(Exchange.EXCEPTION_CAUGHT, JWTExtractionException.class);
        assertNotNull(jwtException);
        assertEquals("501001~JWT Unauthorized Error~Missing or absent token, unable to validate", jwtException.getMessage());
        assertNull(processedExchange.getMessage().getHeader(JWT_TOKEN_VALIDATION_RESULT, String.class));

        JwtEndpoint jwtEndpoint = camelContext.getEndpoint(JWT_VALIDATE_JWT_TOKEN_FROM_SRC_TOKEN_HEADER, JwtEndpoint.class);
        JwsService jwsService = jwtEndpoint.getJwsService();
        verify(jwsService).validate(null,null,true);
    }

    @Test
    @DirtiesContext
    public void testRouteWithJwtTokenNotAvailableAtExchangeShouldReturnInvalidValidationResult() throws Exception {
        AdviceWith.adviceWith(camelContext, TEST_ROUTE,
                a -> a.weaveByToUri(DIRECT_END)
                        .replace().to(JWT_VALIDATE_JWT_TOKEN_FROM_SRC_TOKEN_HEADER)
        );

        camelContext.start();

        Exchange exchange = new DefaultExchange(camelContext);

        Exchange processedExchange = producerTemplate.send(DIRECT_START, exchange);
        JWTExtractionException jwtException = processedExchange.getProperty(Exchange.EXCEPTION_CAUGHT, JWTExtractionException.class);
        assertNotNull(jwtException);
        assertEquals("501001~JWT Unauthorized Error~Missing or absent token, unable to validate", jwtException.getMessage());
        assertNull(processedExchange.getMessage().getHeader(JWT_TOKEN_VALIDATION_RESULT, String.class));

        JwtEndpoint jwtEndpoint = camelContext.getEndpoint(JWT_VALIDATE_JWT_TOKEN_FROM_SRC_TOKEN_HEADER, JwtEndpoint.class);
        JwsService jwsService = jwtEndpoint.getJwsService();
        verify(jwsService).validate(null,null,true);    }

    @Test
    @DirtiesContext
    public void testRouteWithJwtTokenPassedIntoExecutionContextShouldInvokeService() throws Exception {
        AdviceWith.adviceWith(camelContext, TEST_ROUTE,
                a -> a.weaveByToUri(DIRECT_END)
                        .replace().to(JWT_VALIDATE_JWT_TOKEN_FROM_SRC_TOKEN_HEADER)
        );

        camelContext.start();

        Exchange exchange = new DefaultExchange(camelContext);
        exchange.getIn().setHeader(CUSTOMER_ID_TOKEN, JWT_TOKEN_DATA);
        ProducerTemplate producerTemplate  = camelContext.createProducerTemplate();
        Exchange processedExchange = producerTemplate.send(DIRECT_START, exchange);

        JwtEndpoint jwtEndpoint = camelContext.getEndpoint(JWT_VALIDATE_JWT_TOKEN_FROM_SRC_TOKEN_HEADER, JwtEndpoint.class);

        assertEquals(CUSTOMER_ID_TOKEN, jwtEndpoint.getSrcTokenHeader());
        assertEquals(JwtOperation.valueOf(JwtOperation.VALIDATE.name()), jwtEndpoint.getJwtOperation());
        assertEquals(TRUE, processedExchange.getMessage().getHeader(JWT_TOKEN_VALIDATION_RESULT, String.class));

        JwsService jwsService = jwtEndpoint.getJwsService();
        verify(jwsService, times(1)).validate(anyString(),any(),anyBoolean());
        verify(jwsService).validate(eq(JWT_TOKEN_DATA),any(),anyBoolean());
        verifyNoMoreInteractions(jwsService);
    }

    @Test
    @DirtiesContext
    public void testRouteWithInvalidJwtTokenPassedIntoExecutionContextShouldInvokeServiceToExtractSpecificFieldsAndFailToExtract() throws Exception {
        AdviceWith.adviceWith(camelContext, TEST_ROUTE,
                a -> a.weaveByToUri(DIRECT_END)
                        .replace().to(JWT_EXTRACT_SOME_FIELDS_FROM_SRC_TOKEN_HEADER)
        );

        camelContext.start();

        Exchange exchange = new DefaultExchange(camelContext);
        exchange.getIn().setHeader(CUSTOMER_ID_TOKEN, JWT_TOKEN_DATA);
        ProducerTemplate producerTemplate  = camelContext.createProducerTemplate();
        Exchange responseExchange = producerTemplate.send(DIRECT_START, exchange);
        Exception exception = responseExchange.getException();
        assertNotNull(exception);
        assertEquals("501002~JWT Unauthorized Error~JWT malformed, not in expected format", exception.getMessage());

        JwtEndpoint jwtEndpoint = camelContext.getEndpoint(JWT_EXTRACT_SOME_FIELDS_FROM_SRC_TOKEN_HEADER, JwtEndpoint.class);

        assertEquals(CUSTOMER_ID_TOKEN, jwtEndpoint.getSrcTokenHeader());
        assertEquals("iss,customerAppDataRefKey", jwtEndpoint.getFieldsToExtract());
        assertEquals(JwtOperation.valueOf(JwtOperation.EXTRACT.name()), jwtEndpoint.getJwtOperation());

        JwsService jwsService = jwtEndpoint.getJwsService();
        verify(jwsService, times(1)).validate(eq(JWT_TOKEN_DATA),any(),anyBoolean());
        verify(jwsService, times(1)).extractFields(anyString(), any(List.class));
        verify(jwsService, times(1)).extractFields(eq(JWT_TOKEN_DATA), eq(Arrays.asList("iss","customerAppDataRefKey")));

        assertEquals(TRUE, responseExchange.getIn().getHeader(JWT_TOKEN_VALIDATION_RESULT, String.class));
        assertNull(responseExchange.getIn().getHeader(JWT_EXTRACTED_FIELDS, String.class));
    }


    String testToken = "eyJraWQiOiJ2ZXJpZmllZC1zc28tdG9rZW4xIiwiYWxnIjoiUlMyNTYifQ.eyJzdWIiOiJyZDVHRGVENlwvTW45Sk9CWXVrdGdrTHpYUDNMdzVZejFRbFZublRtSjM5VTdQd1JNaHArTWw5T0dROXJjT2xFSkdEQ0RpT1FWaXJNamlSallZTEREZmc9PSIsImlzcyI6InZlcmlmaWVkLmNsb3VkZHF0LmNhcGl0YWxvbmUuY29tIiwicHJvZmlsZVJlZmVyZW5jZUlkM1BLZXkiOiJyZDVHRGVENlwvTW45Sk9CWXVrdGdrTHpYUDNMdzVZejFRbFZublRtSjM5VTdQd1JNaHArTWw5T0dROXJjT2xFSkdEQ0RpT1FWaXJNamlSallZTEREZmc9PSIsImN1c3RvbWVyQXBwRGF0YVJlZktleSI6ImM0MjRjMjNiLWRhNWQtNDUwYi1iMGEyLTk3N2IwNDMxYTNjMiIsImV4cCI6MTY1ODQ5ODU0MiwiaWF0IjoxNjU4NDkxMzQyfQ.V9LpryBK0k8wqixH7-2pLKdMK95ooFFyOu5pjuwkduOveemCDKGcUw0jvX_fmnk1Xq5n4z171Fqq4GaljXQJ_kcCGE2oBYaimSAgFab4Yyyya235xvw29mwgWPmUwDA8J1aS52l-Ms_sf44n_ySeu5P4ijtflI0DrfOyTufv3s0HhsVZ6f4gcaNjIYyRqErX2QC52JP2qIEru3FFZphW2udcMSU7x6Qdcpx_JZkVEajQ9SYQIRFEBN2VogeTyD924fDpPEqTWdDQUjRmo0PC_dcAugKdSPz-6t6Ial5Qc_QRgjk80O8_CunfCdkvLcepJvScuioibxxiK6jx3GquGw";
    String testTokenJsonEmbedded = String.format("{ \"level\" : { \"token\" : \"%s\"} }", testToken);
    @Test
    @DirtiesContext
    public void testRouteWithValidJwtTokenPassedIntoExecutionContextShouldInvokeServiceToExtractSpecificFieldsAndFailToExtract() throws Exception {
        AdviceWith.adviceWith(camelContext, TEST_ROUTE,
                a -> a.weaveByToUri(DIRECT_END)
                        .replace().to(JWT_EXTRACT_SOME_FIELDS_FROM_SRC_TOKEN_HEADER)
        );

        camelContext.start();

        Exchange exchange = new DefaultExchange(camelContext);
        exchange.getIn().setHeader(CUSTOMER_ID_TOKEN, testToken);
        ProducerTemplate producerTemplate  = camelContext.createProducerTemplate();
        Exchange responseExchange = producerTemplate.send(DIRECT_START, exchange);


        JwtEndpoint jwtEndpoint = camelContext.getEndpoint(JWT_EXTRACT_SOME_FIELDS_FROM_SRC_TOKEN_HEADER, JwtEndpoint.class);

        assertEquals(CUSTOMER_ID_TOKEN, jwtEndpoint.getSrcTokenHeader());
        assertEquals("iss,customerAppDataRefKey", jwtEndpoint.getFieldsToExtract());
        assertEquals(JwtOperation.valueOf(JwtOperation.EXTRACT.name()), jwtEndpoint.getJwtOperation());

        JwsService jwsService = jwtEndpoint.getJwsService();
        verify(jwsService, times(1)).validate(eq(testToken),any(),anyBoolean());
        verify(jwsService, times(1)).extractFields(anyString(), any(List.class));
        verify(jwsService, times(1)).extractFields(eq(testToken), eq(Arrays.asList("iss","customerAppDataRefKey")));

        assertEquals(TRUE, responseExchange.getIn().getHeader(JWT_TOKEN_VALIDATION_RESULT, String.class));
        assertEquals("{iss=verified.clouddqt.capitalone.com, customerAppDataRefKey=c424c23b-da5d-450b-b0a2-977b0431a3c2}", responseExchange.getIn().getHeader(JWT_EXTRACTED_FIELDS, String.class));
    }

    @Captor
    ArgumentCaptor<List> extractFieldsArgumentCaptor;

    @Test
    @DirtiesContext
    public void testRouteWithSrcTokenHeaderShouldInvokeServiceToExtractAllFields() throws Exception {
        AdviceWith.adviceWith(camelContext, TEST_ROUTE,
                a -> a.weaveByToUri(DIRECT_END)
                        .replace().to(JWT_EXTRACT_ALL_FIELDS_FROM_SRC_TOKEN_HEADER)
        );

        JwtEndpoint jwtEndpoint = camelContext.getEndpoint(JWT_EXTRACT_ALL_FIELDS_FROM_SRC_TOKEN_HEADER, JwtEndpoint.class);
        JwsService jwsService = jwtEndpoint.getJwsService();
        JwsService jwsServiceSpy = spy(jwsService);
        doReturn(new JwsServiceValidationResponse()).when(jwsServiceSpy).validate(any(),any(),anyBoolean());
        camelContext.start();

        Exchange exchange = new DefaultExchange(camelContext);
        exchange.getIn().setHeader(CUSTOMER_ID_TOKEN, testToken);
        ProducerTemplate producerTemplate  = camelContext.createProducerTemplate();
        producerTemplate.send(DIRECT_START, exchange);


        assertEquals(CUSTOMER_ID_TOKEN, jwtEndpoint.getSrcTokenHeader());
        assertEquals("", jwtEndpoint.getFieldsToExtract());
        assertEquals(JwtOperation.valueOf(JwtOperation.EXTRACT.name()), jwtEndpoint.getJwtOperation());

        verify(jwsService, times(1)).extractFields(anyString(), any(List.class));
        verify(jwsService, times(1)).extractFields(eq(testToken), extractFieldsArgumentCaptor.capture());
        assertEquals(0, extractFieldsArgumentCaptor.getValue().size());
        //verify(jwsService).validate(eq(testToken));
        verify(jwsService).validate(eq(testToken),any(),anyBoolean());
    }

    @Test
    @DirtiesContext
    public void testRouteWithSrcTokenHeaderAndSrcTokenPathShouldInvokeServiceToExtractAllFields() throws Exception {
        AdviceWith.adviceWith(camelContext, TEST_ROUTE,
                a -> a.weaveByToUri(DIRECT_END)
                        .replace().to(JWT_EXTRACT_ALL_FIELDS_FROM_SRC_TOKEN_HEADER_WITH_SRC_TOKEN_PATH)
        );

        JwtEndpoint jwtEndpoint = camelContext.getEndpoint(JWT_EXTRACT_ALL_FIELDS_FROM_SRC_TOKEN_HEADER_WITH_SRC_TOKEN_PATH, JwtEndpoint.class);
        JwsService jwsService = jwtEndpoint.getJwsService();
        JwsService jwsServiceSpy = spy(jwsService);
        doReturn(new JwsServiceValidationResponse()).when(jwsServiceSpy).validate(any(),any(),anyBoolean());
        camelContext.start();

        Exchange exchange = new DefaultExchange(camelContext);
        exchange.getIn().setHeader(CUSTOMER_ID_TOKEN, testTokenJsonEmbedded);
        ProducerTemplate producerTemplate  = camelContext.createProducerTemplate();
        producerTemplate.send(DIRECT_START, exchange);


        assertEquals(CUSTOMER_ID_TOKEN, jwtEndpoint.getSrcTokenHeader());
        assertEquals("", jwtEndpoint.getFieldsToExtract());
        assertEquals(JwtOperation.valueOf(JwtOperation.EXTRACT.name()), jwtEndpoint.getJwtOperation());

        verify(jwsService, times(1)).extractFields(anyString(), any(List.class));
        verify(jwsService, times(1)).extractFields(eq(testToken), extractFieldsArgumentCaptor.capture());
        assertEquals(0, extractFieldsArgumentCaptor.getValue().size());
        verify(jwsService).validate(eq(testToken),any(),anyBoolean());
    }

    @Test
    @DirtiesContext
    public void testRouteWithSrcTokenPathBodyAsAMapShouldInvokeServiceToExtractAllFields() throws Exception {
        AdviceWith.adviceWith(camelContext, TEST_ROUTE,
                a -> a.weaveByToUri(DIRECT_END)
                        .replace().to(JWT_EXTRACT_ALL_FIELDS_FROM_SRC_TOKEN_PATH)
        );

        JwtEndpoint jwtEndpoint = camelContext.getEndpoint(JWT_EXTRACT_ALL_FIELDS_FROM_SRC_TOKEN_PATH, JwtEndpoint.class);
        JwsService jwsService = jwtEndpoint.getJwsService();
        JwsService jwsServiceSpy = spy(jwsService);
        doReturn(new JwsServiceValidationResponse()).when(jwsServiceSpy).validate(any(),any(),anyBoolean());
        camelContext.start();

        Exchange exchange = new DefaultExchange(camelContext);
        Map<String, Serializable> body = new HashMap<>();
        body.put("level", new HashMap<>(Collections.singletonMap("token", testToken)));
        exchange.getIn().setBody(body);
        ProducerTemplate producerTemplate  = camelContext.createProducerTemplate();
        Exchange responseExchange = producerTemplate.send(DIRECT_START, exchange);
        assertNull(responseExchange.getException());

        assertEquals("$.level.token", jwtEndpoint.getSrcTokenPath());
        assertEquals("", jwtEndpoint.getFieldsToExtract());
        assertEquals(JwtOperation.valueOf(JwtOperation.EXTRACT.name()), jwtEndpoint.getJwtOperation());

        verify(jwsService, times(1)).extractFields(anyString(), any(List.class));
        verify(jwsService, times(1)).extractFields(eq(testToken), extractFieldsArgumentCaptor.capture());
        assertEquals(0, extractFieldsArgumentCaptor.getValue().size());
        //verify(jwsService).validate(eq(testToken));
        verify(jwsService).validate(eq(testToken),any(),anyBoolean());
    }

    @Test
    @DirtiesContext
    public void testRouteWithoutSrcTokenHeaderAndSrcTokenPathShouldInvokeServiceToExtractAllFields() throws Exception {
        AdviceWith.adviceWith(camelContext, TEST_ROUTE,
                a -> a.weaveByToUri(DIRECT_END)
                        .replace().to(JWT_EXTRACT_ALL_NO_SRC)
        );

        JwtEndpoint jwtEndpoint = camelContext.getEndpoint(JWT_EXTRACT_ALL_NO_SRC, JwtEndpoint.class);
        JwsService jwsService = jwtEndpoint.getJwsService();
        JwsService jwsServiceSpy = spy(jwsService);
        doReturn(new JwsServiceValidationResponse()).when(jwsServiceSpy).validate(any(),any(),anyBoolean());
        camelContext.start();

        Exchange exchange = new DefaultExchange(camelContext);
        exchange.getIn().setBody(testToken, String.class);
        ProducerTemplate producerTemplate  = camelContext.createProducerTemplate();
        Exchange responseExchange = producerTemplate.send(DIRECT_START, exchange);
        assertNull(responseExchange.getException());

        assertNull(jwtEndpoint.getSrcTokenPath());
        assertNull(jwtEndpoint.getSrcTokenHeader());
        assertEquals("", jwtEndpoint.getFieldsToExtract());
        assertEquals(JwtOperation.valueOf(JwtOperation.EXTRACT.name()), jwtEndpoint.getJwtOperation());

        verify(jwsService, times(1)).extractFields(anyString(), any(List.class));
        verify(jwsService, times(1)).extractFields(eq(testToken), extractFieldsArgumentCaptor.capture());
        assertEquals(0, extractFieldsArgumentCaptor.getValue().size());
        //verify(jwsService).validate(eq(testToken));
        verify(jwsService).validate(eq(testToken),any(),anyBoolean());
    }

    @Test
    @DirtiesContext
    public void testRouteWithUnresolvableSrcTokenPathShouldResultInMalformedTokenError() throws Exception {
        AdviceWith.adviceWith(camelContext, TEST_ROUTE,
                a -> a.weaveByToUri(DIRECT_END)
                        .replace().to(JWT_VALIDATE_JWT_TOKEN_PATH_BAD)
        );

        LinkedHashMap body = new LinkedHashMap();
        body.put("customer", "token");
        Exchange exchange = new DefaultExchange(camelContext);
        exchange.getIn().setBody(body, LinkedHashMap.class);

        camelContext.start();
        Exchange processedExchange = producerTemplate.send(DIRECT_START, exchange);

        JWTExtractionException jwtException = processedExchange.getProperty(Exchange.EXCEPTION_CAUGHT, JWTExtractionException.class);

        assertNotNull(jwtException);
        assertEquals("501002~JWT Unauthorized Error~JWT malformed, not in expected format", jwtException.getMessage());
        assertNull(processedExchange.getMessage().getHeader(JWT_TOKEN_VALIDATION_RESULT, String.class));
    }


    @Captor
    ArgumentCaptor<String> extractedToken;

    @Test
    @DirtiesContext
    public void testRouteWithResolvableSrcTokenPathShouldInvokeServiceWithCorrectExtract() throws Exception {
        AdviceWith.adviceWith(camelContext, TEST_ROUTE,
                a -> a.weaveByToUri(DIRECT_END)
                        .replace().to(JWT_VALIDATE_JWT_TOKEN_PATH)
        );

        Map<String, Serializable> body = new HashMap<>();
        body.put("customerIdToken", testToken);
        Exchange exchange = new DefaultExchange(camelContext);
        exchange.getIn().setBody(body);

        JwtEndpoint jwtEndpoint = camelContext.getEndpoint(JWT_VALIDATE_JWT_TOKEN_PATH, JwtEndpoint.class);
        JwsService jwsService = spy(new JwsService(new JwtPublicKeyRetrievalService(WebClient.create()), new JwkValidatedEventMapper.JwkValidationPublisher(null)));
        doReturn(new JwsServiceValidationResponse()).when(jwsService).validate(anyString(),any(),anyBoolean());
        ReflectionTestUtils.setField(jwtEndpoint, "jwsService", jwsService);

        camelContext.start();
        Exchange response = producerTemplate.send(DIRECT_START, exchange);
        assertNull(response.getException());

        verify(jwsService, times(1)).validate(anyString(),any(),anyBoolean());
        verify(jwsService).validate(extractedToken.capture(),any(),anyBoolean());
        verifyNoMoreInteractions(jwsService);

        assertEquals(testToken, extractedToken.getValue());
    }


    @Configuration
    public static class ContextConfig {

        @SneakyThrows
        @Bean
        public JwtComponent jwtComponent() {
            JwsService jwsService = new JwsService(null, null);
            JwsService spy = spy(jwsService);
            doReturn(new JwsServiceValidationResponse()).when(spy).validate(anyString(),any(),anyBoolean());
            doReturn("valid Token").when(spy).generate(anyString(),any(),any());
            return new JwtComponent(spy);
        }

        public RouteBuilder route() {
            return new RouteBuilder() {
                @Override
                public void configure() throws Exception {
                    from(DIRECT_START).routeId(TEST_ROUTE)
                            .process(Exchange::getIn).id(PROCESSOR_NODE)
                            .to(DIRECT_END);
                }
            };
        }

        @Bean
        protected CamelContext createCamelContext() throws Exception {
            CamelContext camelContext = new DefaultCamelContext();
            camelContext.addRoutes(route());
            camelContext.addComponent(JWT, jwtComponent());
            return camelContext;
        }

        @Bean
        protected ProducerTemplate producerTemplate() throws Exception {
            return createCamelContext().createProducerTemplate();
        }
    }

    public JwsServiceValidationResponse response() {
        return new JwsServiceValidationResponse();
    }

    @Test
    @DirtiesContext
    public void testJwtGenerationRoute() throws Exception {
        AdviceWith.adviceWith(camelContext, TEST_ROUTE,
                a -> a.weaveByToUri(DIRECT_END)
                        .replace().to(JWT_GENERATE_ROUTE)
        );

        camelContext.start();

        Exchange exchange = new DefaultExchange(camelContext);
        exchange.getIn().setHeader("cacheKey","processId from cacheKey");
        exchange.getIn().setHeader("customPayload","{\"customPayloadKey1\":\"customPayloadVal1\",\"customPayloadKey2\":\"customPayloadVal2\"}");
        exchange.getIn().setBody("requestBody");
        ProducerTemplate producerTemplate  = camelContext.createProducerTemplate();
        Exchange processedExchange = producerTemplate.send(DIRECT_START, exchange);

        JwtEndpoint jwtEndpoint = camelContext.getEndpoint(JWT_GENERATE_ROUTE, JwtEndpoint.class);

        assertEquals(JwtOperation.valueOf(JwtOperation.GENERATE.name()), jwtEndpoint.getJwtOperation());
    }
    @Test
    @DirtiesContext
    public void testJwtGenerationRouteWithoutCustomPayloadHeader() throws Exception {
        AdviceWith.adviceWith(camelContext, TEST_ROUTE,
                a -> a.weaveByToUri(DIRECT_END)
                        .replace().to(JWT_GENERATE_ROUTE)
        );

        camelContext.start();

        Exchange exchange = new DefaultExchange(camelContext);
        exchange.getIn().setHeader("cacheKey","processId from cacheKey");
        exchange.getIn().setBody("requestBody");
        ProducerTemplate producerTemplate  = camelContext.createProducerTemplate();
        Exchange processedExchange = producerTemplate.send(DIRECT_START, exchange);

        JwtEndpoint jwtEndpoint = camelContext.getEndpoint(JWT_GENERATE_ROUTE, JwtEndpoint.class);

        assertEquals(JwtOperation.valueOf(JwtOperation.GENERATE.name()), jwtEndpoint.getJwtOperation());
    }
}
