package com.capitalone.identity.identitybuilder.policycore.decisionengine;

import com.capitalone.api.security.AuthenticationResponseMarshaller;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;

@CamelSpringBootTest
@ContextConfiguration(classes = DecisionAuditLoggerNoOpIntegrationTest.StartupTestConfig.class)
@TestPropertySource(properties = {
        "identitybuilder.policycore.feature.decisionengine.output-audit-logger.enabled=false",

})
class DecisionAuditLoggerNoOpIntegrationTest {

    @Autowired
    DecisionAuditLogger auditLogger;

    @Test
    void sendMessage() {
        assertNotNull(auditLogger);
        assertTrue(auditLogger instanceof DecisionAuditLoggerNoOp);
        assertDoesNotThrow(() -> auditLogger.auditDecisionResults(null, null, null));
    }

    @Configuration
    @ComponentScan(basePackageClasses = {DecisionAuditLogger.class})
    public static class StartupTestConfig {

        @Bean
        AuthenticationResponseMarshaller myResponseMarshaller() {
            return (httpServletRequest, httpServletResponse, o) -> "";
        }

        @Bean
        DecisionEngineRuntimeLoadService decisionEngineRuntimeLoadService() {
            return Mockito.mock(DecisionEngineRuntimeLoadService.class);
        }
    }
}
