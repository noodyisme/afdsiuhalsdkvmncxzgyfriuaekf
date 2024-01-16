package com.capitalone.identity.identitybuilder.policycore.operational_audit.mapper;

import com.capitalone.identity.identitybuilder.policycore.operational_audit.OperationalAuditor;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.model.OperationalError;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.model.OperationalEvent;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.model.OperationalEventOutcome;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.model.event_models.HealthCheckEvaluated;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class HealthCheckEvaluatedMapperTest {

    @Mock
    OperationalAuditor operationalAuditor;

    private final OperationalEvent OPERATIONAL_EVENT_EXPECTED = OperationalEvent.builder().build();
    @InjectMocks
    HealthCheckEvaluatedMapper.HealthCheckPublisher healthCheckPublisher;

    @Spy
    HealthCheckEvaluatedMapper healthCheckEvaluatedMapper = new HealthCheckEvaluatedMapper(200);

    @Test
    void buildWithEntitySpecificAttributes_test() {
        healthCheckEvaluatedMapper = new HealthCheckEvaluatedMapper(HttpStatus.SC_OK);
        OperationalEvent mockEvent = healthCheckEvaluatedMapper.buildWithEntitySpecificAttributes();
        assertEquals(OPERATIONAL_EVENT_EXPECTED, mockEvent);
    }

    @ParameterizedTest
    @ValueSource(ints = {200, 424})
    void toOperationalEventOutcome_test(Integer status) {
        healthCheckEvaluatedMapper = new HealthCheckEvaluatedMapper(status);
        OperationalEventOutcome outcome = healthCheckEvaluatedMapper.toOperationalEventOutcome();
        assertEquals(status.toString(), outcome.getSeverity());
    }

    @ParameterizedTest
    @ValueSource(ints = {200, 424})
    void toOperationalError_test(Integer status) {
        healthCheckEvaluatedMapper = new HealthCheckEvaluatedMapper(status);
        Optional<OperationalError> error = healthCheckEvaluatedMapper.toOperationalError();
        assertEquals(Optional.empty(), error);
    }

    @ParameterizedTest
    @ValueSource(ints = {200, 424})
    void publishEvent_test(Integer status) {
        healthCheckEvaluatedMapper = new HealthCheckEvaluatedMapper(status);
        HealthCheckEvaluated event = new HealthCheckEvaluated(status);
        healthCheckPublisher.publishEvent(event);
        verify(operationalAuditor).audit(event, HealthCheckEvaluatedMapper.Factory.class);

    }

}
