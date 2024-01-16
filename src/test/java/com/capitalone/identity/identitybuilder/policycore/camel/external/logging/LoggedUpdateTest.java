package com.capitalone.identity.identitybuilder.policycore.camel.external.logging;

import com.capitalone.identity.identitybuilder.model.EntityActivationStatus;
import com.capitalone.identity.identitybuilder.model.EntityInfo;
import com.capitalone.identity.identitybuilder.model.EntityState;
import com.capitalone.identity.identitybuilder.model.EntityType;
import com.capitalone.identity.identitybuilder.policycore.camel.external.dynamic.MockEntityInfo;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LoggedUpdateTest {

    @Test
    @SuppressWarnings("ConstantConditions")
    void newFromLoad() {
        EntityInfo info = new MockEntityInfo("a/b/c", EntityType.POLICY);
        EntityState.Delta.ChangeType changeType = EntityState.Delta.ChangeType.UPDATE;
        EntityActivationStatus status = EntityActivationStatus.ACTIVE;
        assertDoesNotThrow(() -> LoggedUpdate.newFromLoad(changeType, info, status));
        assertThrows(NullPointerException.class, () -> LoggedUpdate.newFromLoad(null, info, status));
        assertThrows(NullPointerException.class, () -> LoggedUpdate.newFromLoad(changeType, null, status));
        assertThrows(NullPointerException.class, () -> LoggedUpdate.newFromLoad(changeType, info, null));
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    void newFromUnload() {
        EntityInfo info = new MockEntityInfo("a/b/c", EntityType.POLICY);
        assertDoesNotThrow(() -> LoggedUpdate.newFromUnload(info));
        assertThrows(NullPointerException.class, () -> LoggedUpdate.newFromUnload(null));
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    void newFromError() {
        EntityInfo info = new MockEntityInfo("a/b/c", EntityType.POLICY);
        EntityState.Delta.ChangeType changeType = EntityState.Delta.ChangeType.UPDATE;
        Throwable error = new RuntimeException("test");
        assertDoesNotThrow(() -> LoggedUpdate.newFromUnload(info));
        assertThrows(NullPointerException.class, () -> LoggedUpdate.newFromError(null, changeType, error));
        assertThrows(NullPointerException.class, () -> LoggedUpdate.newFromError(info, null, error));
        assertThrows(NullPointerException.class, () -> LoggedUpdate.newFromError(info, changeType, null));
    }
}