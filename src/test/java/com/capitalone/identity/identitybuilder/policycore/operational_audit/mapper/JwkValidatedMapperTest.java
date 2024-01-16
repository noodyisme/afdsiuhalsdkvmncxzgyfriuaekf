package com.capitalone.identity.identitybuilder.policycore.operational_audit.mapper;

import com.capitalone.identity.identitybuilder.policycore.operational_audit.OperationalAuditor;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.model.OperationalError;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.model.OperationalEvent;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.model.OperationalEventOutcome;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.model.OperationalSupplementalAttribute;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.model.event_models.JwkValidated;
import com.capitalone.identity.identitybuilder.policycore.service.jws.JWTExtractionException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.capitalone.identity.identitybuilder.policycore.operational_audit.mapper.JwkValidatedEventMapper.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class JwkValidatedMapperTest {
    @Mock
    OperationalAuditor operationalAuditor;

    private final OperationalEvent OPERATIONAL_EVENT_EXPECTED = OperationalEvent.builder().build();
    @InjectMocks
    JwkValidatedEventMapper.JwkValidationPublisher jwkValidationPublisher;

    @Spy
    JwkValidatedEventMapper jwkValidatedEventMapper = new JwkValidatedEventMapper("defaultMessage",null,true,true,"testProductId","testProductId");




    @Test
    void buildWithEntitySpecificAttributes_test() {
        jwkValidatedEventMapper = new JwkValidatedEventMapper("defaultMessage",null,true,true,"testProductId","testProductId");
        OperationalEvent mockEvent = jwkValidatedEventMapper.buildWithEntitySpecificAttributes();
        final List<OperationalSupplementalAttribute> supplementalAttributes = new ArrayList<>();

        supplementalAttributes.add(new OperationalSupplementalAttribute(EVENT_DETAIL, "defaultMessage"));
        supplementalAttributes.add(new OperationalSupplementalAttribute(JWK_EXCEPTION_TYPE,null));
        supplementalAttributes.add(new OperationalSupplementalAttribute(APPLICATION_LEVEL_PRODUCTID_VALIDATION, "true"));
        supplementalAttributes.add(new OperationalSupplementalAttribute(POLICY_LEVEL_PRODUCTID_VALIDATION, "true"));
        supplementalAttributes.add(new OperationalSupplementalAttribute(JWK_PRODUCTID, "testProductId"));
        supplementalAttributes.add(new OperationalSupplementalAttribute(POLICY_EXPECTED_PRODUCTID, "testProductId"));
        OperationalEvent OPERATIONAL_EVENT_EXPECTED = OperationalEvent.builder().supplementalMetadata(supplementalAttributes).build();
        assertEquals(OPERATIONAL_EVENT_EXPECTED, mockEvent);
    }

    @ParameterizedTest
    @NullSource
    @EnumSource(value= JWTExtractionException.JwsExceptionType.class, names = {"JWT_MISSING_CERTIFICATE","JWT_PUBLIC_KEY_EXPIRED","JWT_PRODUCTID_MISMATCH" })
    void toOperationalEventOutcome_test(JWTExtractionException.JwsExceptionType exceptionType) {
        jwkValidatedEventMapper = new JwkValidatedEventMapper("defaultMessage",exceptionType,true,true,"testProductId","testProductId");
        OperationalEventOutcome outcome = jwkValidatedEventMapper.toOperationalEventOutcome();
        assertEquals(exceptionType==null?"SUCCESS":"FAILURE", outcome.getSeverity());
    }


    @ParameterizedTest
    @NullSource
    @EnumSource(value= JWTExtractionException.JwsExceptionType.class, names = {"JWT_MISSING_CERTIFICATE","JWT_PUBLIC_KEY_EXPIRED","JWT_PRODUCTID_MISMATCH" })
    void toOperationalError_test(JWTExtractionException.JwsExceptionType exceptionType) {
        jwkValidatedEventMapper = new JwkValidatedEventMapper("defaultMessage",exceptionType,true,true,"testProductId","testProductId");
        Optional<OperationalError> error = jwkValidatedEventMapper.toOperationalError();
        assertEquals(Optional.empty(), error);
    }

    @ParameterizedTest
    @NullSource
    @EnumSource(value= JWTExtractionException.JwsExceptionType.class, names = {"JWT_MISSING_CERTIFICATE","JWT_PUBLIC_KEY_EXPIRED","JWT_PRODUCTID_MISMATCH" })
    void publishEvent_test(JWTExtractionException.JwsExceptionType exceptionType) {
        jwkValidatedEventMapper = new JwkValidatedEventMapper("defaultMessage",exceptionType,true,true,"testProductId","testProductId");
        JwkValidated event = new JwkValidated("defaultMessage",exceptionType,true,true,"testProductId","testProductId");
        jwkValidationPublisher.publishEvent(event);
        verify(operationalAuditor).audit(event, JwkValidatedEventMapper.Factory.class);
    }
}
