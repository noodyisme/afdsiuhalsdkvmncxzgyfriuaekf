package com.capitalone.identity.identitybuilder.policycore.camel.external.logging;

import com.capitalone.identity.identitybuilder.model.EntityActivationStatus;
import com.capitalone.identity.identitybuilder.model.EntityInfo;
import com.capitalone.identity.identitybuilder.model.EntityState;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import java.io.IOError;

@Component
@ConditionalOnMissingBean(RuntimeUpdateEventAuditLogger.class)
public class RuntimeUpdateEventSystemLogger implements RuntimeUpdateEventLogger {
    private static final Logger logger = LogManager.getLogger(RuntimeUpdateEventSystemLogger.class.getName());

    private final ObjectMapper mapper = new ObjectMapper()
            .configure(SerializationFeature.INDENT_OUTPUT, true)
            .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, true);

    private <T> T logObject(Level level, T object) {
        logger.log(level, () -> {
            try {
                return mapper.writeValueAsString(object);
            } catch (JsonProcessingException e) {
                throw new IOError(e);
            }
        });
        return object;
    }

    @Override
    public LoggedScan auditScanResult(@NonNull LoggedScan scanResult) {
        return logObject(Level.TRACE, scanResult);
    }

    @Override
    public LoggedTerminationEvent auditTermination(@Nullable String error) {
        return logObject(Level.ERROR, new LoggedTerminationEvent(error));
    }

    @Override
    public LoggedUpdate auditUpdate(@NonNull EntityState.Delta.ChangeType type,
                                    @NonNull EntityInfo info,
                                    @NonNull EntityActivationStatus activationStatus) {
        return logObject(Level.INFO, LoggedUpdate.newFromLoad(type, info, activationStatus));
    }

    @Override
    public LoggedUpdate auditUnload(@NonNull EntityInfo info) {
        return logObject(Level.INFO, LoggedUpdate.newFromUnload(info));
    }

    @Override
    public LoggedUpdate auditLoadOperationFailed(@NonNull EntityState.Delta.ChangeType type,
                                                 @NonNull EntityInfo info, @NonNull Throwable error) {
        return logObject(Level.INFO, LoggedUpdate.newFromError(info, type, error));
    }

    @Override
    public LoggedPollingConfigurationChange auditPollingConfigurationChange(@NonNull String configurationDescription,
                                                                            @Nullable String error) {
        return logObject(Level.INFO, new LoggedPollingConfigurationChange(configurationDescription, error));
    }

}
