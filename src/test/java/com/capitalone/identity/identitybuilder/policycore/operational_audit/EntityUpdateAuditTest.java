package com.capitalone.identity.identitybuilder.policycore.operational_audit;

import com.capitalone.identity.identitybuilder.audit.models.AuditEvent;
import com.capitalone.identity.identitybuilder.audit.sdpv4.AuditPublisher;
import com.capitalone.identity.identitybuilder.audit.sdpv4.ProducerProperties;
import com.capitalone.identity.identitybuilder.model.ConfigStoreItemInfo;
import com.capitalone.identity.identitybuilder.model.EntityInfo;
import com.capitalone.identity.identitybuilder.model.EntityState;
import com.capitalone.identity.identitybuilder.model.EntityType;
import com.capitalone.identity.identitybuilder.policycore.camel.external.dynamic.UpdateTransactionResult;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.cyber.AWSHostContextCollector;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.mapper.EntityUpdateEventMapper;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.mapper.OperationalEventEnricher;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.model.*;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.model.configstore.EntityUpdateEventOccurred;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.model.configstore.OperationalConfigStoreMetadata;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.model.type.OperationalEventType;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.util.MockHostContext;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.util.OperationalAuditPublisher;
import com.google.common.collect.Sets;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.Set;

public class EntityUpdateAuditTest {

    private final static Set<ConfigStoreItemInfo> expectedItems = Sets.newHashSet(
            new ConfigStoreItemInfo("testName1", "testTag1"),
            new ConfigStoreItemInfo("testName2", "testTag2"));
    private final static String expectedEntityId = "testId1";
    private final static String expectedVersion = "2.1";
    private final static Set<String> expectedFilteredItemNames = Sets.newHashSet("README.md", "IgnoreMe");

    private static final HashMap<String, Object> expectedHostContext = new HashMap<>();


    private static EntityInfo buildDefaultMockEntityInfo(final EntityType expectedEntityType) {
        final EntityInfo retVal = Mockito.mock(EntityInfo.class);
        Mockito.when(retVal.getItemInfo()).thenReturn(expectedItems);
        Mockito.when(retVal.getId()).thenReturn(expectedEntityId);
        Mockito.when(retVal.getType()).thenReturn(expectedEntityType);
        Mockito.when(retVal.getVersion()).thenReturn(expectedVersion);
        Mockito.when(retVal.getFilteredItemNames()).thenReturn(expectedFilteredItemNames);
        return retVal;
    }

    // Dependencies
    private AuditPublisher auditPublisher;
    private OperationalSystemContext operationalSystemContext;

    // Argument captor
    private ArgumentCaptor<AuditEvent> auditEventArgumentCaptor;

    // Object under test
    private OperationalAuditor operationalAuditor;
    private final AWSHostContextCollector mockAWSHostContextCollector = new AWSHostContextCollector(new MockHostContext());

    @BeforeAll
    static void setExpectedHostContext() {
        expectedHostContext.put("arn", "arn:aws:ecs:us-west-2:123456789012:service/cluster-name/service-name");
        expectedHostContext.put("mac", "empty-mac");
        expectedHostContext.put("region", "us-west-2");
        expectedHostContext.put("threadId", String.valueOf(Thread.currentThread().getId()));

    }

    @BeforeEach
    void setup() {
        // Initialize the aspect
        auditPublisher = Mockito.mock(AuditPublisher.class);
        ProducerProperties producerProperties = Mockito.mock(ProducerProperties.class);
        Mockito.lenient().when(producerProperties.getSchemaName()).thenReturn("testSchema");
        operationalSystemContext = Mockito.mock(OperationalSystemContext.class);
        operationalAuditor = new OperationalAuditor(
                new OperationalAuditor.OperationalAuditDispatcher(
                        new OperationalEventEnricher(operationalSystemContext, true),
                        new OperationalAuditPublisher(auditPublisher, producerProperties.getSchemaName()),
                        mockAWSHostContextCollector));

        // Initialize the argument captor used for verification
        auditEventArgumentCaptor = ArgumentCaptor.forClass(AuditEvent.class);
    }

    @Test
    void entityBootstrapAuditSuccessEventSuccess() {
        // Arrange
        final OperationalEventType expectedOperationalEventType = OperationalEventType.ENTITY_BOOTSTRAPPED;
        final EntityState.Delta.ChangeType expectedChangeType = EntityState.Delta.ChangeType.ADD;
        final EntityType expectedEntityType = EntityType.ACCESS;
        final EntityInfo expectedEntityInfo = buildDefaultMockEntityInfo(expectedEntityType);
        final UpdateTransactionResult expectedResult = UpdateTransactionResult.success();
        final Long expectedStartTime = System.currentTimeMillis();

        // Act
        operationalAuditor.audit(EntityUpdateEventOccurred.builder()
                        .operationalEventType(expectedOperationalEventType)
                        .changeType(expectedChangeType)
                        .info(expectedEntityInfo)
                        .result(expectedResult)
                        .startTimestamp(expectedStartTime)
                        .endTimestamp(System.currentTimeMillis())
                        .build(),
                EntityUpdateEventMapper.Factory.class);

        // Assert
        assertExpectedOperationalEvent(expectedOperationalEventType,
                expectedEntityType, expectedChangeType,
                OperationalEventOutcome.of(OperationalStatus.SUCCESS, "INFO"), null);
    }

