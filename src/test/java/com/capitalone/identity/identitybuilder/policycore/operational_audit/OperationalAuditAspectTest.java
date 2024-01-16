package com.capitalone.identity.identitybuilder.policycore.operational_audit;

import com.capitalone.identity.identitybuilder.policycore.operational_audit.cyber.AWSHostContextCollector;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.mapper.OperationalEventEnricher;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.mapper.OperationalEventMapper;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.model.OperationalError;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.model.OperationalEvent;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.model.OperationalEventOutcome;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.model.OperationalStatus;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.model.type.OperationalEventType;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.util.MockHostContext;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.util.OperationalAuditPublisher;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;

class OperationalAuditAspectTest {

    // Mocked inputs
    private static final OperationalEvent OPERATIONAL_EVENT_EXPECTED = Mockito.mock(OperationalEvent.class);
    private static final Object INPUT_EXPECTED = Mockito.mock(Object.class);
    private static final Object OUTPUT_EXPECTED = Mockito.mock(Object.class);

    private OperationalAuditPublisher operationalAuditPublisher;

    // Object under test
    private AnnotatedExample annotatedExample;
    private final AWSHostContextCollector mockAWSHostContextCollector = new AWSHostContextCollector(new MockHostContext());

    @BeforeEach
    void setup() {
        // Mocked dependencies
        final OperationalEventEnricher operationalEventEnricher = Mockito.mock(OperationalEventEnricher.class);
        operationalAuditPublisher = Mockito.mock(OperationalAuditPublisher.class);

        // Instantiate aspect and register annotated example with it
        final OperationalAuditAspect aspect = new OperationalAuditAspect(operationalEventEnricher,
                operationalAuditPublisher, new HashMap<>(), mockAWSHostContextCollector);
        Mockito.when(operationalEventEnricher.enrich(any(), any(), any())).thenReturn(OPERATIONAL_EVENT_EXPECTED);
        annotatedExample = OperationalAuditTestUtil.registerWithAspect(new AnnotatedExample(), aspect);
    }

    @Test
    void success() {
        // Act
        final Object outputActual = annotatedExample.runDefault(INPUT_EXPECTED);

        // Assert
        Assertions.assertSame(OUTPUT_EXPECTED, outputActual);
        Mockito.verify(operationalAuditPublisher).publish(OPERATIONAL_EVENT_EXPECTED);
    }

    @Test
    void mapperFailedFactoryInstantiation() {
        // Act
        final Object actualOutput = annotatedExample.runFailedFactoryInstantiation(INPUT_EXPECTED);

        // Assert
        Assertions.assertSame(OUTPUT_EXPECTED, actualOutput);
        Mockito.verifyNoInteractions(operationalAuditPublisher);
    }

    @Test
    void mapperFailedCreation() {
        // Act
        final Object actualOutput = annotatedExample.runFailedMapperCreation(INPUT_EXPECTED);

        // Assert
        Assertions.assertSame(OUTPUT_EXPECTED, actualOutput);
        Mockito.verifyNoInteractions(operationalAuditPublisher);
    }

    @Test
    void exceptionRaisedFailure() {
        // Act
        Assertions.assertThrows(IllegalStateException.class, () -> annotatedExample.runExceptionFailure(INPUT_EXPECTED));

        // Assertions
        Mockito.verify(operationalAuditPublisher).publish(OPERATIONAL_EVENT_EXPECTED);
    }

    @Test
    void publishFailure() {
        // Arrange
        Mockito.doThrow(new IllegalStateException("test")).when(operationalAuditPublisher).publish(OPERATIONAL_EVENT_EXPECTED);

        // Act
        final Object outputActual = annotatedExample.runDefault(INPUT_EXPECTED);

        // Assert
        Assertions.assertSame(OUTPUT_EXPECTED, outputActual);
    }

    @Test
    void publishNoArgs() {
        // Act
        final Object outputActual = annotatedExample.runNoArgs();

        // Assert
        Assertions.assertSame(OUTPUT_EXPECTED, outputActual);
    }

    /**
     * Supporting Test-Only Example of Aspect Usage
     */
    @AllArgsConstructor
    private static class AnnotatedExample {

        @OperationalAudit(eventType = OperationalEventType.POLICY_EVALUATED, mapperFactory = ExampleMapper.SuccessFactory.class)
        Object runDefault(final Object input) {
            return OUTPUT_EXPECTED;
        }

        @OperationalAudit(eventType = OperationalEventType.POLICY_EVALUATED, mapperFactory = ExampleMapper.SuccessFactory.class)
        Object runExceptionFailure(final Object input) {
            throw new IllegalStateException("test");
        }

        @OperationalAudit(eventType = OperationalEventType.POLICY_EVALUATED, mapperFactory = ExampleMapper.FailedInstantiationFactory.class)
        Object runFailedFactoryInstantiation(final Object input) {
            return OUTPUT_EXPECTED;
        }

        @OperationalAudit(eventType = OperationalEventType.POLICY_EVALUATED, mapperFactory = ExampleMapper.FailedCreationFactory.class)
        Object runFailedMapperCreation(final Object input) {
            return OUTPUT_EXPECTED;
        }

        @OperationalAudit(eventType = OperationalEventType.POLICY_EVALUATED, mapperFactory = ExampleMapper.SuccessFactory.class)
        Object runNoArgs() {
            return OUTPUT_EXPECTED;
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

        static class SuccessFactory implements AspectEventFactory {
            @Override
            public @NonNull OperationalEventMapper create(Object request, Object response) {
                return new ExampleMapper();
            }
        }

        static class FailedInstantiationFactory extends SuccessFactory {
            private FailedInstantiationFactory(){
            }
        }

        static class FailedCreationFactory implements AspectEventFactory {
            @Override
            public @NonNull OperationalEventMapper create(Object request, Object response) {
                throw new ClassCastException("test");
            }
        }
    }

}