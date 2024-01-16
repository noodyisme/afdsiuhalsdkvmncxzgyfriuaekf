package com.capitalone.identity.identitybuilder.policycore.operational_audit.mapper;

import com.capitalone.identity.identitybuilder.model.DynamicUpdateProperties;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.model.OperationalError;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.model.OperationalEvent;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.model.OperationalEventOutcome;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.model.configstore.OperationalConfigStoreMetadata;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.model.configstore.PollingConfigEvent;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.util.OperationalAuditConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Duration;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class PollingConfigEventMapperTest {

    // Expected outputs
    private OperationalEventOutcome outcomeExpected;
    private Optional<OperationalError> operationalErrorExpected;

    // Event Inputs & Event
    private Long timestamp;
    private DynamicUpdateProperties dynamicUpdateProperties;
    private Throwable throwable;
    private PollingConfigEvent pollingConfigEvent;

    // Object under test
    private PollingConfigEventMapper pollingConfigEventMapper;

    @BeforeEach
    void setup() {
        // Initialize default happy path expected outputs
        outcomeExpected = OperationalAuditConstants.OUTCOME_SUCCESS_INFO;
        operationalErrorExpected = Optional.empty();

        // Initialize default happy path event and mapper
        timestamp = System.currentTimeMillis();
        dynamicUpdateProperties = Mockito.mock(DynamicUpdateProperties.class);
        Mockito.when(dynamicUpdateProperties.getInterval()).thenReturn(Duration.of(1, ChronoUnit.DAYS));
        Mockito.when(dynamicUpdateProperties.getTimeOfDayUTC()).thenReturn(LocalTime.NOON);
        throwable = null;
        initPollingConfigEventMapper();
    }

    @Test
    void buildPollingConfigAttributesSuccess() {
        // Arrange
        final OperationalConfigStoreMetadata configStoreMetadataExpected = OperationalConfigStoreMetadata.builder()
                .newPollingConfig("{\"timeOfDayUTC\":\"12:00:00\",\"interval\":86400.000000000}")
                .build();

        // Act
        final OperationalEvent operationalEvent = pollingConfigEventMapper.buildWithEntitySpecificAttributes();

        // Assert
        assertEquals(configStoreMetadataExpected, operationalEvent.getConfigStoreMetadata());
    }

    @Test
    void operationalOutcomeNoThrowableSuccess() {
        // Act
        final OperationalEventOutcome outcomeActual = pollingConfigEventMapper.toOperationalEventOutcome();

        // Assert
        assertEquals(outcomeExpected, outcomeActual);
    }

    @Test
    void operationalErrorNoThrowableSuccess() {
        // Act
        final Optional<OperationalError> operationalErrorActual = pollingConfigEventMapper.toOperationalError();

        // Assert
        assertEquals(operationalErrorExpected, operationalErrorActual);
    }

    @Test
    void operationalOutcomeThrowableSuccess() {
        // Arrange
        throwable = new RuntimeException("Runtime Error!!");
        initPollingConfigEventMapper();
        final OperationalEventOutcome outcomeExpected = OperationalAuditConstants.OUTCOME_FAILURE_ERROR;

        // Act
        final OperationalEventOutcome outcomeActual = pollingConfigEventMapper.toOperationalEventOutcome();

        // Assert
        assertEquals(outcomeExpected, outcomeActual);
    }

    @Test
    void operationalErrorThrowableSuccess() {
        // Arrange
        throwable = new RuntimeException("Runtime error!");
        initPollingConfigEventMapper();
        operationalErrorExpected = Optional.of(OperationalError.of(throwable));


        // Act
        final Optional<OperationalError> operationalErrorActual = pollingConfigEventMapper.toOperationalError();

        // Assert
        assertEquals(operationalErrorExpected, operationalErrorActual);
    }

    @Test
    void createFromFactorySuccess() {
        // Arrange
        PollingConfigEventMapper.Factory factory = new PollingConfigEventMapper.Factory();

        // Act
        final OperationalEventMapper operationalEventMapper = factory.create(pollingConfigEvent);

        // Assert
        assertInstanceOf(PollingConfigEventMapper.class, operationalEventMapper);
    }

    private void initPollingConfigEventMapper() {
        pollingConfigEvent = new PollingConfigEvent(timestamp, dynamicUpdateProperties, throwable);
        pollingConfigEventMapper = new PollingConfigEventMapper(pollingConfigEvent);
    }

}