    @Test
    void entityUpdateAuditFailureEventSuccess() {
        // Arrange
        final OperationalEventType expectedOperationalEventType = OperationalEventType.ENTITY_UPDATED;
        final EntityState.Delta.ChangeType expectedChangeType = EntityState.Delta.ChangeType.UPDATE;
        final EntityType expectedEntityType = EntityType.ACCESS;
        final EntityInfo expectedEntityInfo = buildDefaultMockEntityInfo(expectedEntityType);
        final UpdateTransactionResult expectedResult = UpdateTransactionResult.error(new RuntimeException("test"));
        final Long expectedStartTime = System.currentTimeMillis();

        // Act
        operationalAuditor.audit(EntityUpdateEventOccurred.builder()
                        .operationalEventType(expectedOperationalEventType)
                        .changeType(expectedChangeType)
                        .info(expectedEntityInfo)
                        .result(expectedResult)
                        .startTimestamp(expectedStartTime)
                        .endTimestamp(System.currentTimeMillis())
                        .build(),
                EntityUpdateEventMapper.Factory.class);


        // Assert
        assertExpectedOperationalEvent(expectedOperationalEventType,
                expectedEntityType, expectedChangeType, OperationalEventOutcome.of(OperationalStatus.FAILURE, "ERROR"),
                OperationalError.builder()
                        .developerText("RuntimeException: test")
                        .build());
    }

    private void assertExpectedOperationalEvent(final OperationalEventType expectedOperationalEventType,
                                                final EntityType expectedEntityType,
                                                final EntityState.Delta.ChangeType expectedChangeType,
                                                final OperationalEventOutcome operationalEventOutcome,
                                                final OperationalError expectedError) {
        final OperationalEvent operationalEvent = OperationalAuditTestUtil.captureOperationalEvent(
                auditPublisher, auditEventArgumentCaptor);

        // Entity Naming Assertions
        Assertions.assertEquals(expectedOperationalEventType.toString(), operationalEvent.getEventName());
        Assertions.assertEquals(expectedOperationalEventType.getEntity().toString(), operationalEvent.getEventEntity());
        Assertions.assertEquals(expectedOperationalEventType.getAction().toString(), operationalEvent.getEventAction());

        // Status
        Assertions.assertEquals(operationalEventOutcome, operationalEvent.getEventOutcome());

        // System Context
        Assertions.assertSame(operationalSystemContext, operationalEvent.getSystemContext());

        // Request Context
        Assertions.assertEquals("UNDEFINED", operationalEvent.getRequestMessageId());
        Assertions.assertEquals("UNDEFINED", operationalEvent.getRequestCorrelationId());

        // Error Metadata
        if (operationalEventOutcome.getResult().equals("SUCCESS")) {
            Assertions.assertNull(operationalEvent.getErrorMetadata());
        } else {
            Assertions.assertEquals(expectedError, operationalEvent.getErrorMetadata());
        }

        // Timed Metadata
        Assertions.assertNull(operationalEvent.getEventOperationId());
        Assertions.assertTrue(operationalEvent.getEventStartTimestamp() <= System.currentTimeMillis());
        Assertions.assertTrue(operationalEvent.getEventEndTimestamp() >= operationalEvent.getEventStartTimestamp());
        Assertions.assertEquals(operationalEvent.getEventEndTimestamp() - operationalEvent.getEventStartTimestamp(),
                Long.valueOf(operationalEvent.getEventDurationMs()));

        // Event Entity Name
        Assertions.assertEquals(expectedEntityId, operationalEvent.getEventEntityName());

        // Policy Metadata
        Assertions.assertNull(operationalEvent.getPolicyMetadata());

        // Pip Metadata
        Assertions.assertNull(operationalEvent.getPipMetadata());

        // Execution Context
        Assertions.assertNull(operationalEvent.getExecutionContext());

        // Config Store Metadata
        Assertions.assertNotNull(operationalEvent.getConfigStoreMetadata());
        final OperationalConfigStoreMetadata operationalConfigStoreMetadata =
                operationalEvent.getConfigStoreMetadata();
        Assertions.assertEquals(expectedChangeType.toString(), operationalConfigStoreMetadata.getChangeType());
        Assertions.assertEquals(expectedEntityType.toString(), operationalConfigStoreMetadata.getEntityType());
        Assertions.assertEquals(expectedEntityId, operationalConfigStoreMetadata.getEntityId());
        Assertions.assertEquals(expectedVersion, operationalConfigStoreMetadata.getEntityVersion());
        Assertions.assertEquals(expectedItems.toString(), operationalConfigStoreMetadata.getEntityItems());
        Assertions.assertEquals(String.valueOf(expectedItems.size()), operationalConfigStoreMetadata.getItemsCount());
        Assertions.assertEquals(expectedFilteredItemNames.toString(), operationalConfigStoreMetadata.getIgnoredNonPolicyItems());
        Assertions.assertNull(operationalConfigStoreMetadata.getScanScheduled());
        Assertions.assertNull(operationalConfigStoreMetadata.getScanType());
        Assertions.assertNull(operationalConfigStoreMetadata.getNewPollingConfig());

        // Dmn Metadata
        Assertions.assertNull(operationalEvent.getDmnMetadata());

        // Supplemental Metadata
        Assertions.assertTrue(operationalEvent.getSupplementalMetadata().isEmpty());

        //cyber data
        Assertions.assertEquals(expectedHostContext.get("arn"), operationalEvent.getAmazonResourceName());
        Assertions.assertEquals(expectedHostContext.get("mac"), operationalEvent.getHostMacAddress());
        Assertions.assertEquals(expectedHostContext.get("threadId"), operationalEvent.getThreadId());


    }
}
