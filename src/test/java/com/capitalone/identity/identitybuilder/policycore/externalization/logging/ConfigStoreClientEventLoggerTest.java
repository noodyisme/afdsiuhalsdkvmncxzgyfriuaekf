package com.capitalone.identity.identitybuilder.policycore.externalization.logging;

import com.capitalone.identity.identitybuilder.client.dynamic.PollingConfiguration;
import com.capitalone.identity.identitybuilder.model.ConfigStoreScanCompleted;
import com.capitalone.identity.identitybuilder.model.ScanRequest;
import com.capitalone.identity.identitybuilder.policycore.camel.external.logging.LoggedScan;
import com.capitalone.identity.identitybuilder.policycore.camel.external.logging.RuntimeUpdateEventLogger;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.OperationalAuditor;
import com.capitalone.identity.identitybuilder.polling.PollingConfigurationApplied;
import com.capitalone.identity.identitybuilder.polling.PollingConfigurationErrorOccurred;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;

import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ConfigStoreClientEventLoggerTest {

    @Mock
    RuntimeUpdateEventLogger auditLogger;

    @Mock
    OperationalAuditor operationalAuditor;

    ConfigStoreClientEventLogger applicationLogger;

    @BeforeEach
    void createLogger() {
        applicationLogger = new ConfigStoreClientEventLogger(auditLogger, operationalAuditor);
    }

    @Test
    void testNonNullConstructor() {
        assertThrows(NullPointerException.class, () -> new ConfigStoreClientEventLogger(null, null));
    }

    @Test
    void testPublishScanResultEvent() {
        ConfigStoreScanCompleted applicationEvent = new ConfigStoreScanCompleted(new ScanRequest(0L));

        applicationLogger.publishEvent(applicationEvent);

        LoggedScan auditEvent = LoggedScan.newFromResult(applicationEvent);
        verify(auditLogger).auditScanResult(auditEvent);
    }

    @Test
    void testPublishPollingConfigurationChangeEvent() {
        PollingConfiguration configuration = new PollingConfiguration(Duration.ofDays(1));
        PollingConfigurationApplied applicationEvent = new PollingConfigurationApplied(configuration);

        applicationLogger.publishEvent(applicationEvent);

        verify(auditLogger).auditPollingConfigurationChange(
                configuration.toString(),
                null);
    }

    @Test
    void testPublishPollingConfigurationErrorEvent() {
        PollingConfiguration configuration = new PollingConfiguration(Duration.ofDays(1));
        RuntimeException error = new RuntimeException("test");
        PollingConfigurationErrorOccurred applicationEvent = new PollingConfigurationErrorOccurred(
                configuration, error
        );

        applicationLogger.publishEvent(applicationEvent);

        verify(auditLogger).auditPollingConfigurationChange(
                configuration.toString(),
                ExceptionUtils.getStackTrace(error));
    }
}
