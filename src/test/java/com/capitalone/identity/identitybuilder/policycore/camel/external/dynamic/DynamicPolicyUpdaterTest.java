package com.capitalone.identity.identitybuilder.policycore.camel.external.dynamic;

import com.capitalone.identity.identitybuilder.model.Entity;
import com.capitalone.identity.identitybuilder.model.EntityActivationStatus;
import com.capitalone.identity.identitybuilder.model.EntityInfo;
import com.capitalone.identity.identitybuilder.model.PolicyInfo;
import com.capitalone.identity.identitybuilder.model.parsing.PolicyDefinition;
import com.capitalone.identity.identitybuilder.policycore.service.versions.PolicyVersionService;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DynamicPolicyUpdaterTest {

    DynamicPolicyUpdater updater;

    @Mock
    DynamicPolicyLoader loader;

    @Mock
    EntityUpdateRegulator regulator;

    @Mock
    PolicyVersionService policyVersionService;

    @BeforeEach
    void setUp() {
        updater = new DynamicPolicyUpdater(loader, regulator, policyVersionService);
    }

    private Entity setUpExistingPolicy(String name, String version, EntityActivationStatus activationStatus) {
        Entity mockEntity = getMockPolicyEntity(name, version, activationStatus);
        lenient().when(loader.add(mockEntity)).thenReturn(UpdateTransactionResult.success());
        updater.add(mockEntity);
        clearInvocations(loader);
        clearInvocations(policyVersionService);
        return mockEntity;
    }

    @NotNull
    private Entity getMockPolicyEntity(String name, String version, EntityActivationStatus activationStatus) {
        return getMockPolicyEntity(name, version, 0, activationStatus);
    }

    @NotNull
    private Entity getMockPolicyEntity(String name, String version, int versionNumber, EntityActivationStatus activationStatus) {
        EntityInfo.Policy init = new EntityInfo.Policy(
                new PolicyDefinition(name, version +"."+ versionNumber), Collections.emptySet());

        EntityInfo.Policy info = spy(init);
        lenient().when(info.getId()).thenReturn(name);
        lenient().when(info.getVersion()).thenReturn(version);
        lenient().when(info.getPatchVersion()).thenReturn(versionNumber);
        return new Entity.Policy(info, Collections.emptySet(), activationStatus);
    }

    @ParameterizedTest
    @EnumSource(names = {"AVAILABLE", "ACTIVE"})
    void performAdd_normal(EntityActivationStatus status) {
        UpdateTransactionResult txSuccess = UpdateTransactionResult.success();
        when(loader.add(any())).thenReturn(txSuccess);

        Entity mockEntity = getMockPolicyEntity("test", "1", status);
        updater.add(mockEntity);

        verify(loader).add(any());
        verify(policyVersionService).set((PolicyInfo.Patch) mockEntity.getInfo(), status);
    }

    @ParameterizedTest
    @EnumSource(names = {"DISABLED"})
    void performAdd_disabledPolicyIsNotLoaded(EntityActivationStatus status) {
        Entity mockEntity = getMockPolicyEntity("test", "1", status);
        updater.add(mockEntity);

        // load operation is not attempted
        verifyNoInteractions(loader);
        // version service should still be notified
        verify(policyVersionService).set((PolicyInfo.Patch) mockEntity.getInfo(), status);
    }

    @Test
    void performAdd_error() {
        UpdateTransactionResult txRolledBack = UpdateTransactionResult.error(new RuntimeException("test"));
        when(loader.add(any())).thenReturn(txRolledBack);

        Entity mockEntity = getMockPolicyEntity("b", "1", EntityActivationStatus.AVAILABLE);
        assertThrows(RuntimeException.class, () -> updater.add(mockEntity));

        verify(loader).add(any());
        verifyNoInteractions(policyVersionService);
    }

    @Test
    void performUpdate_normal() {
        Entity previous = setUpExistingPolicy("test", "1", EntityActivationStatus.AVAILABLE);
        UpdateTransactionResult txSuccess = UpdateTransactionResult.success();
        when(loader.delete(any())).thenReturn(txSuccess);
        when(loader.add(any())).thenReturn(txSuccess);

        Entity update = getMockPolicyEntity("test", "2", EntityActivationStatus.AVAILABLE);
        assertDoesNotThrow(() -> updater.update(update, previous.getInfo()));

        verify(regulator).checkUpdateAllowedOrThrow(any(), any());
        verify(loader).delete(previous.getInfo());
        verify(loader).add(update);
        verify(policyVersionService).set((PolicyInfo.Patch) update.getInfo(), EntityActivationStatus.AVAILABLE);
    }

    @Test
    void performUpdate_error() {
        Entity previous = setUpExistingPolicy("test", "1", EntityActivationStatus.AVAILABLE);
        RuntimeException error = new RuntimeException("test");
        when(loader.delete(any())).thenReturn(UpdateTransactionResult.success());
        when(loader.add(any())).thenReturn(UpdateTransactionResult.error(error));

        Entity update = getMockPolicyEntity("test", "2", EntityActivationStatus.AVAILABLE);
        EntityInfo prevInfo = previous.getInfo();
        assertThrows(RuntimeException.class, () -> updater.update(update, prevInfo));

        verify(regulator).checkUpdateAllowedOrThrow(any(), any());
        verify(loader).delete(prevInfo);
        verify(loader).add(update);
        verify(policyVersionService).remove(any());
    }

    @Test
    void performUpdate_noUnsafeLoadInteractionsWhenPolicyUpdatedToDisabledState() {
        // when policy is updated to disabled state, the effect is the same as delete.
        Entity previousEntity = setUpExistingPolicy("test", "1", EntityActivationStatus.AVAILABLE);

        Entity update = getMockPolicyEntity("test", "1", 1, EntityActivationStatus.DISABLED);
        assertDoesNotThrow(() -> updater.update(update, previousEntity.getInfo()));

        verify(regulator).checkUpdateAllowedOrThrow(any(), any());
        // update to disabled state, if successful, should clean up the old policy from camel
        verify(loader).delete(previousEntity.getInfo());
        // version service should be notified
        verify(policyVersionService).set((PolicyInfo.Patch) update.getInfo(), EntityActivationStatus.DISABLED);
    }

    @Test
    void performUpdate_policyUpdatedToEnabledStateResultsInAddOperation() {
        // when policy is updated to disabled state, the effect is the same as delete.
        final Entity previousEntity = getMockPolicyEntity("test", "1", EntityActivationStatus.DISABLED);
        updater.add(previousEntity);

        Entity update = getMockPolicyEntity("test", "1", EntityActivationStatus.AVAILABLE);
        when(loader.add(any())).thenReturn(UpdateTransactionResult.success());
        assertDoesNotThrow(() -> updater.update(update, previousEntity.getInfo()));

        verify(regulator).checkUpdateAllowedOrThrow(any(), any());
        verify(loader).add(update);
        verify(policyVersionService).set((PolicyInfo.Patch) update.getInfo(), EntityActivationStatus.AVAILABLE);
    }

    @Test
    void performDelete() {
        Entity addedEntity = setUpExistingPolicy("test", "1", EntityActivationStatus.AVAILABLE);
        when(loader.delete(any())).thenReturn(UpdateTransactionResult.success());
        EntityInfo toDelete = addedEntity.getInfo();

        assertDoesNotThrow(() -> updater.delete(toDelete));

        verify(loader).delete(toDelete);
        verify(policyVersionService).remove((PolicyInfo.Patch) toDelete);

    }

    @Test
    void performDelete_ofDisabledPolicy() {
        EntityInfo toDelete = setUpExistingPolicy("test", "1", EntityActivationStatus.DISABLED).getInfo();

        assertDoesNotThrow(() -> updater.delete(toDelete));

        verifyNoInteractions(loader);
        verify(policyVersionService).remove((PolicyInfo.Patch) toDelete);

    }

    @Test
    void performDelete_error() {
        Entity addedEntity = setUpExistingPolicy("test", "1", EntityActivationStatus.AVAILABLE);
        RuntimeException error = new RuntimeException("test");
        when(loader.delete(any())).thenReturn(UpdateTransactionResult.error(error));
        EntityInfo toDelete = addedEntity.getInfo();

        RuntimeException actual = assertThrows(RuntimeException.class, () -> updater.delete(toDelete));
        assertEquals(error, actual.getCause());
        verify(loader).delete(toDelete);

    }

}
