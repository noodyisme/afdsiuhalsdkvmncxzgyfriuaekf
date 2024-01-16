package com.capitalone.identity.identitybuilder.policycore.configmanagement;

import com.capitalone.api.security.AuthenticationResponseMarshaller;
import com.capitalone.identity.identitybuilder.policycore.camel.PolicyConstants;
import com.capitalone.identity.identitybuilder.policycore.feature.prerelease.PreReleaseGuard;
import com.capitalone.identity.identitybuilder.policycore.schema.SchemaManager;
import org.apache.camel.CamelContext;
import org.apache.camel.CamelExecutionException;
import org.apache.camel.EndpointInject;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.direct.DirectConsumerNotAvailableException;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.model.ModelCamelContext;
import org.apache.camel.model.dataformat.JsonDataFormat;
import org.apache.camel.spring.SpringCamelContext;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@CamelSpringBootTest
@SpringBootApplication
@ContextConfiguration(classes = {
        ConfigManagementMultipleVersionsPolicyLoadTest.StartupTestConfig.class,
        PreReleaseGuard.class,
})
@TestPropertySource(properties = {
        "spring.main.allow-bean-definition-overriding=true",
        "csc.dynamic-updates.enabled=false",
        "csc.client-environment=dev",
        "csc.dev-local.enabled=true",
        "csc.dev-local.debug-root-directory=configmanagement/config_management_multiple_versions_policies",
        "identitybuilder.policycore.feature.version-forwarder.enabled=false",
        "chassis.spring.boot.starter.jersey.extensions.metrics.filter.enabled=false",

})
class ConfigManagementMultipleVersionsPolicyLoadTest {

    @Autowired
    CamelContext camelContext;

    @EndpointInject("policy:config_management_multiple_versions_policies_0.0.0?timeout=1000")
    ProducerTemplate producer;

    @EndpointInject("policy:config_management_multiple_versions_policies_0.1.0?timeout=1000")
    ProducerTemplate producerB;

    @EndpointInject("policy:config_management_multiple_versions_policies_0.2.0?timeout=1000")
    ProducerTemplate producerC;

    @EndpointInject("policy:config_management_multiple_versions_policies_0.3.0?timeout=1000")
    ProducerTemplate producerD;

    @EndpointInject("mock:end")
    MockEndpoint mockEnd;

    @BeforeEach
    void setup() {
        camelContext.start();
    }

    @Test
    @DirtiesContext
    void validPolicyVersionLoadsVersionWithInvalidUseCaseDoesNotLoad() {

        //TODO: ADD A MULTIPLE PATCH VERSION TEST

        testPolicy0_0();

        HashMap<String, Object> headers = new HashMap<>();
        headers.put(PolicyConstants.HEADER_POLICYNAME, "config_management_policy");
        headers.put(PolicyConstants.HEADER_POLICYVERSION, "0.1");
        headers.put(PolicyConstants.HEADER_BUSINESS_EVENT, "useCase_A_invalid");
        Map<Object, Object> body = Collections.emptyMap();
        RuntimeException e = assertThrows(CamelExecutionException.class,
                () -> producerB.sendBodyAndHeaders(body, headers));

        assertTrue(e.getCause() instanceof DirectConsumerNotAvailableException);
    }

    @Test
    @DirtiesContext
    void validPolicyVersionLoadsVersionWithInvalidDefaultsDoesNotLoad() {
        testPolicy0_0();

        HashMap<String, Object> headers = new HashMap<>();
        headers.put(PolicyConstants.HEADER_POLICYNAME, "config_management_policy");
        headers.put(PolicyConstants.HEADER_POLICYVERSION, "0.2");
        headers.put(PolicyConstants.HEADER_BUSINESS_EVENT, "useCase_A_valid");
        Map<Object, Object> body = Collections.emptyMap();
        RuntimeException e = assertThrows(CamelExecutionException.class,
                () -> producerC.sendBodyAndHeaders(body, headers));

        assertTrue(e.getCause() instanceof DirectConsumerNotAvailableException);
    }

    @Test
    @DirtiesContext
    void validPolicyVersionLoadsVersionWithInvalidSchemaDoesNotLoad() {
        testPolicy0_0();

        HashMap<String, Object> headers = new HashMap<>();
        headers.put(PolicyConstants.HEADER_POLICYNAME, "config_management_policy");
        headers.put(PolicyConstants.HEADER_POLICYVERSION, "0.3");
        headers.put(PolicyConstants.HEADER_BUSINESS_EVENT, "useCase_A_valid");
        Map<Object, Object> body = Collections.emptyMap();
        RuntimeException e = assertThrows(CamelExecutionException.class,
                () -> producerD.sendBodyAndHeaders(body, headers));

        assertTrue(e.getCause() instanceof DirectConsumerNotAvailableException);
    }

    void testPolicy0_0() {
        HashMap<String, Object> headers = new HashMap<>();
        headers.put(PolicyConstants.HEADER_POLICYNAME, "config_management_policy");
        headers.put(PolicyConstants.HEADER_POLICYVERSION, "0.0");
        headers.put(PolicyConstants.HEADER_BUSINESS_EVENT, "useCase_A_valid");
        producer.sendBodyAndHeaders(new HashMap<>(), headers);
        String outputHeaderKey = "testOutputHeader";
        String resultA = (String) mockEnd.getReceivedExchanges().get(0).getMessage().getHeader(outputHeaderKey);
        assertEquals("code-0.0", resultA);
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
        protected CamelContext camelContext(ApplicationContext applicationContext) {
            CamelContext camelContext = new SpringCamelContext(applicationContext);
            camelContext.adapt(ModelCamelContext.class)
                    .setDataFormats(Collections.singletonMap("jackson", new JsonDataFormat()));
            camelContext.build();
            return camelContext;
        }

        @Bean
        protected ProducerTemplate producerTemplate(CamelContext camelContext) {
            return camelContext.createProducerTemplate();
        }

        @Bean
        AuthenticationResponseMarshaller myResponseMarshaller() {
            return (httpServletRequest, httpServletResponse, o) -> "";
        }

    }

}
