package com.capitalone.identity.identitybuilder.policycore.operational_audit.mapper;

import com.capitalone.identity.identitybuilder.model.ScanRequest;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.model.OperationalError;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.model.OperationalEvent;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.model.OperationalEventOutcome;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.model.configstore.OperationalConfigStoreMetadata;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.model.configstore.ScanOperationEvent;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.util.OperationalAuditConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

public class ScanOperationEventMapperTest {

    private OperationalEventOutcome outcomeExpected;
    private Optional<OperationalError> operationalErrorExpected;

    private ScanOperationEvent scanOperationEvent;
    private ScanOperationEventMapper scanOperationEventMapper;

    private Long timestamp;
    private ScanRequest scanRequest;

    @BeforeEach
    void setup() {
        outcomeExpected = OperationalAuditConstants.OUTCOME_SUCCESS_INFO;
        operationalErrorExpected = Optional.empty();

        timestamp = System.currentTimeMillis();
        scanRequest = Mockito.mock(ScanRequest.class);
        Mockito.when(scanRequest.getScanType()).thenReturn(ScanRequest.ScanType.POLL);
        Mockito.when(scanRequest.getStartScheduled()).thenReturn(100L);
        Mockito.when(scanRequest.getStartActual()).thenReturn(200L);
        initScanOperationEventMapper();
    }

    @Test
    void buildScanOperationAttributesSuccess() {
        final OperationalConfigStoreMetadata configStoreMetadataExpected =  OperationalConfigStoreMetadata.builder()
                .scanScheduled("100")
                .scanType(ScanRequest.ScanType.POLL.name())
                .build();

        final OperationalEvent operationalEvent = scanOperationEventMapper.buildWithEntitySpecificAttributes();

        assertEquals(configStoreMetadataExpected, operationalEvent.getConfigStoreMetadata());
    }

    @Test
    void operationalOutcomeNoThrowableSuccess() {
        // Act
        final OperationalEventOutcome outcomeActual = scanOperationEventMapper.toOperationalEventOutcome();

        // Assert
        assertEquals(outcomeExpected, outcomeActual);
    }

    @Test
    void operationalErrorNoThrowableSuccess() {
        // Act
        final Optional<OperationalError> operationalErrorActual = scanOperationEventMapper.toOperationalError();

        // Assert
        assertEquals(operationalErrorExpected, operationalErrorActual);
    }

    @Test
    void createFromFactorySuccess() {
        // Arrange
        ScanOperationEventMapper.Factory factory = new ScanOperationEventMapper.Factory();

        // Act
        final OperationalEventMapper operationalEventMapper = factory.create(scanOperationEvent);

        // Assert
        assertInstanceOf(ScanOperationEventMapper.class, operationalEventMapper);
    }


    private void initScanOperationEventMapper() {
        scanOperationEvent = new ScanOperationEvent(timestamp, scanRequest);
        scanOperationEventMapper = new ScanOperationEventMapper(scanOperationEvent);
    }
}
