package com.capitalone.identity.identitybuilder.policycore.operational_audit.mapper;

import com.capitalone.identity.identitybuilder.policycore.operational_audit.model.*;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.model.event_models.ApplicationBootstrapComplete;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.time.FastDateFormat;

import java.util.*;


@Log4j2
@AllArgsConstructor
public class ApplicationBootstrapCompleteMapper implements OperationalEventMapper {
    private static final FastDateFormat SIMPLE_LOGGING_DATE_FORMAT = FastDateFormat.getInstance("MM/dd/yyyy HH:mm:ss.SSS", TimeZone.getTimeZone("UTC"));

    private final Long systemBootstrapDuration;
    private final Long applicationAcceptingTrafficTime;
    HashMap<String, String> auditDurationMap;

    @Override
    public OperationalEvent buildWithEntitySpecificAttributes() {
        final OperationalEvent.OperationalEventBuilder operationalEventBuilder = OperationalEvent.builder();

        List<OperationalSupplementalAttribute> supplementalAttributeList = new ArrayList<>();
        for (Map.Entry<String, String> entry : auditDurationMap.entrySet()) {
            supplementalAttributeList.add(new OperationalSupplementalAttribute(entry.getKey(), entry.getValue()));
        }
        supplementalAttributeList.add(new OperationalSupplementalAttribute("time_to_healthy_duration_ms", systemBootstrapDuration.toString()));
        return operationalEventBuilder
                .supplementalMetadata(supplementalAttributeList)
                .additionalSupportingInformation(String.format("Spring Application able to receive traffic at: %s UTC",
                        SIMPLE_LOGGING_DATE_FORMAT.format(applicationAcceptingTrafficTime)))
                .build();
    }

    @Override
    public OperationalEventOutcome toOperationalEventOutcome() {
        return OperationalEventOutcome.of(OperationalStatus.SUCCESS, String.format("Application Bootstrap Duration: %s ms", systemBootstrapDuration));
    }

    @Override
    public Optional<OperationalError> toOperationalError() {
        return Optional.empty();
    }

    public static class Factory implements OperationalEventMapper.NotificationEventFactory {
        @Override
        public @NonNull OperationalEventMapper create(OperationalEventOccurred eventOccurred) {
            final long acceptingTrafficEpoch = ((ApplicationBootstrapComplete) eventOccurred).getApplicationAvailableTime();
            final long startTime = eventOccurred.getStartTimestamp();
            final long endTime = eventOccurred.getEndTimestamp();
            final long systemBootstrapDuration = endTime - startTime;

            final HashMap<String, String> durationMap = ((ApplicationBootstrapComplete) eventOccurred).getAuditEventDurations();


            return new ApplicationBootstrapCompleteMapper(systemBootstrapDuration, acceptingTrafficEpoch, durationMap);
        }
    }
}