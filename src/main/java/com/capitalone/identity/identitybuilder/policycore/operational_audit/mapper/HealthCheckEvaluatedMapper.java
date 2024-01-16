package com.capitalone.identity.identitybuilder.policycore.operational_audit.mapper;

import com.capitalone.identity.identitybuilder.policycore.operational_audit.OperationalAuditor;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.model.*;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.model.event_models.ApplicationBootstrapComplete;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.model.event_models.ApplicationBootstrapComplete_Publisher;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.model.event_models.HealthCheckEvaluated;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.model.event_models.HealthCheckEvaluated_Publisher;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.util.ApplicationBootstrapListener;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Log4j2
@AllArgsConstructor
public class HealthCheckEvaluatedMapper implements OperationalEventMapper {
    private final Integer status;

    @Override
    public OperationalEvent buildWithEntitySpecificAttributes() {
        final OperationalEvent.OperationalEventBuilder operationalEventBuilder = OperationalEvent.builder();
        return operationalEventBuilder.build();
    }

    @Override
    public OperationalEventOutcome toOperationalEventOutcome() {
        if (status / 100 == 2) {
            return OperationalEventOutcome.of(OperationalStatus.SUCCESS, status.toString());
        } else {
            return OperationalEventOutcome.of(OperationalStatus.FAILURE, status.toString());
        }
    }

    @Override
    public Optional<OperationalError> toOperationalError() {
        return Optional.empty();
    }

    @Component
    public static class HealthCheckPublisher implements HealthCheckEvaluated_Publisher, ApplicationBootstrapComplete_Publisher {

        private final OperationalAuditor operationalAuditor;
        private final ApplicationBootstrapListener bootstrapCompleteListener = new ApplicationBootstrapListener();

        public HealthCheckPublisher(OperationalAuditor auditor) {
            operationalAuditor = auditor;
        }

        @Override
        public void publishEvent(HealthCheckEvaluated event) {
            if (event.getStatus() == 200 && bootstrapCompleteListener.isBootstrapComplete()) {
                publishEvent(bootstrapCompleteListener.setBootstrapCompleteAndReturnEvent());
            }
            operationalAuditor.audit(event, HealthCheckEvaluatedMapper.Factory.class);
        }

        @Override
        public void publishEvent(ApplicationBootstrapComplete event) {
            operationalAuditor.audit(event, ApplicationBootstrapCompleteMapper.Factory.class);
        }
    }

    public static class Factory implements OperationalEventMapper.NotificationEventFactory {
        @Override
        public @NonNull OperationalEventMapper create(OperationalEventOccurred eventOccurred) {
            final Integer status = ((HealthCheckEvaluated) eventOccurred).getStatus();
            return new HealthCheckEvaluatedMapper(status);
        }
    }
}


