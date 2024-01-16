package com.capitalone.identity.identitybuilder.policycore.camel.external.logging;

import com.capitalone.identity.identitybuilder.client.dynamic.DynamicUpdateConfigurationProperties;
import com.capitalone.identity.identitybuilder.model.*;
import com.capitalone.identity.identitybuilder.policycore.camel.external.dynamic.MockEntityInfo;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RuntimeUpdateEventLoggerTest {

    static List<RuntimeUpdateEventLogger> provideLogger() {
        return Arrays.asList(new RuntimeUpdateEventSystemLogger(), new RuntimeUpdateEventAuditLogger());
    }

    private static IOException newUnserializableError() {
        final ObjectMapper mapper = new ObjectMapper()
                .configure(JsonParser.Feature.STRICT_DUPLICATE_DETECTION, true);
        final String value = "{\"Status\": \"ARCHIVE\",\"Status\": \"ACTIVE\"\n}";
        try {
            mapper.readValue(value, Map.class);
            throw new IllegalStateException("expected to generate an error that is known to be unserializable");
        } catch (JsonProcessingException e) {
            return e;
        }
    }

    private final EntityInfo mockEntityInfo = new MockEntityInfo("a/b/c", EntityType.POLICY);

    @Mock
    ConfigStoreScanCompleted mockScanResult;

    @ParameterizedTest
    @MethodSource("provideLogger")
    void auditTermination(RuntimeUpdateEventLogger logger) {
        assertDoesNotThrow(() -> logger.auditTermination(null));
        assertDoesNotThrow(() -> logger.auditTermination("test"));
    }

    @ParameterizedTest
    @MethodSource("provideLogger")
    void auditUpdate(RuntimeUpdateEventLogger logger) {
        EntityState.Delta<EntityInfo> change = new EntityState.Delta<>(EntityState.Delta.ChangeType.ADD, mockEntityInfo);
        assertDoesNotThrow(() -> logger.auditUpdate(change.getType(), change.getEntityInfo(), EntityActivationStatus.ACTIVE));
        assertDoesNotThrow(() -> logger.auditUpdate(change.getType(), change.getEntityInfo(), EntityActivationStatus.ACTIVE));
        assertDoesNotThrow(() -> logger.auditUpdate(change.getType(), change.getEntityInfo(), EntityActivationStatus.ACTIVE));
    }


    @ParameterizedTest
    @MethodSource("provideLogger")
    void auditScanResult(RuntimeUpdateEventLogger logger) {
        ScanRequest mockRequest = new ScanRequest(System.currentTimeMillis(), ScanRequest.ScanType.POLL);
        when(mockScanResult.getRequest()).thenReturn(mockRequest);
        assertDoesNotThrow(() -> logger.auditScanResult(LoggedScan.newFromResult(mockScanResult)));
    }

    @ParameterizedTest
    @MethodSource("provideLogger")
    void auditPollingConfigProperties(RuntimeUpdateEventLogger logger) {
        String description = new DynamicUpdateConfigurationProperties("12:00:00", "PT12H").toString();
        assertDoesNotThrow(() -> logger.auditPollingConfigurationChange(description, "test"));
        assertDoesNotThrow(() -> logger.auditPollingConfigurationChange(description, null));
    }

}
