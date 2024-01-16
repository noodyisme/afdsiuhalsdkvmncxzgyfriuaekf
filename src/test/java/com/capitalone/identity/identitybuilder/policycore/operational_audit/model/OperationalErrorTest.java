package com.capitalone.identity.identitybuilder.policycore.operational_audit.model;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OperationalErrorTest {

    @Test
    void ofThrowablePresent() {
        // Arrange
        final Throwable throwable = new RuntimeException("Error caught!");
        final OperationalError operationalErrorExpected = OperationalError.builder()
                .developerText("RuntimeException: Error caught!")
                .stackTrace(ExceptionUtils.getStackTrace(throwable))
                .build();

        // Act
        final OperationalError operationalErrorActual = OperationalError.of(throwable);

        // Assert
        assertEquals(operationalErrorExpected, operationalErrorActual);
    }

    @Test
    void ofThrowableNotPresent() {
        // Act & Assert
        assertNull(OperationalError.of(null));

    }

}