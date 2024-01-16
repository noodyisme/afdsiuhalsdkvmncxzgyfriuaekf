package com.capitalone.identity.identitybuilder.policycore.operational_audit.model;

import com.capitalone.identity.identitybuilder.policycore.operational_audit.model.type.OperationalEventType;
import lombok.NonNull;
import lombok.Value;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Optional;

class OperationalEventOccurredTest {

    /**
     * Verify that default implementations of methods are empty as expected
     */
    @Test
    void eventOccurredDefaultsSuccess() {
        // Arrange & Act
        final TestOperationalEventOccurred testEvent = new TestOperationalEventOccurred(
                OperationalEventType.ENTITY_UPDATED, 0L);

        // Assert
        Assertions.assertEquals(0L, testEvent.getStartTimestamp());
        Assertions.assertEquals(Optional.empty(), testEvent.getLoggerContextEventIdOptional());
    }

    @Value
    private static class TestOperationalEventOccurred implements OperationalEventOccurred {
        @NonNull OperationalEventType operationalEventType;
        @NonNull Long endTimestamp;
    }

}