package com.capitalone.identity.identitybuilder.policycore.externalization;

import com.capitalone.identity.identitybuilder.ClientEnvironment;
import com.capitalone.identity.identitybuilder.client.ConfigStoreClient;
import com.capitalone.identity.identitybuilder.client.dynamic.DynamicUpdateConfigurationProperties;
import com.capitalone.identity.identitybuilder.model.*;
import com.capitalone.identity.identitybuilder.policycore.abac.config.PolicyAccessRegistry;
import com.capitalone.identity.identitybuilder.policycore.camel.external.dynamic.MockEntityInfo;
import com.capitalone.identity.identitybuilder.policycore.camel.external.dynamic.UpdateTransactionResult;
import com.capitalone.identity.identitybuilder.policycore.camel.external.logging.RuntimeUpdateEventAuditLogger;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.OperationalAuditor;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.mapper.EntityUpdateEventMapper;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.model.OperationalEventOccurred;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.model.configstore.EntityUpdateEventOccurred;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;

import java.util.Collections;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DynamicEntityManagerTest {

    private static final EntityType policyType = EntityType.PIP;
    private static final String childItemName = "test/itemA";
    private static final String childItemTag = "tagA";
    private static final String policyName = "test";
    DynamicUpdateConfigurationProperties properties;
    @Mock
    ConfigStoreClient configStoreClient;
    @Mock
    Entity.Access mockEntity;
    @Mock
    EntityInfo.Access mockEntityInfo;
    @Mock
    RuntimeUpdateEventAuditLogger updateEventLogger;
    @Mock
    OperationalAuditor operationalAuditor;
    @Mock
    PolicyAccessRegistry policyAccessRegistry;
    @Captor
    ArgumentCaptor<OperationalEventOccurred> operationalEventOccurredArgumentCaptor;
    DynamicEntityManager manager;
    private EntityUpdateService entityUpdateService;

    private static EntityState.Delta<EntityInfo> getTestChangeObject(EntityState.Delta.ChangeType changeType) {
        ConfigStoreItemInfo item = new ConfigStoreItemInfo(childItemName, childItemTag);
        MockEntityInfo mockEntityInfo = new MockEntityInfo(policyName, policyType, item);
        return new EntityState.Delta<>(changeType, mockEntityInfo);
    }

    @BeforeEach
    void createResult() {
        properties = new DynamicUpdateConfigurationProperties(null, "PT2S");
        AccessControlEntityUpdater accessControlEntityUpdater = new AccessControlEntityUpdater(policyAccessRegistry, ClientEnvironment.QA);
        entityUpdateService = new EntityUpdateService(configStoreClient, accessControlEntityUpdater);
        manager = new DynamicEntityManager(configStoreClient, updateEventLogger, false, operationalAuditor, properties, entityUpdateService, ClientEnvironment.QA);
        Mockito.lenient().when(configStoreClient.getEntityInfo(any())).thenReturn(Flux.empty());
        Mockito.lenient().when(mockEntityInfo.getPriorVersion()).thenReturn(null);

    }

    @AfterEach
    void stopService() {
        manager.doStop();
    }

    @Test
    void construct() {
        assertDoesNotThrow(() -> {
            manager.doStart();
            manager.doStop();
        });
    }

    @Test
    void startFail_loadPolicyError_strictStartupModeDisabled() {
        manager = new DynamicEntityManager(configStoreClient, updateEventLogger, false, operationalAuditor,
                properties, entityUpdateService, ClientEnvironment.QA);
        when(configStoreClient.getEntityInfo(any())).thenReturn(Flux.just(mockEntityInfo).cast(EntityInfo.class));
        when(configStoreClient.getEntity(any())).thenThrow(new RuntimeException("test"));

        assertDoesNotThrow(() -> manager.doStart());
        assertTrue(manager.isAccessControlLoadComplete());

    }

    @Test
    void startFail_loadPolicyError_strictStartupMode() {
        manager = new DynamicEntityManager(configStoreClient, updateEventLogger, true, operationalAuditor,
                properties, entityUpdateService, ClientEnvironment.QA);
        when(configStoreClient.getEntityInfo(any())).thenReturn(Flux.just(mockEntityInfo).cast(EntityInfo.class));
        when(configStoreClient.getEntity(any())).thenThrow(new RuntimeException("test"));

        assertThrows(IllegalStateException.class, () -> manager.doStart());
        assertFalse(manager.isAccessControlLoadComplete());

    }

    @Test
    void startFail_loadPolicyError_strictStartupModeEntityLoad() {
        manager = new DynamicEntityManager(configStoreClient, updateEventLogger, true, operationalAuditor,
                properties, entityUpdateService, ClientEnvironment.QA);
        when(configStoreClient.getEntityInfo(any())).thenReturn(Flux.just(mockEntityInfo).cast(EntityInfo.class));
        when(configStoreClient.getEntity(any())).thenReturn(mockEntity);

        when(policyAccessRegistry.setPolicyAccess(any())).thenThrow(new RuntimeException("test"));

        assertThrows(IllegalStateException.class, () -> manager.doStart());
        assertFalse(manager.isAccessControlLoadComplete());

    }

    @Test
    void startFail_loadPolicyFailure() {
        manager = new DynamicEntityManager(configStoreClient, updateEventLogger, true, operationalAuditor,
                properties, entityUpdateService, ClientEnvironment.QA);
        when(configStoreClient.getEntityInfo(any())).thenReturn(Flux.error(new RuntimeException("test")));

        assertThrows(RuntimeException.class, () -> manager.doStart());
        assertFalse(manager.isAccessControlLoadComplete());
    }

    @Test
    void stream_termination_quiescent() {
        when(configStoreClient.getEntityUpdatesBatch(any(), any())).thenReturn(Flux.never());
        manager.doStart();
        manager.doStop();

        // check that stream termination events are not observed under normal stream lifecycle
        verify(updateEventLogger, times(0)).auditTermination(any());

    }

    @Test
    void stream_termination_withError() {
        String errorId = UUID.randomUUID().toString();
        RuntimeException error = new RuntimeException(errorId);
        when(configStoreClient.getEntityUpdatesBatch(any(), any())).thenReturn(Flux.error(error));
        assertDoesNotThrow(() -> manager.doStart());
        verify(updateEventLogger, times(1)).auditTermination(any());

    }

    @Test
    void stream_termination_withNoError() {
        when(configStoreClient.getEntityUpdatesBatch(any(), any())).thenReturn(Flux.empty());

        assertDoesNotThrow(() -> manager.doStart());
        verify(updateEventLogger, times(1)).auditTermination(null);
    }

    @Test
    void delta_scan_success() {

        EntityState.Delta<EntityInfo> delta = getTestChangeObject(EntityState.Delta.ChangeType.ADD);
        when(configStoreClient.getEntityUpdatesBatch(eq(Collections.emptyList()), any())).thenReturn(Flux.just(Collections.singletonList(delta)));

        assertDoesNotThrow(() -> manager.doStart());

    }

    @Test
    void handlePolicyLoadOperations() {
        EntityInfo.Access mockEntityInfo = Mockito.mock(EntityInfo.Access.class);
        Entity.Access mockEntity = Mockito.mock(Entity.Access.class);
        when(configStoreClient.getEntity(mockEntityInfo)).thenReturn(mockEntity);

        // ADD
        manager.tryLoadBatchedEntities(Collections.singletonList(EntityState.Delta.add(mockEntityInfo)));
        verify(updateEventLogger, times(1))
                .auditUpdate(EntityState.Delta.ChangeType.ADD, mockEntityInfo, EntityActivationStatus.ACTIVE);
        verifyEntityUpdatedOperationalAudit(EntityState.Delta.ChangeType.ADD, UpdateTransactionResult.success());
        verify(policyAccessRegistry, times(1))
                .setPolicyAccess(any());

        // UPDATE
        reset(policyAccessRegistry);
        when(mockEntityInfo.getVersion()).thenReturn("abc");
        manager.tryLoadBatchedEntities(Collections.singletonList(EntityState.Delta.update(mockEntityInfo)));
        verify(updateEventLogger, times(1))
                .auditUpdate(EntityState.Delta.ChangeType.UPDATE, mockEntityInfo, EntityActivationStatus.ACTIVE);
        verifyEntityUpdatedOperationalAudit(EntityState.Delta.ChangeType.UPDATE, UpdateTransactionResult.success());
        verify(policyAccessRegistry, times(1))
                .setPolicyAccess(any());

        // DELETE
        when(mockEntityInfo.getPolicyShortName()).thenReturn("a");
        when(mockEntityInfo.getPolicyMajorVersion()).thenReturn(1);
        manager.tryLoadBatchedEntities(Collections.singletonList(EntityState.Delta.delete(mockEntityInfo)));
        verify(updateEventLogger, times(1))
                .auditUpdate(EntityState.Delta.ChangeType.DELETE, mockEntityInfo, EntityActivationStatus.ACTIVE);
        verifyEntityUpdatedOperationalAudit(EntityState.Delta.ChangeType.DELETE, UpdateTransactionResult.success());
        verify(policyAccessRegistry, times(1))
                .removePolicyAccess("a", 1);
    }

    private void verifyEntityUpdatedOperationalAudit(final EntityState.Delta.ChangeType expectedChangeType,
                                                     final UpdateTransactionResult expectedResult) {
        verify(operationalAuditor, Mockito.atLeast(1)).audit(operationalEventOccurredArgumentCaptor.capture(),
                eq(EntityUpdateEventMapper.Factory.class));
        final OperationalEventOccurred actualEventOccurred = operationalEventOccurredArgumentCaptor.getValue();
        Assertions.assertInstanceOf(EntityUpdateEventOccurred.class, actualEventOccurred);
        final EntityUpdateEventOccurred actualEntityUpdateEventOccurred = (EntityUpdateEventOccurred) actualEventOccurred;
        Assertions.assertEquals(expectedChangeType, actualEntityUpdateEventOccurred.getChangeType());
        Assertions.assertEquals(expectedResult.getStatus(), actualEntityUpdateEventOccurred.getResult().getStatus());
        Assertions.assertEquals(expectedResult.getError(), actualEntityUpdateEventOccurred.getResult().getError());
        Assertions.assertNotNull(actualEntityUpdateEventOccurred.getInfo());
        Assertions.assertNotNull(actualEntityUpdateEventOccurred.getStartTimestamp());
        Assertions.assertNotNull(actualEntityUpdateEventOccurred.getEndTimestamp());
    }

}

