package com.capitalone.identity.identitybuilder.policycore.camel.external.dynamic;

import com.capitalone.identity.identitybuilder.ClientEnvironment;
import com.capitalone.identity.identitybuilder.client.ConfigStoreClient;
import com.capitalone.identity.identitybuilder.model.*;
import com.capitalone.identity.identitybuilder.model.parsing.PolicyDefinition;
import com.capitalone.identity.identitybuilder.policycore.externalization.events.EntityLoadEvents;
import com.capitalone.identity.identitybuilder.policycore.externalization.logging.DynamicLoadingEventLogger;
import org.apache.camel.CamelContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;

import java.io.IOError;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DynamicPolicyManagerTest {

    private static final String childItemName = "test_policy/1.0/process/test_policy.xml";
    private static final String childItemTag = "tagA";
    @Mock
    DynamicPolicyUpdater policyUpdater;
    @Mock
    ConfigStoreClient configStoreClient;
    @Mock
    CamelContext camelContext;
    @Mock
    Entity mockEntity;
    @Mock
    EntityInfo mockEntityInfo;
    @Mock
    DynamicLoadingEventLogger dynamicLoadingEventLogger;
    @Mock
    PolicyValidator policyValidator;
    DynamicPolicyManager manager;

    private static EntityState.Delta<EntityInfo> getTestChangeObject(EntityState.Delta.ChangeType changeType) {
        return getTestChangeObject(changeType, childItemTag);
    }

    private static EntityState.Delta<EntityInfo> getTestChangeObject(EntityState.Delta.ChangeType changeType, String tag) {
        ConfigStoreItemInfo item = new ConfigStoreItemInfo(childItemName, tag);
        EntityInfo.Policy mockEntityInfoPolicy = new EntityInfo.Policy(
                new PolicyDefinition("test_policy/1.0", "test_policy", "test_policy", 1, 0, 0),
                new HashSet<>(Collections.singleton(item)));
        return new EntityState.Delta<>(changeType, mockEntityInfoPolicy);
    }

    @BeforeEach
    void createResult() {
        manager = new DynamicPolicyManager(configStoreClient, policyUpdater, policyValidator, false, ClientEnvironment.QA, dynamicLoadingEventLogger);
        lenient().when(configStoreClient.getEntityInfo(any())).thenReturn(Flux.empty());
        lenient().when(configStoreClient.getEntityInfo(EntityType.PIP, EntityType.POLICY)).thenReturn(Flux.empty());
    }

    @Test
    void construct() {
        assertDoesNotThrow(() -> {
            manager.beforeApplicationStart(camelContext);
            manager.doStart();
            manager.afterApplicationStart(camelContext);
            manager.doStop();
        });
    }

    @Test
    void startFail_camelServiceAdd() throws Exception {
        doThrow(new RuntimeException("test"))
                .when(camelContext).addService(any(Object.class), anyBoolean(), anyBoolean());

        assertThrows(IOError.class, () -> manager.beforeApplicationStart(camelContext));
    }

    @Test
    void startFail_loadPolicyError_lenientStartupMode() {
        when(configStoreClient.getEntityInfo(any(), any())).thenReturn(Flux.just(mockEntityInfo));

        doThrow(new RuntimeException("test")).when(policyUpdater).set(any());

        manager.beforeApplicationStart(camelContext);
        assertDoesNotThrow(() -> manager.afterApplicationStart(camelContext));
        assertTrue(manager.isExternalLoadComplete());

        verify(dynamicLoadingEventLogger)
                .onEntityLoadFailed(argThat(loaded -> loaded.equals(
                        new EntityLoadEvents.Failed(mockEntityInfo, EntityState.Delta.ChangeType.ADD, loaded.getError(), loaded.getStartTime(), loaded.getEndTime(), true)
                )));

    }

    @Test
    void startFail_loadPolicyError_strictStartupMode() {
        manager = new DynamicPolicyManager(configStoreClient, policyUpdater, policyValidator,
                true, ClientEnvironment.PROD, dynamicLoadingEventLogger);
        when(configStoreClient.getEntityInfo(any(), any())).thenReturn(Flux.just(mockEntityInfo));
        when(configStoreClient.getEntity(mockEntityInfo)).thenThrow(new RuntimeException("test"));

        assertDoesNotThrow(() -> manager.beforeApplicationStart(camelContext));
        assertThrows(IllegalStateException.class, () -> manager.afterApplicationStart(camelContext));
        assertFalse(manager.isExternalLoadComplete());

    }

    @Test
    void startFail_loadPolicyError_strictStartupModeEntityLoad() {
        manager = new DynamicPolicyManager(configStoreClient, policyUpdater, policyValidator,
                true, ClientEnvironment.PROD, dynamicLoadingEventLogger);

        when(configStoreClient.getEntityInfo(any(), any())).thenReturn(Flux.just(mockEntityInfo));
        when(configStoreClient.getEntity(mockEntityInfo)).thenReturn(mockEntity);

        doThrow(new RuntimeException("test")).when(policyUpdater).set(any());

        assertDoesNotThrow(() -> manager.beforeApplicationStart(camelContext));
        assertThrows(IllegalStateException.class, () -> manager.afterApplicationStart(camelContext));
        assertFalse(manager.isExternalLoadComplete());

    }

    @Test
    void startFail_loadPolicyFailure() {
        // Permit startup only in case of policy load exceptions that are handled
        // External load is not complete if there are unhandled exceptions on load.
        when(configStoreClient.getEntityInfo(any(), any())).thenThrow(new RuntimeException("test"));

        assertDoesNotThrow(() -> manager.beforeApplicationStart(camelContext));
        assertThrows(RuntimeException.class, () -> manager.afterApplicationStart(camelContext));
        assertFalse(manager.isExternalLoadComplete());

    }

    @Test
    void stream_termination_quiescent() {
        when(configStoreClient.getEntityUpdatesBatch(any(), any(), any())).thenReturn(Flux.never());
        manager.beforeApplicationStart(camelContext);
        manager.doStart();
        manager.afterApplicationStart(camelContext);
        manager.doStop();

        // check that stream termination events are not observed under normal stream lifecycle
        verify(dynamicLoadingEventLogger, times(0)).onNonEntityError(any());

    }

    @Test
    void stream_termination_withError() {
        String errorId = UUID.randomUUID().toString();
        RuntimeException error = new RuntimeException(errorId);
        when(configStoreClient.getEntityUpdatesBatch(any(), any(), any())).thenReturn(Flux.error(error));

        assertDoesNotThrow(() -> manager.beforeApplicationStart(camelContext));
        assertDoesNotThrow(() -> manager.afterApplicationStart(camelContext));

        // startup works fine
        // verify stream terminates in expected way
        assertTrue(manager.isExternalLoadComplete());

        // runtime update stream terminated w/ informative error
        verify(dynamicLoadingEventLogger, timeout(2000).times(2))
                .onNonEntityError(new EntityLoadEvents.NonLoadingError(error));
    }

    @Test
    void stream_termination_withNoError() {
        when(configStoreClient.getEntityUpdatesBatch(any(), any(), any())).thenReturn(Flux.empty());

        manager.beforeApplicationStart(camelContext);
        manager.afterApplicationStart(camelContext);
        verify(dynamicLoadingEventLogger, times(1)).onNonEntityError(new EntityLoadEvents.NonLoadingError(null));
        assertDoesNotThrow(() -> manager.afterApplicationStart(camelContext));
    }

    @Test
    void delta_scan_success() {

        EntityState.Delta<EntityInfo> delta = getTestChangeObject(EntityState.Delta.ChangeType.ADD);
        when(configStoreClient.getEntityUpdatesBatch(any(), any(), any())).thenReturn(Flux.just(Collections.singletonList(delta)));

        Entity.Policy entity = mock(Entity.Policy.class);
        when(configStoreClient.getEntity(delta.getEntityInfo())).thenReturn(entity);

        manager.beforeApplicationStart(camelContext);

        assertDoesNotThrow(() -> manager.afterApplicationStart(camelContext));

        // verify happy path of delta stream
        verify(policyUpdater, times(1)).set(any());
        verify(dynamicLoadingEventLogger)
                .onEntityLoaded(argThat(loaded -> loaded.equals(
                        new EntityLoadEvents.Loaded(entity, EntityState.Delta.ChangeType.ADD, loaded.getStartTime(), loaded.getEndTime(), false)
                )));
    }

    @Test
    void delta_scan_update_success() {

        EntityState.Delta<EntityInfo> deltaV1 = getTestChangeObject(EntityState.Delta.ChangeType.ADD, "a");
        final Entity.Policy entityV1 = mock(Entity.Policy.class);
        when(configStoreClient.getEntity(deltaV1.getEntityInfo())).thenReturn(entityV1);

        EntityState.Delta<EntityInfo> deltaV2 = getTestChangeObject(EntityState.Delta.ChangeType.UPDATE, "b");
        final Entity.Policy entityV2 = mock(Entity.Policy.class);
        lenient().when(entityV2.getInfo()).thenReturn((EntityInfo.Policy) deltaV2.getEntityInfo());
        when(configStoreClient.getEntity(deltaV2.getEntityInfo())).thenReturn(entityV2);
        when(configStoreClient.getEntityUpdatesBatch(any(), any(), any())).thenReturn(Flux.just(Arrays.asList(deltaV1, deltaV2)));

        manager.beforeApplicationStart(camelContext);

        assertDoesNotThrow(() -> manager.afterApplicationStart(camelContext));

        // verify happy path of delta stream
        InOrder inOrder = inOrder(policyUpdater, policyUpdater);
        inOrder.verify(policyUpdater).set(entityV1);
        inOrder.verify(policyUpdater).set(entityV2);

        // update logger
        InOrder inOrderLog = inOrder(dynamicLoadingEventLogger, dynamicLoadingEventLogger);
        inOrderLog.verify(dynamicLoadingEventLogger).onEntityLoaded(argThat(loaded -> loaded.equals(
                new EntityLoadEvents.Loaded(entityV1, EntityState.Delta.ChangeType.ADD, loaded.getStartTime(), loaded.getEndTime(), false)
        )));
        inOrderLog.verify(dynamicLoadingEventLogger).onEntityLoaded(argThat(loaded -> loaded.equals(
                new EntityLoadEvents.Loaded(entityV2, EntityState.Delta.ChangeType.UPDATE, loaded.getStartTime(), loaded.getEndTime(), false)
        )));
    }

    @Test
    void delta_scan_update_error() {

        EntityState.Delta<EntityInfo> deltaV1 = getTestChangeObject(EntityState.Delta.ChangeType.UPDATE);
        Entity.Policy entityV1 = mock(Entity.Policy.class);
        when(configStoreClient.getEntity(deltaV1.getEntityInfo())).thenReturn(entityV1);

        when(configStoreClient.getEntityUpdatesBatch(any(), any(), any())).thenReturn(Flux.just(Collections.singletonList(deltaV1)));

        manager.beforeApplicationStart(camelContext);

        RuntimeException expectLoadError = new RuntimeException("test");

        doThrow(expectLoadError).when(policyUpdater).set(any());

        assertDoesNotThrow(() -> manager.afterApplicationStart(camelContext));

        // verify behavior during an update error (stream should terminate)
        verify(policyUpdater, times(1)).set(entityV1);

        verify(dynamicLoadingEventLogger, times(1)).onEntityLoadFailed(argThat(loaded -> loaded.equals(
                new EntityLoadEvents.Failed(deltaV1.getEntityInfo(), EntityState.Delta.ChangeType.UPDATE, expectLoadError, loaded.getStartTime(), loaded.getEndTime(), false)
        )));
    }

    @Test
    void delta_scan_delete_success() {

        EntityState.Delta<EntityInfo> delta = getTestChangeObject(EntityState.Delta.ChangeType.DELETE);
        when(configStoreClient.getEntityUpdatesBatch(any(), any(), any())).thenReturn(Flux.just(Collections.singletonList(delta)));
        doNothing().when(policyUpdater).delete(any());

        manager.beforeApplicationStart(camelContext);

        assertDoesNotThrow(() -> manager.afterApplicationStart(camelContext));

        // verify happy path of delta stream
        verify(policyUpdater, times(1)).delete(any());
        verify(dynamicLoadingEventLogger)
                .onEntityUnloaded(argThat(unloaded -> unloaded.equals(
                        new EntityLoadEvents.Unloaded(delta.getEntityInfo(), unloaded.getStartTime(), unloaded.getEndTime())
                )));

    }

}
