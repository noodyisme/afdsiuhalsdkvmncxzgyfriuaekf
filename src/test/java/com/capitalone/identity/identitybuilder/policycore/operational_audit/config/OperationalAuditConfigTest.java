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
        "ba=test",
        "SERVICE_NAME=policy_core_test",
        "IDB-DECISION-AUDIT-CLIENT-ID=bilbo",
        "IDB-DECISION-AUDIT-CLIENT-SECRET=baggins",
        "OPERATIONAL-AUDIT-CLIENT-ID=frodo",
        "OPERATIONAL-AUDIT-CLIENT-SECRET=samwise",
        "env.gatewayURL=test",
        "chassis.config.env=dev",
        "identitybuilder.policycore.operational_audit.suppress_stack_trace=true",
        "identitybuilder.policycore.operational_audit.schema_name=test",
        "chassis.app.name=testappname",
        "chassis.app.code=testappcode",
        "CLUSTER_ID=testappcluster",
        "CONTAINER_ID=testappcontainer",
        "HOSTNAME=testapphost",
        "DEPLOYMENT_REGION=testappregion",
        "ENVIRONMENT_NAME=testappenv",
        "SERVICE_NAME=testapp",
        "testapp_client_id=testappclientid",
        "identitybuilder.policycore.operational_audit.enabled=true"
})
@SpringBootTest(classes = {OperationalAuditConfig.class})
@ContextConfiguration(classes = {OperationalAuditConfigTestUtil.MockedChassisConfig.class})
@DirtiesContext
class OperationalAuditConfigTest {
    private final AWSHostContextCollector mockAWSHostContextCollector = new AWSHostContextCollector(new MockHostContext());
    private final RequestContext mockRequestContext = new MockRequestContext().MockRequestContext();
    private final OperationalEventEnricher.CyberAttributes cyberAttributes = mockAWSHostContextCollector.setCyberAttributes(this.getClass().getName(), mockRequestContext);
    @Autowired
    private OperationalAuditConfig operationalAuditConfig;

    @Autowired
    private OperationalEventEnricher operationalEventEnricher;

    // Assert injection successful and that debug error text suppressed if specified to do so in config
    @Test
    public void testStartupSuccess() {
        // Assert
        Assertions.assertNotNull(operationalAuditConfig);
    }

    @Test
    public void testSensitiveErrorInfoSuppressedIfNeededSuccess() {
        // Act
        final OperationalEvent operationalEvent = operationalEventEnricher.enrich(OperationalEvent.builder().build(),
                OperationalAuditConfigTestUtil.buildDefaultCommonEnrichmentInputWithError(), cyberAttributes);

        // Assert
        Assertions.assertNull(operationalEvent.getErrorMetadata().getStackTrace());
        Assertions.assertEquals("REDACTED", operationalEvent.getEventDetail());
    }

    @Test
    public void testOperationalSystemContextAllFieldsPresentSuccess() {
        // Arrange
        final OperationalSystemContext expectedOperationalSystemContext = OperationalSystemContext.builder()
                .applicationName("testappname")
                .applicationCode("testappcode")
                .clusterId("testappcluster")
                .containerId("testappcontainer")
                .hostName("testapphost")
                .region("testappregion")
                .environmentName("testappenv")
                .clientId("testappclientid")
                .businessApplication("test")
                .systemId("testappcode.testappenv.testappregion.testappcluster-testappcontainer")
                .build();

        // Act
        final OperationalEvent operationalEvent = operationalEventEnricher.enrich(OperationalEvent.builder().build(),
                OperationalAuditConfigTestUtil.buildDefaultCommonEnrichmentInput(), cyberAttributes);

        // Assert
        Assertions.assertEquals(expectedOperationalSystemContext, operationalEvent.getSystemContext());
    }
}