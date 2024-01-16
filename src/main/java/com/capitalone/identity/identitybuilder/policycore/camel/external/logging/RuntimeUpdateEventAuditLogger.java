package com.capitalone.identity.identitybuilder.policycore.camel.external.logging;

import com.capitalone.chassis.cal1_5.builder.Cal1_5EventStreamDataBuilder;
import com.capitalone.chassis.engine.annotations.stream.EventStream;
import com.capitalone.identity.identitybuilder.client.DevLocalProperties;
import com.capitalone.identity.identitybuilder.model.EntityActivationStatus;
import com.capitalone.identity.identitybuilder.model.EntityInfo;
import com.capitalone.identity.identitybuilder.model.EntityState;
import com.capitalone.identity.identitybuilder.policycore.service.constants.ApplicationConstants;
import com.capitalone.identity.identitybuilder.policycore.service.logging.PolicySystemEventLog;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Primary;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

@Primary
@Component
@Conditional(DevLocalProperties.DevLocalDisabled.class)
public class RuntimeUpdateEventAuditLogger implements RuntimeUpdateEventLogger {
    private static final Logger logger = LogManager.getLogger(RuntimeUpdateEventAuditLogger.class.getName());

    @Override
    @PolicySystemEventLog
    @EventStream(
            eventName = ApplicationConstants.EXTERNAL_DYNAMIC_EVENT_NAME_SCAN,
            customEventStreams = @EventStream.CustomEventStream(
                    type = EventStream.EventStreamType.AUDIT,
                    emitters = "sdpv3",
                    eventStreamDataBuilder = Cal1_5EventStreamDataBuilder.BUILDER_TYPE)
    )
    public LoggedScan auditScanResult(@NonNull LoggedScan scanResult) {
        return scanResult;
    }

    @Override
    @PolicySystemEventLog
    @EventStream(
            eventName = ApplicationConstants.EXTERNAL_DYNAMIC_EVENT_NAME_TERMINATE,
            customEventStreams = @EventStream.CustomEventStream(
                    type = EventStream.EventStreamType.AUDIT,
                    emitters = "sdpv3",
                    eventStreamDataBuilder = Cal1_5EventStreamDataBuilder.BUILDER_TYPE)
    )
    public LoggedTerminationEvent auditTermination(@Nullable String error) {
        if (error == null) {
            logger.error("Terminated without error");
        } else {
            logger.error("Terminate with error {}", error);
        }
        return new LoggedTerminationEvent(error);
    }

    @Override
    @PolicySystemEventLog
    @EventStream(
            eventName = ApplicationConstants.EXTERNAL_DYNAMIC_EVENT_NAME_UPDATE,
            customEventStreams = @EventStream.CustomEventStream(
                    type = EventStream.EventStreamType.AUDIT,
                    emitters = "sdpv3",
                    eventStreamDataBuilder = Cal1_5EventStreamDataBuilder.BUILDER_TYPE)
    )
    public LoggedUpdate auditUpdate(@NonNull EntityState.Delta.ChangeType type,
                                    @NonNull EntityInfo info,
                                    @NonNull EntityActivationStatus activationStatus) {
        return LoggedUpdate.newFromLoad(type, info, activationStatus);
    }

    @Override
    @PolicySystemEventLog
    @EventStream(
            eventName = ApplicationConstants.EXTERNAL_DYNAMIC_EVENT_NAME_UPDATE,
            customEventStreams = @EventStream.CustomEventStream(
                    type = EventStream.EventStreamType.AUDIT,
                    emitters = "sdpv3",
                    eventStreamDataBuilder = Cal1_5EventStreamDataBuilder.BUILDER_TYPE)
    )
    public LoggedUpdate auditUnload(@NonNull EntityInfo info) {
        return LoggedUpdate.newFromUnload(info);
    }

    @Override
    @PolicySystemEventLog
    @EventStream(
            eventName = ApplicationConstants.EXTERNAL_DYNAMIC_EVENT_NAME_UPDATE,
            customEventStreams = @EventStream.CustomEventStream(
                    type = EventStream.EventStreamType.AUDIT,
                    emitters = "sdpv3",
                    eventStreamDataBuilder = Cal1_5EventStreamDataBuilder.BUILDER_TYPE)
    )
    public LoggedUpdate auditLoadOperationFailed(@NonNull EntityState.Delta.ChangeType type, @NonNull EntityInfo info,
                                                 @NonNull Throwable error) {
        return LoggedUpdate.newFromError(info, type, error);
    }

    @Override
    @PolicySystemEventLog
    @EventStream(
            eventName = ApplicationConstants.EXTERNAL_DYNAMIC_EVENT_NAME_POLL_CONFIGURATION,
            customEventStreams = @EventStream.CustomEventStream(
                    type = EventStream.EventStreamType.AUDIT,
                    emitters = "sdpv3",
                    eventStreamDataBuilder = Cal1_5EventStreamDataBuilder.BUILDER_TYPE)
    )
    public LoggedPollingConfigurationChange auditPollingConfigurationChange(@NonNull String configurationDescription,
                                                                            @Nullable String error) {
        return new LoggedPollingConfigurationChange(configurationDescription, error);
    }
}
