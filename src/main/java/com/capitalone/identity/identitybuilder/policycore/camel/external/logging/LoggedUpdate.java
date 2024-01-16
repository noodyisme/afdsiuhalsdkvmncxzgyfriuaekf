package com.capitalone.identity.identitybuilder.policycore.camel.external.logging;

import com.capitalone.identity.identitybuilder.model.EntityActivationStatus;
import com.capitalone.identity.identitybuilder.model.EntityInfo;
import com.capitalone.identity.identitybuilder.model.EntityState;
import com.capitalone.identity.identitybuilder.policycore.camel.external.dynamic.UpdateTransactionResult;

import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.Value;

/**
 * see {@link RuntimeUpdateEventLogger}
 */

public class LoggedUpdate {

    private LoggedUpdate() {
    }

    static LoggedUpdate newFromLoad(@NonNull EntityState.Delta.ChangeType type,
                                    @NonNull EntityInfo info,
                                    @NonNull EntityActivationStatus activationStatus) {
        return new LoggedUpdate.Load(
                LoggedEntityInfo.newFromInfo(info), type, LoggedResult.newFromTransaction(UpdateTransactionResult.success()),
                activationStatus);
    }

    static LoggedUpdate newFromUnload(@NonNull EntityInfo info) {
        return new LoggedUpdate.Unload(LoggedEntityInfo.newFromInfo(info),
                LoggedResult.newFromTransaction(UpdateTransactionResult.success()));
    }

    static LoggedUpdate newFromError(@NonNull EntityInfo info, @NonNull EntityState.Delta.ChangeType type,
                                     @NonNull Throwable error) {
        return new LoggedUpdate.Error(
                LoggedEntityInfo.newFromInfo(info),
                type,
                LoggedResult.newFromTransaction(UpdateTransactionResult.error(error)));
    }


    @Value
    @EqualsAndHashCode(callSuper = false)
    public static class Load extends LoggedUpdate {
        @NonNull LoggedEntityInfo entity;
        @NonNull EntityState.Delta.ChangeType type;
        @NonNull LoggedResult result;
        @NonNull EntityActivationStatus activationStatus;
    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    public static class Unload extends LoggedUpdate {
        @NonNull LoggedEntityInfo entity;
        EntityState.Delta.ChangeType type = EntityState.Delta.ChangeType.DELETE;
        @NonNull LoggedResult result;
    }

    @Value
    @EqualsAndHashCode(callSuper = false)
    public static class Error extends LoggedUpdate {
        @NonNull LoggedEntityInfo entity;
        @NonNull EntityState.Delta.ChangeType type;
        @NonNull LoggedResult result;
    }


}
