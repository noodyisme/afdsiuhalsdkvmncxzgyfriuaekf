package com.capitalone.identity.identitybuilder.policycore.operational_audit.config;

import com.capitalone.chassis.engine.model.context.RequestContext;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.cyber.AWSHostContextCollector;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.mapper.OperationalEventEnricher;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.model.OperationalEvent;
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
        "identitybuilder.policycore.operational_audit.batch_size=100",
        "identitybuilder.policycore.operational_audit.wait_between_batches=150",
        "identitybuilder.policycore.operational_audit.concurrent_connections=4",
        "identitybuilder.policycore.operational_audit.initial_backoff=250",
        "identitybuilder.policycore.operational_audit.retry_timeout=3000",
        "identitybuilder.policycore.operational_audit.max_allowed_retries=3",
        "identitybuilder.policycore.operational_audit.preemptive_fallback=true",
        "identitybuilder.policycore.operational_audit.max_queue_size=100000",
        "identitybuilder.policycore.operational_audit.fixed_thread_pool_size=4",
        "onestream.host=test",
        "onestream.port=8080",
        "ba=test",
        "env.gatewayURL=test",
        "IDB-DECISION-AUDIT-CLIENT-ID=bilbo",
        "IDB-DECISION-AUDIT-CLIENT-SECRET=baggins",
        "OPERATIONAL-AUDIT-CLIENT-ID=frodo",
        "OPERATIONAL-AUDIT-CLIENT-SECRET=samwise",
        "chassis.app.name=testapp",
        "chassis.app.code=testcode",
        "CLUSTER_ID=testclusterid",
        "CONTAINER_ID=testcontainerid",
        "HOSTNAME=testhostname",
        "DEPLOYMENT_REGION=testregion",
        "ENVIRONMENT_NAME=unittest",
        "identitybuilder.policycore.operational_audit.suppress_stack_trace=false",
        "identitybuilder.policycore.operational_audit.schema_name=test",
        "identitybuilder.policycore.operational_audit.enabled=true"
})
@SpringBootTest(classes = OperationalAuditConfig.class)
@ContextConfiguration(classes = {OperationalAuditConfigTestUtil.MockedChassisConfig.class})
@DirtiesContext
public class OperationalAuditConfigUnsuppressedTest {
    private final AWSHostContextCollector mockAWSHostContextCollector = new AWSHostContextCollector(new MockHostContext());
    RequestContext mockRequestContext = new MockRequestContext().MockRequestContext();
    private final OperationalEventEnricher.CyberAttributes cyberAttributes = mockAWSHostContextCollector.setCyberAttributes(this.getClass().getName(), mockRequestContext);
    @Autowired
    private OperationalAuditConfig requestEventAuditConfig;
    @Autowired
    private OperationalEventEnricher enrichmentHelper;

    @Test
    public void testConfigurationBootSuccess() {
        Assertions.assertNotNull(requestEventAuditConfig);
    }

    @Test
    public void testSensitiveErrorInfoSuppressedIfNeededSuccess() {
        // Act
        final OperationalEvent operationalAuditEvent = enrichmentHelper.enrich(OperationalEvent.builder().build(),
                OperationalAuditConfigTestUtil.buildDefaultCommonEnrichmentInputWithError(), cyberAttributes);

        // Assert
        Assertions.assertNotNull(operationalAuditEvent.getErrorMetadata().getStackTrace());
        Assertions.assertNotEquals("REDACTED", operationalAuditEvent.getEventDetail());
    }

}