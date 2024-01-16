package com.capitalone.identity.identitybuilder.policycore.operational_audit.mapper;

import com.capitalone.identity.identitybuilder.audit.util.JsonSerializationUtil;
import com.capitalone.identity.identitybuilder.model.DynamicUpdateProperties;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.model.*;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.model.configstore.OperationalConfigStoreMetadata;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.model.configstore.PollingConfigEvent;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.util.OperationalAuditConstants;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.Value;

import java.time.Duration;
import java.time.LocalTime;
import java.util.Optional;

@AllArgsConstructor
public class PollingConfigEventMapper implements OperationalEventMapper {

    private PollingConfigEvent pollingConfigEvent;

    @Override
    public OperationalEvent buildWithEntitySpecificAttributes() {
        final DynamicUpdateProperties dynamicUpdateProperties =
                pollingConfigEvent.getDynamicUpdateProperties();
        return OperationalEvent.builder().configStoreMetadata(
                OperationalConfigStoreMetadata.builder()
                        .newPollingConfig(JsonSerializationUtil.writeAsString(
                                new DynamicUpdatePropertiesAudit(
                                        dynamicUpdateProperties.getTimeOfDayUTC(),
                                        dynamicUpdateProperties.getInterval())))
                        .build())
                .build();
    }

    @Override
    public OperationalEventOutcome toOperationalEventOutcome() {
        if (pollingConfigEvent.getThrowable() == null) {
            return OperationalAuditConstants.OUTCOME_SUCCESS_INFO;
        }
        return OperationalAuditConstants.OUTCOME_FAILURE_ERROR;

    }

    @Override
    public Optional<OperationalError> toOperationalError() {
        return Optional.ofNullable(OperationalError.of(pollingConfigEvent.getThrowable()));
    }

    @Value
    private static class DynamicUpdatePropertiesAudit {
        LocalTime timeOfDayUTC;
        Duration interval;
    }

    public static class Factory implements OperationalEventMapper.NotificationEventFactory {
        @Override public @NonNull OperationalEventMapper create(
                final OperationalEventOccurred operationalEventOccurred) {
            return new PollingConfigEventMapper((PollingConfigEvent)operationalEventOccurred);
        }
    }

}
