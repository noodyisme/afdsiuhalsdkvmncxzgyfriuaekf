package com.capitalone.identity.identitybuilder.policycore.operational_audit.model.configstore;

import com.capitalone.identity.identitybuilder.model.EntityActivationStatus;
import com.capitalone.identity.identitybuilder.model.EntityInfo;
import com.capitalone.identity.identitybuilder.model.EntityState;
import com.capitalone.identity.identitybuilder.policycore.camel.external.dynamic.UpdateTransactionResult;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.model.OperationalEventOccurred;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.model.type.OperationalEventType;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import org.springframework.lang.Nullable;

import java.util.Optional;

/**
 * Describes an operational event involving config store entity updates
 */
@Value
@Builder
public class EntityUpdateEventOccurred implements OperationalEventOccurred {
    @NonNull OperationalEventType operationalEventType;
    @NonNull EntityState.Delta.ChangeType changeType;
    @NonNull EntityInfo info;
    @NonNull UpdateTransactionResult result;
    @NonNull Long startTimestamp;
    @NonNull Long endTimestamp;
    @Nullable EntityActivationStatus activationStatus;
}
