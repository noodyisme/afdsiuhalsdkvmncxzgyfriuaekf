package com.capitalone.identity.identitybuilder.policycore.externalization;

import com.capitalone.identity.identitybuilder.ClientEnvironment;
import com.capitalone.identity.identitybuilder.model.Entity;
import com.capitalone.identity.identitybuilder.model.EntityInfo;
import com.capitalone.identity.identitybuilder.policycore.abac.config.PolicyAccessRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccessControlEntityUpdaterTest {
    @Mock
    PolicyAccessRegistry registry;

    @Test
    void constructThrowsNPE() {
        assertThrows(NullPointerException.class,
                () -> new AccessControlEntityUpdater(null, ClientEnvironment.PROD));

        assertDoesNotThrow(() -> new AccessControlEntityUpdater(registry, null));
    }

    @Test
    void changeMethodCalls() {
        AccessControlEntityUpdater updater = new AccessControlEntityUpdater(registry, ClientEnvironment.PROD);
        EntityInfo.Access info = Mockito.mock(EntityInfo.Access.class);
        Entity.Access entity = Mockito.mock(Entity.Access.class);

        assertDoesNotThrow(() -> updater.add(entity));
        assertDoesNotThrow(() -> updater.update(entity));
        assertDoesNotThrow(() -> updater.delete(info));
    }

    @Test
    void checkUpdateAllowedOrThrow_happy() {
        AccessControlEntityUpdater updater = new AccessControlEntityUpdater(registry, ClientEnvironment.PROD);
        EntityInfo.Access prev = Mockito.mock(EntityInfo.Access.class);
        when(prev.getPatchVersion()).thenReturn(1);
        when(prev.getVersion()).thenReturn("a");
        EntityInfo.Access next = Mockito.mock(EntityInfo.Access.class);
        when(next.getPatchVersion()).thenReturn(2);
        when(next.getVersion()).thenReturn("b");

        assertDoesNotThrow(() -> updater.checkUpdateAllowedOrThrow(prev, next));
    }

    @Test
    void checkUpdateAllowedOrThrow_sad() {
        AccessControlEntityUpdater updater = new AccessControlEntityUpdater(registry, ClientEnvironment.PROD);
        EntityInfo.Access prev = Mockito.mock(EntityInfo.Access.class);
        when(prev.getPatchVersion()).thenReturn(1);
        when(prev.getVersion()).thenReturn("a");
        EntityInfo.Access next = Mockito.mock(EntityInfo.Access.class);
        when(next.getPatchVersion()).thenReturn(1);
        when(next.getVersion()).thenReturn("b");

        assertThrows(AccessControlEntityUpdater.IllegalProdAccessUpdateException.class,
                () -> updater.checkUpdateAllowedOrThrow(prev, next));
    }
}
