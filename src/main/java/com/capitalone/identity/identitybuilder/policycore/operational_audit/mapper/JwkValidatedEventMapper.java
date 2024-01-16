package com.capitalone.identity.identitybuilder.policycore.operational_audit.mapper;

import com.capitalone.identity.identitybuilder.policycore.operational_audit.OperationalAuditor;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.model.*;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.model.event_models.JwkValidated;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.model.event_models.JwkValidated_Publisher;
import com.capitalone.identity.identitybuilder.policycore.service.jws.JWTExtractionException;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Log4j2
@AllArgsConstructor
public class JwkValidatedEventMapper implements OperationalEventMapper {
    private final String eventMessage;
    private final JWTExtractionException.JwsExceptionType jwsExceptionType;
    private final Boolean applicationLevelProductIdValidation;
    private final Boolean policyLevelProductIdValidation;
    private final String jwkProductId;
    private final String policyExpectedProductId;

    static final String EVENT_DETAIL = "event_detail";
    static final String JWK_EXCEPTION_TYPE = "jwk_exception_type";
    static final String APPLICATION_LEVEL_PRODUCTID_VALIDATION = "application_level_productId_validation";
    static final String POLICY_LEVEL_PRODUCTID_VALIDATION = "policy_level_productId_validation";
    static final String JWK_PRODUCTID = "jwk_productId";
    static final String POLICY_EXPECTED_PRODUCTID = "policy_expected_productId";
    static final String SEVERITY_SUCCESS_STRING = "SUCCESS";
    static final String SEVERITY_FAILURE_STRING = "FAILURE";

    @Override
    public OperationalEvent buildWithEntitySpecificAttributes() {
        final OperationalEvent.OperationalEventBuilder operationalEventBuilder = OperationalEvent.builder();
        final List<OperationalSupplementalAttribute> supplementalAttributes = new ArrayList<>();

        supplementalAttributes.add(new OperationalSupplementalAttribute(EVENT_DETAIL, eventMessage));
        supplementalAttributes.add(new OperationalSupplementalAttribute(JWK_EXCEPTION_TYPE,jwsExceptionType!=null? jwsExceptionType.name():null));
        supplementalAttributes.add(new OperationalSupplementalAttribute(APPLICATION_LEVEL_PRODUCTID_VALIDATION, applicationLevelProductIdValidation.toString()));
        supplementalAttributes.add(new OperationalSupplementalAttribute(POLICY_LEVEL_PRODUCTID_VALIDATION, policyLevelProductIdValidation.toString()));
        supplementalAttributes.add(new OperationalSupplementalAttribute(JWK_PRODUCTID, jwkProductId));
        supplementalAttributes.add(new OperationalSupplementalAttribute(POLICY_EXPECTED_PRODUCTID, policyExpectedProductId));


        return operationalEventBuilder.supplementalMetadata(supplementalAttributes).build();
    }

    @Override
    public OperationalEventOutcome toOperationalEventOutcome() {
        if(jwsExceptionType!=null){
            return OperationalEventOutcome.of(OperationalStatus.FAILURE, SEVERITY_FAILURE_STRING);
        } else {
            return OperationalEventOutcome.of(OperationalStatus.SUCCESS, SEVERITY_SUCCESS_STRING);

        }
    }

    @Override
    public Optional<OperationalError> toOperationalError() {
        return Optional.empty();
    }

    @Component
    public static class JwkValidationPublisher implements JwkValidated_Publisher{

        private final OperationalAuditor operationalAuditor;

        public JwkValidationPublisher(OperationalAuditor auditor) {
            operationalAuditor = auditor;
        }

        @Override
        public void publishEvent(JwkValidated event) {
            operationalAuditor.audit(event, JwkValidatedEventMapper.Factory.class);
        }
    }

    public static class Factory implements OperationalEventMapper.NotificationEventFactory {
        @Override
        public @NonNull OperationalEventMapper create(OperationalEventOccurred eventOccurred) {
            final String eventMessage = ((JwkValidated) eventOccurred).getEventMessage();
            final JWTExtractionException.JwsExceptionType jwsExceptionType = ((JwkValidated) eventOccurred).getJwsExceptionType();
            final boolean applicationLevelProductIdValidation = ((JwkValidated) eventOccurred).getApplicationLevelProductIdValidation();
            final boolean policyLevelProductIdValidation = ((JwkValidated) eventOccurred).getPolicyLevelProductIdValidation();
            final String jwkProductId = ((JwkValidated) eventOccurred).getJwkProductId();
            final String policyExpectedProductId = ((JwkValidated) eventOccurred).getPolicyExpectedProductId();

            return new JwkValidatedEventMapper(eventMessage, jwsExceptionType, applicationLevelProductIdValidation
                    ,policyLevelProductIdValidation, jwkProductId, policyExpectedProductId);
        }
    }
}
