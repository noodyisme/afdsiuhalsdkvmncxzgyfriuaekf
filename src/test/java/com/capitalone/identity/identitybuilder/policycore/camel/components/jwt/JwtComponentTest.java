package com.capitalone.identity.identitybuilder.policycore.camel.components.jwt;

import com.capitalone.identity.identitybuilder.policycore.operational_audit.mapper.JwkValidatedEventMapper;
import com.capitalone.identity.identitybuilder.policycore.service.jws.JwsService;
import com.capitalone.identity.identitybuilder.policycore.service.jws.JwtPublicKeyRetrievalService;
import com.capitalone.identity.identitybuilder.policycore.utils.CamelSpringBootContextAwareTest;
import org.apache.camel.*;
import org.apache.camel.builder.AdviceWith;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.test.spring.junit5.UseAdviceWith;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import static com.capitalone.identity.identitybuilder.policycore.camel.components.jwt.JwtComponent.INVALID_ARGUMENTS;
import static com.capitalone.identity.identitybuilder.policycore.utils.TestUtils.*;
import static java.util.Collections.EMPTY_MAP;
import static org.junit.jupiter.api.Assertions.*;

@CamelSpringBootContextAwareTest(
        contextConfigClasses = JwtComponentTest.ContextConfig.class,
        excludeFromComponentScan = @ComponentScan.Filter(type = FilterType.ASPECTJ,
                pattern = "com.capitalone.identity.identitybuilder.policycore.camel.components.crypto-jwt.*"))
@UseAdviceWith
class JwtComponentTest {

    @Autowired
    JwtComponent jwtComponent;

    @Autowired
    CamelContext camelContext;

    @Autowired
    ProducerTemplate producerTemplate;

    @Test
    void testInvalidEndpoint() throws Exception {
        camelContext.start();
        IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class, () -> jwtComponent.createEndpoint(JWT, UUID.randomUUID().toString(), new HashMap<>()));
        assertEquals(String.format(INVALID_ARGUMENTS, JWT), illegalArgumentException.getMessage());
    }

    @Test
    void testEndpointWithoutSrcTokenHeaderOrSrcTokenPathShouldNotThrowException() throws Exception {
        camelContext.start();
        assertDoesNotThrow(() -> jwtComponent.createEndpoint(JWT, "validate", EMPTY_MAP));
    }

    @Test
    void testValidEndpointWithSrcTokenHeader() throws Exception {
        camelContext.start();
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("srcTokenHeader", "customerIdToken");
        JwtEndpoint jwtEndpoint = (JwtEndpoint) jwtComponent.createEndpoint(JWT, "validate", parameters);
        assertEquals(JwtOperation.valueOf("validate".toUpperCase(Locale.ROOT)), jwtEndpoint.getJwtOperation());
        assertEquals("customerIdToken", jwtEndpoint.getSrcTokenHeader());
    }

    @Test
    void testValidEndpointWithSrcTokenPath() throws Exception {
        camelContext.start();
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("srcTokenPath", "customerIdToken");
        JwtEndpoint jwtEndpoint = (JwtEndpoint) jwtComponent.createEndpoint(JWT, "validate", parameters);
        assertEquals(JwtOperation.valueOf("validate".toUpperCase(Locale.ROOT)), jwtEndpoint.getJwtOperation());
        assertEquals("customerIdToken", jwtEndpoint.getSrcTokenPath());
    }

    @Test
    void testUnsupportedConsumer() {
        camelContext.start();
        Exception exception = assertThrows(FailedToCreateRouteException.class, () -> AdviceWith.adviceWith(camelContext, TEST_ROUTE,
                a -> a.replaceFromWith("crypto-jwt:somethingElse")));
        assertEquals(ResolveEndpointFailedException.class, exception.getCause().getClass());
    }

    @Configuration
    public static class ContextConfig {

        @Bean
         JwtComponent jwtComponent() {
            return new JwtComponent(new JwsService(new JwtPublicKeyRetrievalService(WebClient.create()), new JwkValidatedEventMapper.JwkValidationPublisher(null)));
        }

        public RouteBuilder route() {
            return new RouteBuilder() {
                @Override
                public void configure() {
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
}