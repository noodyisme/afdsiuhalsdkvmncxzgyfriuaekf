package com.capitalone.identity.identitybuilder.policycore.configmanagement.camel;

import com.capitalone.api.security.AuthenticationResponseMarshaller;
import com.capitalone.identity.identitybuilder.client.dynamic.DynamicUpdateConfigurationProperties;
import com.capitalone.identity.identitybuilder.policycore.camel.PolicyConstants;
import com.capitalone.identity.identitybuilder.policycore.configmanagement.ConfigManagementService;
import com.capitalone.identity.identitybuilder.policycore.configmanagement.UnsupportedBusinessEventException;
import com.capitalone.identity.identitybuilder.policycore.feature.prerelease.PreReleaseGuard;
import com.capitalone.identity.identitybuilder.policycore.feature.property.ConfigManagementPropertySetter;
import com.capitalone.identity.identitybuilder.policycore.schema.SchemaManager;
import org.apache.camel.*;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.model.ModelCamelContext;
import org.apache.camel.model.dataformat.JsonDataFormat;
import org.apache.camel.spring.SpringCamelContext;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.mock.mockito.MockBean;
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

import static com.capitalone.identity.identitybuilder.policycore.service.versions.PolicyVersionService.FEATURE_FLAG_VERSION_FORWARDER_KEY;
import static org.junit.jupiter.api.Assertions.*;

@CamelSpringBootTest
@SpringBootApplication
@ContextConfiguration(classes = {
        ConfigManagementComponentTest.StartupTestConfig.class,
        PreReleaseGuard.class,
        ConfigManagementPropertySetter.class,
})
@TestPropertySource(properties = {
        "spring.main.allow-bean-definition-overriding=true",
        "csc.dynamic-updates.enabled=false",
        "csc.client-environment=dev",
        "csc.dev-local.enabled=true",
        "csc.dev-local.debug-root-directory=configmanagement/config_management_policy",
        "identitybuilder.policycore.feature.strict-policy-start-mode=true",
        "chassis.spring.boot.starter.jersey.extensions.metrics.filter.enabled=false",
        FEATURE_FLAG_VERSION_FORWARDER_KEY + "=false",
})
class ConfigManagementComponentTest {

    @EndpointInject("policy:config_management_policy_0.0.0?timeout=1000")
    ProducerTemplate producer;

    @EndpointInject("policy:config_management_policy_0.1.0?timeout=1000")
    ProducerTemplate producerB;

    @EndpointInject("policy:config_management_policy_0.2.0?timeout=1000")
    ProducerTemplate producerC;

    @EndpointInject("mock:end")
    MockEndpoint mockEnd;

    @MockBean
    DynamicUpdateConfigurationProperties properties;

    @Autowired
    CamelContext camelContext;

    @BeforeEach
    void setup() {
        camelContext.start();
    }

    @Test
    @DirtiesContext
    void sendConfigManagementComponentMessage_0_0() {
        HashMap<String, Object> headers = new HashMap<>();
        headers.put(PolicyConstants.HEADER_POLICYNAME, "config_management_policy");
        headers.put(PolicyConstants.HEADER_POLICYVERSION, "0.0");
        headers.put(PolicyConstants.HEADER_BUSINESS_EVENT, "useCase_A_valid");
        producer.sendBodyAndHeaders(new HashMap<>(), headers);
        String outputHeaderKey = "testOutputHeader";
        String resultA = (String) mockEnd.getReceivedExchanges().get(0).getMessage().getHeader(outputHeaderKey);
        assertEquals("code-0.0", resultA);
    }

    @Test
    @DirtiesContext
    void sendConfigManagementComponentMessage_error() {
        HashMap<String, Object> headers = new HashMap<>();
        headers.put(PolicyConstants.HEADER_POLICYNAME, "config_management_policy");
        headers.put(PolicyConstants.HEADER_POLICYVERSION, "0.0");
        headers.put(PolicyConstants.HEADER_BUSINESS_EVENT, "useCase_X_not_exist");
        Map<Object, Object> body = Collections.emptyMap();
        RuntimeException e = assertThrows(CamelExecutionException.class,
                () -> producer.sendBodyAndHeaders(body, headers));

        assertTrue(e.getCause() instanceof UnsupportedBusinessEventException);
    }

    @Test
    @DirtiesContext
    void sendConfigManagementComponentMessage_0_1() {
        final String outputHeaderKey = "testOutputHeader";
        HashMap<String, Object> headers = new HashMap<>();
        headers.put(PolicyConstants.HEADER_POLICYNAME, "config_management_policy");
        headers.put(PolicyConstants.HEADER_POLICYVERSION, "0.1");
        headers.put(PolicyConstants.HEADER_BUSINESS_EVENT, "useCase_A_valid");
        producerB.sendBodyAndHeaders(new HashMap<>(), headers);
        String resultB = (String) mockEnd.getReceivedExchanges().get(0).getMessage().getHeader(outputHeaderKey);
        assertEquals("code-0.1", resultB);
    }

    @Test
    @DirtiesContext
    void sendConfigManagementComponentMessage_0_2_featureProperty() {
        final String outputHeaderKey = "testOutputHeader";
        HashMap<String, Object> headers = new HashMap<>();
        headers.put(PolicyConstants.HEADER_POLICYNAME, "config_management_policy");
        headers.put(PolicyConstants.HEADER_POLICYVERSION, "0.2");
        producerC.sendBodyAndHeaders(new HashMap<>(), headers);
        String resultB = (String) mockEnd.getReceivedExchanges().get(0).getMessage().getHeader(outputHeaderKey);
        assertEquals("ConfigFeatureProperty-Default", resultB);
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
