package com.capitalone.identity.identitybuilder.policycore.externalization.logging;

import com.capitalone.identity.identitybuilder.client.ConfigStoreClient_ApplicationEventPublisher;
import com.capitalone.identity.identitybuilder.model.ConfigStoreScanCompleted;
import com.capitalone.identity.identitybuilder.policycore.camel.external.logging.LoggedScan;
import com.capitalone.identity.identitybuilder.policycore.camel.external.logging.RuntimeUpdateEventLogger;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.OperationalAuditor;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.mapper.PollingConfigEventMapper;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.mapper.ScanOperationEventMapper;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.model.configstore.PollingConfigEvent;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.model.configstore.ScanOperationEvent;
import com.capitalone.identity.identitybuilder.polling.PollingConfigurationApplied;
import com.capitalone.identity.identitybuilder.polling.PollingConfigurationErrorOccurred;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnBean(value = {RuntimeUpdateEventLogger.class, OperationalAuditor.class})
@AllArgsConstructor
public class ConfigStoreClientEventLogger implements ConfigStoreClient_ApplicationEventPublisher {

    @NonNull private final RuntimeUpdateEventLogger logger;
    @NonNull private final OperationalAuditor operationalAuditor;

    @Override
    public void publishEvent(ConfigStoreScanCompleted event) {
        logger.auditScanResult(LoggedScan.newFromResult(event));
        operationalAuditor.audit(ScanOperationEvent.of(event), ScanOperationEventMapper.Factory.class);
    }

    @Override
    public void publishEvent(PollingConfigurationApplied event) {
        logger.auditPollingConfigurationChange(event.getConfiguration().toString(), null);
        operationalAuditor.audit(PollingConfigEvent.of(event), PollingConfigEventMapper.Factory.class);
    }

    @Override
    public void publishEvent(PollingConfigurationErrorOccurred event) {
        logger.auditPollingConfigurationChange(event.getConfiguration().toString(),
                ExceptionUtils.getStackTrace(event.getError()));
        operationalAuditor.audit(PollingConfigEvent.of(event), PollingConfigEventMapper.Factory.class);

    }
}
