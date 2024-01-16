package com.capitalone.identity.identitybuilder.policycore.camel.external.dynamic;

import com.capitalone.api.security.AuthenticationResponseMarshaller;
import com.capitalone.identity.identitybuilder.policycore.abac.config.PolicyAccessRegistry;
import com.capitalone.identity.identitybuilder.policycore.configmanagement.ConfigManagementService;
import com.capitalone.identity.identitybuilder.policycore.decisionengine.DecisionEngineRuntimeLoadService;
import com.capitalone.identity.identitybuilder.policycore.externalization.EntityUpdateService;
import com.capitalone.identity.identitybuilder.policycore.schema.SchemaManager;
import com.capitalone.identity.identitybuilder.policycore.service.versions.PolicyVersionService;
import org.apache.camel.CamelContext;
import org.apache.camel.EndpointInject;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

import java.util.HashMap;

import static com.capitalone.identity.identitybuilder.policycore.service.versions.PolicyVersionService.FEATURE_FLAG_VERSION_FORWARDER_KEY;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@CamelSpringBootTest
@SpringBootApplication
@ContextConfiguration(classes = DynamicPolicyManagerIntegrationTest.StartupTestConfig.class)
@TestPropertySource(properties = {
        "chassis.spring.boot.starter.jersey.extensions.metrics.filter.enabled=false",
        "spring.main.allow-bean-definition-overriding=true",
        "csc.dynamic-updates.enabled=false",
        "csc.client-environment=qa",
        "csc.dev-local.enabled=true",
        "csc.dev-local.debug-root-directory=externalization/startup",
        FEATURE_FLAG_VERSION_FORWARDER_KEY + "=false",
        "identitybuilder.policycore.operational_audit.enabled=false"
})
class DynamicPolicyManagerIntegrationTest {

    @Autowired
    ApplicationContext applicationContext;

    @Autowired
    private CamelContext context;

    @EndpointInject("policy:print_body_policy_1.0.0?timeout=1000")
    ProducerTemplate producer;

    @BeforeEach
    void setup() {
        context.start();
    }

    @Test
    void sendMessage() {
        assertNotNull(context);
        assertDoesNotThrow(() -> producer.sendBody(new HashMap<String, Object>()));
    }

    @Configuration
    @ComponentScan(
            basePackages = {
                    "com.capitalone.identity.identitybuilder.client",
                    "com.capitalone.identity.identitybuilder.policycore.camel.external.dynamic",
                    "com.capitalone.identity.identitybuilder.policycore.externalization"
            },
            basePackageClasses = {SchemaManager.class, ConfigManagementService.class, PolicyAccessRegistry.class,
                    EntityUpdateService.class, PolicyVersionService.class}
    )
    public static class StartupTestConfig {

        @Bean
        AuthenticationResponseMarshaller myResponseMarshaller() {
            return (httpServletRequest, httpServletResponse, o) -> "";
        }

        @Bean
        DecisionEngineRuntimeLoadService mockDecisionEngine() {
            return Mockito.mock(DecisionEngineRuntimeLoadService.class);
        }

    }
}
