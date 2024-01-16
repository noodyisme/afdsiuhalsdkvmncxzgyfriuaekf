package com.capitalone.identity.identitybuilder.policycore.camel.util;

import com.capitalone.api.security.AuthenticationResponseMarshaller;
import com.capitalone.chassis.engine.model.exception.ChassisSystemException;
import com.capitalone.identity.identitybuilder.policycore.camel.PolicyConstants;
import com.capitalone.identity.identitybuilder.policycore.camel.external.logging.RuntimeUpdateEventLogger;
import com.capitalone.identity.identitybuilder.policycore.camel.external.logging.RuntimeUpdateEventSystemLogger;
import com.capitalone.identity.identitybuilder.policycore.configmanagement.ConfigManagementService;
import com.capitalone.identity.identitybuilder.policycore.feature.prerelease.PreReleaseGuard;
import com.capitalone.identity.identitybuilder.policycore.model.ErrorInfo;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.OperationalAuditor;
import com.capitalone.identity.identitybuilder.policycore.schema.SchemaManager;
import com.capitalone.identity.identitybuilder.policycore.service.exception.ForwardedDownstreamException;
import org.apache.camel.*;
import org.apache.camel.support.DefaultExchange;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

import java.util.Collections;
import java.util.HashMap;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.when;

@CamelSpringBootTest
@DirtiesContext
@ContextConfiguration(classes = {
        PolicyErrorUtilIntegrationTest.StartupTestConfig.class,
        PreReleaseGuard.class,

})
@TestPropertySource(properties = {
        "spring.main.allow-bean-definition-overriding=true",
        "csc.dynamic-updates.enabled=false",
        "csc.client-environment=qa",
        "csc.dev-local.enabled=true",
        "csc.dev-local.debug-root-directory=service/exception",
        "identitybuilder.policycore.feature.version-forwarder.enabled=false",
        "identitybuilder.policycore.abac-enforcement.strict=false",
        "identitybuilder.policycore.feature.strict-policy-start-mode=true",
        "chassis.spring.boot.starter.jersey.extensions.metrics.filter.enabled=false",

})
class PolicyErrorUtilIntegrationTest {

    private static final String POLICY_VERSION = "0.0.0";
    private static final String POLICY_NAME = "bad_policy";

    @Autowired
    CamelContext camelContext;

    @Autowired
    ProducerTemplate producerTemplate;

    private Exchange exchange;
    @Mock
    Message message;
    @EndpointInject("policy:bad_policy_0.0.0?timeout=500")
    ProducerTemplate producerA;
    @EndpointInject("direct:bad_policy_0.0.0-illegal-argument-with-cause?timeout=500")
    ProducerTemplate producerB;
    @EndpointInject("direct:bad_policy_0.0.0-chassis-exception?timeout=500")
    ProducerTemplate producerC;

    @EndpointInject("direct:bad_policy_0.0.0-downstream-valid-exception?timeout=500")
    ProducerTemplate producerD;

    @EndpointInject("direct:bad_policy_0.0.0-downstream-invalid-exception?timeout=500")
    ProducerTemplate producerE;

    @Autowired
    PolicyErrorUtil policyErrorUtil;

    @BeforeEach
    void setup() {
        camelContext.start();
        exchange = new DefaultExchange(camelContext);
        when(message.getHeader(PolicyConstants.HEADER_POLICYNAME)).thenReturn(POLICY_NAME);
        when(message.getHeader(PolicyConstants.HEADER_POLICYVERSION_PATCH)).thenReturn(POLICY_VERSION);
        when(message.getBody()).thenReturn(new HashMap<>());
    }

    @Test
    @DirtiesContext
    void testRuntimeExceptionThrown() {
        Exception expected = new RuntimeException();
        Exchange resp = producerA.send(exchange);
        assertThat(resp.getException()).usingRecursiveComparison().hasSameClassAs(expected);
    }

    @Test
    @DirtiesContext
    void testIllegalArgumentExceptionThrown() {
        Exception expected = new IllegalArgumentException("This argument is invalid.");
        Exchange resp = producerB.send(exchange);
        assertThat(resp.getException()).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    @DirtiesContext
    void testChassisExceptionThrown() {
        Exception expected = new ChassisSystemException();
        Exchange resp = producerC.send(exchange);
        //ignore the majority of dev text because chassis uses ToStringBuilder.reflectionToString for it's toString method
        assertThat(resp.getException()).usingRecursiveComparison()
                .withEqualsForFields((s1, s2) -> StringUtils.contains(s1.toString(), "ChassisSystemException")
                                && StringUtils.contains(s2.toString(), "ChassisSystemException"),
                        "errorInfo.developerText").isEqualTo(expected);
    }

    @Test
    @DirtiesContext
    void testValidDownstreamExceptionThrown() {
        Exception expected = new ForwardedDownstreamException(new ErrorInfo("200200", "This is the text test.", "this was from a downstream error."));
        Exchange resp = producerD.send(exchange);
        assertThat(resp.getException()).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    @DirtiesContext
    void testInvalidDownstreamExceptionThrown() {
        Exception expected = new MockDownstreamException(Collections.singletonMap("policyInfo", "Good Policy."));
        Exchange resp = producerE.send(exchange);
        assertThat(resp.getException()).usingRecursiveComparison().isEqualTo(expected);
    }


    @Configuration
    @ComponentScan(
            basePackages = {
                    "com.capitalone.identity.identitybuilder.client",
                    "com.capitalone.identity.identitybuilder.policycore.camel.external.dynamic",
            },
            basePackageClasses = {SchemaManager.class, ConfigManagementService.class}
    )
    public static class StartupTestConfig {

        @Bean
        AuthenticationResponseMarshaller myResponseMarshaller() {
            return (httpServletRequest, httpServletResponse, o) -> "";
        }

        @Bean
        RuntimeUpdateEventLogger testLogger() {
            return new RuntimeUpdateEventSystemLogger();
        }

        @Bean
        OperationalAuditor operationalAuditor() {
            return new OperationalAuditor(null);
        }

        @Bean
        PolicyErrorUtil policyErrorUtil() {
            return new PolicyErrorUtil();
        }

        @MockBean
        RequestParameter requestParameter;

    }

}