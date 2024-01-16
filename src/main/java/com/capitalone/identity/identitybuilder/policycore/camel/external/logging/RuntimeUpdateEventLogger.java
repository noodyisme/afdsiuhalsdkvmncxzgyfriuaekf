package com.capitalone.identity.identitybuilder.policycore.camel.external.logging;

import com.capitalone.identity.identitybuilder.model.EntityActivationStatus;
import com.capitalone.identity.identitybuilder.model.EntityInfo;
import com.capitalone.identity.identitybuilder.model.EntityState;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

/**
 * Logging formats defined at link below. Serialized objects defined in test expected to remain static.
 * https://confluence.kdc.capitalone.com/display/HOF/Logging+Requirements
 */
public interface RuntimeUpdateEventLogger {

    LoggedScan auditScanResult(@NonNull LoggedScan scanResult);

    LoggedTerminationEvent auditTermination(@Nullable String error);

    LoggedUpdate auditUpdate(@NonNull EntityState.Delta.ChangeType type,
                             @NonNull EntityInfo info,
                             @NonNull EntityActivationStatus activationStatus);

    LoggedUpdate auditUnload(@NonNull EntityInfo info);

    LoggedUpdate auditLoadOperationFailed(@NonNull EntityState.Delta.ChangeType type,
                                          @NonNull EntityInfo info,
                                          @NonNull Throwable error);

    LoggedPollingConfigurationChange auditPollingConfigurationChange(@NonNull String configurationDescription,
                                                                     @Nullable String error);

}
