package com.capitalone.identity.identitybuilder.policycore.operational_audit.mapper;

import com.capitalone.identity.identitybuilder.policycore.operational_audit.OperationalAuditor;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.model.*;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.model.event_models.ExceptionIntercepted;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.model.event_models.ExceptionIntercepted_Publisher;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.model.policy.OperationalExecutionContext;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Log4j2
@AllArgsConstructor
public class ExceptionInterceptedEventMapper implements OperationalEventMapper {

    private final String methodSource;
    private final String callingClass;
    private final String args;
    private final Throwable throwable;
    private final Integer status;

    @Override
    public OperationalEvent buildWithEntitySpecificAttributes() {


        final OperationalEvent.OperationalEventBuilder operationalEventBuilder = OperationalEvent.builder();
        final OperationalExecutionContext.OperationalExecutionContextBuilder executionContextBuilder =
                OperationalExecutionContext.builder();
        List<OperationalSupplementalAttribute> supplementalAttributeList = new ArrayList<>();
        supplementalAttributeList.add(new OperationalSupplementalAttribute("method_arguments", args));
        return operationalEventBuilder
                .eventEntityName(throwable.getClass().getSimpleName())
                .eventEntityValue(callingClass + "#" + methodSource + " -> " + ExceptionUtils.getMessage(throwable))
                .executionContext(executionContextBuilder.build())
                .supplementalMetadata(supplementalAttributeList)
                .build();
    }

    @Override
    public OperationalEventOutcome toOperationalEventOutcome() {
        if ((status / 100) == 4) {
            return OperationalEventOutcome.of(OperationalStatus.INVALID, status.toString());
        } else if ((status / 100) == 5) {
            return OperationalEventOutcome.of(OperationalStatus.FAILURE, status.toString());
        } else { //1xx 2xx 3xx
            return OperationalEventOutcome.of(OperationalStatus.SUCCESS, status.toString());
        }
    }


    @Override
    public Optional<OperationalError> toOperationalError() { return Optional.of(OperationalError.of(throwable)); }

    @Component
    @AllArgsConstructor
    public static class ExceptionInterceptPublisher implements ExceptionIntercepted_Publisher {

        private final OperationalAuditor operationalAuditor;

        @Override
        public void publishEvent(ExceptionIntercepted event) {
            operationalAuditor.audit(event, ExceptionInterceptedEventMapper.Factory.class);
        }
    }


    public static class Factory implements OperationalEventMapper.NotificationEventFactory {


        @Override
        public @NonNull OperationalEventMapper create(OperationalEventOccurred eventOccurred) {
            final ExceptionIntercepted exceptionIntercepted = ((ExceptionIntercepted) eventOccurred);


            return new ExceptionInterceptedEventMapper(exceptionIntercepted.getMethodSource(),
                    exceptionIntercepted.getCallingClass(),
                    exceptionIntercepted.getArgs(),
                    exceptionIntercepted.getThrowable(),
                    exceptionIntercepted.getHttpStatus());
        }
    }
}
