package com.capitalone.identity.identitybuilder.policycore.operational_audit.mapper;

import com.capitalone.identity.identitybuilder.policycore.operational_audit.model.*;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.util.OperationalAuditConstants;
import com.capitalone.identity.identitybuilder.policycore.service.jws.JwtPublicKeyRetrievalResponse;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.extern.log4j.Log4j2;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Log4j2
@AllArgsConstructor
public class JwtPublicKeyRequestedEventMapper implements OperationalEventMapper{
    private final @NonNull Optional<String> kidOptional;
    private final @NonNull Optional<JwtPublicKeyRetrievalResponse> jwtPublicKeyRetrievalResponseOptional;

    private static final String JWT_CERTIFICATE_REQUESTED_EVENT = "JWT_CERTIFICATE_REQUESTED";



    @Override
    public OperationalEvent buildWithEntitySpecificAttributes() {
        final OperationalEvent.OperationalEventBuilder operationalEventBuilder = OperationalEvent.builder();
        final List<OperationalSupplementalAttribute> supplementalAttributes = new ArrayList<>();

        kidOptional.ifPresent(kid->{
            operationalEventBuilder.eventEntityName(JWT_CERTIFICATE_REQUESTED_EVENT);
            supplementalAttributes.add(new OperationalSupplementalAttribute("KID",kid));
        });

        jwtPublicKeyRetrievalResponseOptional.ifPresent(jwtPublicKeyRetrievalResponse ->
                supplementalAttributes.add(new OperationalSupplementalAttribute("JWK",jwtPublicKeyRetrievalResponse.getJwk())));

        return operationalEventBuilder.supplementalMetadata(supplementalAttributes).build();
    }

    @Override
    public OperationalEventOutcome toOperationalEventOutcome() {
        if(!jwtPublicKeyRetrievalResponseOptional.isPresent()){
            return OperationalEventOutcome.of(OperationalStatus.FAILURE, OperationalAuditConstants.UNDEFINED);
        }
        final JwtPublicKeyRetrievalResponse certificateRetrievalResponse = jwtPublicKeyRetrievalResponseOptional.get();
        if(certificateRetrievalResponse.getJwk()!=null) {
            return OperationalEventOutcome.of(OperationalStatus.SUCCESS, "SUCCESS");
        } else {
            return OperationalEventOutcome.of(OperationalStatus.FAILURE, "FAILURE");
        }
    }

    @Override
    public Optional<OperationalError> toOperationalError() {

        if (!jwtPublicKeyRetrievalResponseOptional.isPresent()) {
            return Optional.empty();
        }
        final JwtPublicKeyRetrievalResponse certificateRetrievalResponse = jwtPublicKeyRetrievalResponseOptional.get();
        if (certificateRetrievalResponse.getJwk()!=null) {
            return Optional.empty();
        }
        final String responseAuditString = certificateRetrievalResponse.getErrorMessage();
        return Optional.of(OperationalError.builder()
                .text("Unable to retrieve public key from DXGW Key Management API")
                .developerText(responseAuditString)
                .build());
    }


    public static class Factory implements OperationalEventMapper.AspectEventFactory {
        @NonNull
        @Override
        public JwtPublicKeyRequestedEventMapper create(final Object request,
                                                       final Object response) {

            return new JwtPublicKeyRequestedEventMapper(Optional.ofNullable((String) request),
                    Optional.ofNullable((JwtPublicKeyRetrievalResponse) response));
        }
    }
}