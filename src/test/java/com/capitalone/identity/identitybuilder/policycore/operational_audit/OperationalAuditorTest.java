package com.capitalone.identity.identitybuilder.policycore.operational_audit;

import com.capitalone.identity.identitybuilder.policycore.operational_audit.cyber.AWSHostContextCollector;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.mapper.OperationalEventEnricher;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.mapper.OperationalEventMapper;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.model.*;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.model.type.OperationalEventType;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.util.MockHostContext;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.util.OperationalAuditPublisher;
import lombok.NonNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;

class OperationalAuditorTest {

    // Mocked inputs
    private static final OperationalEvent OPERATIONAL_EVENT_EXPECTED = Mockito.mock(OperationalEvent.class);
    private static final OperationalEventOccurred INPUT = new ExampleOperationalEventOccurred();

    // Mocked Dependencies
    private OperationalEventEnricher operationalEventEnricher;
    private OperationalAuditPublisher operationalAuditPublisher;

    // Non-mocked Dependencies
    private OperationalAuditor.OperationalAuditDispatcher operationalAuditDispatcher;

    private final AWSHostContextCollector mockAWSHostContextCollector = new AWSHostContextCollector(new MockHostContext());


    // Object under test
    private OperationalAuditor operationalAuditor;

    @BeforeEach
    void setup() {
        operationalEventEnricher = Mockito.mock(OperationalEventEnricher.class);
        operationalAuditPublisher = Mockito.mock(OperationalAuditPublisher.class);
        operationalAuditDispatcher = new OperationalAuditor.OperationalAuditDispatcher(
                operationalEventEnricher, operationalAuditPublisher, mockAWSHostContextCollector);
        operationalAuditor = new OperationalAuditor(operationalAuditDispatcher);
        Mockito.when(operationalEventEnricher.enrich(any(), any(), any())).thenReturn(OPERATIONAL_EVENT_EXPECTED);
    }

    @Test
    void auditSuccess() {
        // Act
        operationalAuditor.audit(INPUT, ExampleMapper.DefaultFactory.class);

        // Assert
        Mockito.verify(operationalAuditPublisher).publish(OPERATIONAL_EVENT_EXPECTED);
    }

    @Test
    void auditFailure() {
        // Arrange
        operationalAuditDispatcher = Mockito.mock(OperationalAuditor.OperationalAuditDispatcher.class);
        Mockito.doThrow(new IllegalStateException("test")).when(operationalAuditDispatcher).doAudit(any(), any());

        // Act & Assert
        Assertions.assertDoesNotThrow(() -> operationalAuditor.audit(INPUT, ExampleMapper.DefaultFactory.class));
    }

    @Test
    void auditNoDispatcher() {
        // Arrange
        operationalAuditor = new OperationalAuditor(null);

        // Act
        operationalAuditor.audit(INPUT, ExampleMapper.DefaultFactory.class);

        // Assert
        Mockito.verifyNoInteractions(operationalEventEnricher);
        Mockito.verifyNoInteractions(operationalAuditPublisher);
    }

    @Test
    void auditMapperFactoryInstantiationFailure() {
        // Act
        operationalAuditor.audit(INPUT, ExampleMapper.FailedInstantiationFactory.class);

        // Assert
        Mockito.verifyNoInteractions(operationalEventEnricher);
        Mockito.verifyNoInteractions(operationalAuditPublisher);
    }

    @Test
    void auditMapperCreationFailure() {
        // Act
        operationalAuditor.audit(INPUT, ExampleMapper.FailedMapperCreation.class);

        // Assert
        Mockito.verifyNoInteractions(operationalEventEnricher);
        Mockito.verifyNoInteractions(operationalAuditPublisher);
    }

    /**
     * Supporting Test-Only Event Object
     */
    private static class ExampleOperationalEventOccurred implements OperationalEventOccurred {

        @Override public @NonNull OperationalEventType getOperationalEventType() {
            return OperationalEventType.ENTITY_UPDATED;
        }

        @Override public @NonNull Long getEndTimestamp() {
            return System.currentTimeMillis();
        }
    }

    /**
     * Supporting Test-Only Event Mapper
     */
    private static class ExampleMapper implements OperationalEventMapper {

        @Override public OperationalEvent buildWithEntitySpecificAttributes() {
            return OPERATIONAL_EVENT_EXPECTED;
        }

        @Override public OperationalEventOutcome toOperationalEventOutcome() {
            return OperationalEventOutcome.of(OperationalStatus.SUCCESS, "INFO");
        }

        @Override public Optional<OperationalError> toOperationalError() {
            return Optional.empty();
        }

        static class DefaultFactory implements NotificationEventFactory {
            @Override public @NonNull OperationalEventMapper create(
                    OperationalEventOccurred operationalEventOccurred) {
                return new ExampleMapper();
            }
        }

        static class FailedInstantiationFactory extends DefaultFactory {
            private FailedInstantiationFactory() {}
        }

        static class FailedMapperCreation implements NotificationEventFactory {
            @Override public @NonNull OperationalEventMapper create(
                    OperationalEventOccurred operationalEventOccurred) {
                throw new ClassCastException("test");
            }
        }
    }

}