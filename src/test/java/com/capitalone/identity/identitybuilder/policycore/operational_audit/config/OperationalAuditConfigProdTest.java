package com.capitalone.identity.identitybuilder.policycore.operational_audit.config;

import com.capitalone.chassis.engine.model.context.RequestContext;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.cyber.AWSHostContextCollector;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.mapper.OperationalEventEnricher;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.model.OperationalEvent;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.model.OperationalSystemContext;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.util.MockHostContext;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.util.MockRequestContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

@TestPropertySource(properties = {
        "onestream.host=test",
        "onestream.port=8080",
        "SERVICE_NAME=test_service",
        "ba=test",
        "IDB-DECISION-AUDIT-CLIENT-ID=bilbo",
        "IDB-DECISION-AUDIT-CLIENT-SECRET=baggins",
        "OPERATIONAL-AUDIT-CLIENT-ID=frodo",
        "OPERATIONAL-AUDIT-CLIENT-SECRET=samwise",
        "env.gatewayURL=test",
        "ENVIRONMENT_NAME=prod",
        "CONTAINER_ID=testcontainer",
        "HOSTNAME=testhost",
        "identitybuilder.policycore.operational_audit.suppress_stack_trace=false",
        "identitybuilder.policycore.operational_audit.schema_name=test",
        "identitybuilder.policycore.operational_audit.enabled=true",
        "chassis.config.env=prod"
})
@SpringBootTest(classes = OperationalAuditConfig.class)
@ContextConfiguration(classes = {OperationalAuditConfigTestUtil.MockedChassisConfig.class})
@DirtiesContext
public class OperationalAuditConfigProdTest {
    private final AWSHostContextCollector mockAWSHostContextCollector = new AWSHostContextCollector(new MockHostContext());
    private final RequestContext mockRequestContext = new MockRequestContext().MockRequestContext();
    private final OperationalEventEnricher.CyberAttributes cyberAttributes = mockAWSHostContextCollector.setCyberAttributes(this.getClass().getName(), mockRequestContext);
    @Autowired
    private OperationalAuditConfig operationalAuditConfig;
    @Autowired
    private OperationalEventEnricher operationalEventEnricher;

    @Test
    public void testStartupSuccess() {
        Assertions.assertNotNull(operationalAuditConfig);
    }

    @Test
    public void testSensitiveErrorInfoSuppressedIfNeededSuccess() {
        // Act
        final OperationalEvent operationalAuditEvent = operationalEventEnricher.enrich(OperationalEvent.builder().build(),
                OperationalAuditConfigTestUtil.buildDefaultCommonEnrichmentInputWithError(), cyberAttributes);

        // Assert
        Assertions.assertNull(operationalAuditEvent.getErrorMetadata().getStackTrace());
        Assertions.assertEquals("REDACTED", operationalAuditEvent.getEventDetail());
    }

    @Test
    public void testOperationalSystemContextOnlyRequiredFieldsPresentSuccess() {
        // Arrange
        final OperationalSystemContext expectedOperationalSystemContext = OperationalSystemContext.builder()
                .applicationName("dummy-app")
                .applicationCode("core-test-code")
                .clusterId(null)
                .containerId("testcontainer")
                .hostName("testhost")
                .region(null)
                .environmentName("prod")
                .clientId(null)
                .businessApplication("test")
                .systemId("core-test-code.prod.NA.NA-testcontainer")
                .build();

        // Act
        final OperationalEvent operationalEvent = operationalEventEnricher.enrich(OperationalEvent.builder().build(),
                OperationalAuditConfigTestUtil.buildDefaultCommonEnrichmentInput(), cyberAttributes);

        // Assert
        Assertions.assertEquals(expectedOperationalSystemContext, operationalEvent.getSystemContext());
    }
}
