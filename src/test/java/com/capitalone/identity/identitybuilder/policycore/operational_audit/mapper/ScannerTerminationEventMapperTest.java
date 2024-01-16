package com.capitalone.identity.identitybuilder.policycore.operational_audit.mapper;

import com.capitalone.identity.identitybuilder.policycore.camel.external.dynamic.UpdateTransactionResult;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.model.OperationalError;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.model.OperationalEventOutcome;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.model.configstore.ScannerTerminationEventOccurred;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.model.type.OperationalEventType;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.util.OperationalAuditConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

public class ScannerTerminationEventMapperTest {
    private OperationalEventOutcome outcomeExpected;
    private Optional<OperationalError> operationalErrorExpected;
    private UpdateTransactionResult updateTransactionResult;
    private Throwable throwable;

    ScannerTerminationEventMapper scannerTerminationEventMapper;
    ScannerTerminationEventOccurred scannerTerminationEvent;

    @BeforeEach
    void setup() {
        outcomeExpected = OperationalAuditConstants.OUTCOME_SUCCESS_INFO;
        operationalErrorExpected = Optional.empty();

        throwable = null;
        updateTransactionResult = UpdateTransactionResult.success();
        initScanTerminationEventMapper();
    }

    @Test
    void operationalOutcomeNoThrowableSuccess() {
        final OperationalEventOutcome outcomeActual = scannerTerminationEventMapper.toOperationalEventOutcome();
        assertEquals(outcomeExpected, outcomeActual);
    }

    @Test
    void operationalOutcomeThrowableSuccess() {
        throwable = new RuntimeException("Runtime Error!!");
        updateTransactionResult = UpdateTransactionResult.error(throwable);
        initScanTerminationEventMapper();

        final OperationalEventOutcome outcomeExpected = OperationalAuditConstants.OUTCOME_FAILURE_ERROR;
        final OperationalEventOutcome outcomeActual = scannerTerminationEventMapper.toOperationalEventOutcome();

        assertEquals(outcomeExpected, outcomeActual);
    }

    @Test
    void operationalErrorNoThrowableSuccess() {
        // Act
        final Optional<OperationalError> operationalErrorActual = scannerTerminationEventMapper.toOperationalError();

        // Assert
        assertEquals(operationalErrorExpected, operationalErrorActual);
    }

    @Test
    void operationalErrorThrowableSuccess() {
        throwable = new RuntimeException("Test");
        updateTransactionResult = UpdateTransactionResult.error(throwable);
        initScanTerminationEventMapper();
        operationalErrorExpected = Optional.of(OperationalError.of(throwable));

        final Optional<OperationalError> operationalErrorActual = scannerTerminationEventMapper.toOperationalError();

        assertEquals(operationalErrorExpected, operationalErrorActual);
    }

    @Test
    void createFromFactoryTest() {
        ScannerTerminationEventMapper.Factory factory = new ScannerTerminationEventMapper.Factory();

        final  OperationalEventMapper operationalEventMapper = factory.create(scannerTerminationEvent);

        assertInstanceOf(ScannerTerminationEventMapper.class, operationalEventMapper);
    }



    private void initScanTerminationEventMapper() {
        scannerTerminationEvent = ScannerTerminationEventOccurred.builder()
                .operationalEventType(OperationalEventType.SCANNER_TERMINATED)
                .result(updateTransactionResult)
                .build();
        scannerTerminationEventMapper = new ScannerTerminationEventMapper(scannerTerminationEvent);
    }

}
