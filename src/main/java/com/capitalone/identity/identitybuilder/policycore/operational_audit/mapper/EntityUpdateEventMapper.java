package com.capitalone.identity.identitybuilder.policycore.operational_audit.mapper;

import com.capitalone.identity.identitybuilder.model.*;
import com.capitalone.identity.identitybuilder.policycore.camel.external.dynamic.UpdateTransactionResult;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.model.*;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.model.configstore.EntityUpdateEventOccurred;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.model.configstore.OperationalConfigStoreMetadata;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.util.OperationalAuditConstants;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.extern.log4j.Log4j2;

import java.util.Optional;
import java.util.Set;

@AllArgsConstructor
@Log4j2
public class EntityUpdateEventMapper implements OperationalEventMapper {

    private final @NonNull EntityUpdateEventOccurred entityUpdateEventOccurred;

    @Override
    public OperationalEvent buildWithEntitySpecificAttributes() {

        // Initialize builders
        final OperationalEvent.OperationalEventBuilder operationalEventBuilder = OperationalEvent.builder();
        final OperationalConfigStoreMetadata.OperationalConfigStoreMetadataBuilder configStoreMetadataBuilder =
                OperationalConfigStoreMetadata.builder();

        // Populate event metadata
        final EntityInfo entityInfo = entityUpdateEventOccurred.getInfo();
        final EntityState.Delta.ChangeType changeType = entityUpdateEventOccurred.getChangeType();
        final Optional<Set<ConfigStoreItemInfo>> configStoreItemInfoOptional = Optional.ofNullable(entityInfo.getItemInfo());
        final Optional<EntityActivationStatus> activationStatus = Optional.ofNullable(entityUpdateEventOccurred.getActivationStatus());

        // Populate necessary generic event fields
        operationalEventBuilder.eventEntityName(entityInfo.getId());

        // Populate necessary event-specific fields
        configStoreMetadataBuilder.configStoreMetadataVersionNumber(Integer.toString(entityInfo.getPatchVersion()));
        configStoreMetadataBuilder.configStoreMetadataDeploymentStatus(activationStatus
                .map(EntityActivationStatus::toString).orElse(null));
        configStoreMetadataBuilder.changeType(changeType.name());
        configStoreMetadataBuilder.entityType(Optional.ofNullable(entityInfo.getType())
                .map(EntityType::name).orElse(null));
        configStoreMetadataBuilder.entityId(entityInfo.getId());
        configStoreMetadataBuilder.entityVersion(entityInfo.getVersion());
        configStoreMetadataBuilder.entityItems(configStoreItemInfoOptional.map(Set::toString).orElse(null));
        configStoreMetadataBuilder.itemsCount(configStoreItemInfoOptional.map(Set::size)
                .map(String::valueOf).orElse(null));
        configStoreMetadataBuilder.ignoredNonPolicyItems(Optional.ofNullable(entityInfo.getFilteredItemNames())
                .map(Set::toString).orElse(null));

        return operationalEventBuilder
                .configStoreMetadata(configStoreMetadataBuilder.build())
                .build();
    }

    @Override
    public OperationalEventOutcome toOperationalEventOutcome() {
        final UpdateTransactionResult updateTransactionResult = entityUpdateEventOccurred.getResult();
        switch (updateTransactionResult.getStatus()) {
            case SUCCESS:
                return OperationalAuditConstants.OUTCOME_SUCCESS_INFO;
            case ERROR:
                return OperationalAuditConstants.OUTCOME_FAILURE_ERROR;
            default:
                log.error("Attempt to audit invalid config store load event status: {}",
                        updateTransactionResult.getStatus());
                return OperationalEventOutcome.of(OperationalStatus.INVALID, OperationalAuditConstants.UNDEFINED);
        }
    }

    @Override
    public Optional<OperationalError> toOperationalError() {
        final Throwable throwable = entityUpdateEventOccurred.getResult().getError();
        return Optional.ofNullable(OperationalError.of(throwable));
    }

    public static class Factory implements OperationalEventMapper.NotificationEventFactory {

        @NonNull @Override
        public OperationalEventMapper create(final OperationalEventOccurred operationalEventOccurred) {
            return new EntityUpdateEventMapper((EntityUpdateEventOccurred) operationalEventOccurred);
        }
    }
}