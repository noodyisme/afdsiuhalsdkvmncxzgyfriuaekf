package com.capitalone.identity.identitybuilder.policycore.operational_audit.mapper;


import com.capitalone.identity.identitybuilder.policycore.operational_audit.model.*;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.util.OperationalAuditConstants;
import com.capitalone.identity.identitybuilder.policycore.service.jws.JwtKeyModel;
import com.capitalone.identity.identitybuilder.policycore.service.jws.JwtPublicKeyPushResponse;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.extern.log4j.Log4j2;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@AllArgsConstructor
@Log4j2
public class JwtPublicKeyRegisteredEventMapper implements OperationalEventMapper {
    private final @NonNull Optional<JwtKeyModel> jwtKeyModelOptional;
    private final @NonNull Optional<JwtPublicKeyPushResponse> devExchangeResponseOptional;

    private static final String JWT_KEY_PUSHED_EVENT_ENTITY_NAME = "JWT_KEY_PAIR_GENERATION";
    private static final String KID = "Kid";
    private static final String PUBLIC_KEY = "public_key";
    private static final String KEY_EXPIRATION_TIME = "key_expiration_time";
    private static final String DXGW_RESPONSE = "DXGW_response";
    private static final String DXGW_STATUS_CODE = "DXGW_status_code";

    private static final String UNABLE_TO_PUSH_PUBLIC_KEY_TO_DXGW = "Unable to push public key to DXGW";

    @Override
    public OperationalEvent buildWithEntitySpecificAttributes() {
        final OperationalEvent.OperationalEventBuilder operationalEventBuilder = OperationalEvent.builder();
        final List<OperationalSupplementalAttribute> supplementalAttributes = new ArrayList<>();

        jwtKeyModelOptional.ifPresent(jwtKeyModel -> {
            operationalEventBuilder.eventEntityName(JWT_KEY_PUSHED_EVENT_ENTITY_NAME);
            supplementalAttributes.add(new OperationalSupplementalAttribute(KID,jwtKeyModel.getKeyPairKid()));
            supplementalAttributes.add(new OperationalSupplementalAttribute(PUBLIC_KEY, jwtKeyModel.getTransformedPublicJWK()));
            supplementalAttributes.add(new OperationalSupplementalAttribute(KEY_EXPIRATION_TIME, String.valueOf(jwtKeyModel.getKeyPairExp())));
        });

        devExchangeResponseOptional.ifPresent(devExchangeResponse -> {
            supplementalAttributes.add(new OperationalSupplementalAttribute(DXGW_RESPONSE,devExchangeResponse.getPushResponse()));
            supplementalAttributes.add(new OperationalSupplementalAttribute(DXGW_STATUS_CODE,String.valueOf(devExchangeResponse.getRawHttpStatusCode())));
        });

        return operationalEventBuilder.supplementalMetadata(supplementalAttributes).build();
    }

    @Override
    public OperationalEventOutcome toOperationalEventOutcome() {
        if (!devExchangeResponseOptional.isPresent()) {
            return OperationalEventOutcome.of(OperationalStatus.FAILURE, OperationalAuditConstants.UNDEFINED);
        }
        final JwtPublicKeyPushResponse jwtPublicKeyPushResponse = devExchangeResponseOptional.get();
        final int rawStatusCode = jwtPublicKeyPushResponse.getRawHttpStatusCode();
        final String statusCodeString = String.valueOf(rawStatusCode);
        if (rawStatusCode == 200) {
            return OperationalEventOutcome.of(OperationalStatus.SUCCESS, statusCodeString);
        } else {
            return OperationalEventOutcome.of(OperationalStatus.FAILURE, statusCodeString);
        }
    }

    @Override
    public Optional<OperationalError> toOperationalError() {
        if (!devExchangeResponseOptional.isPresent()) {
            return Optional.empty();
        }
        final JwtPublicKeyPushResponse jwtPublicKeyPushResponse = devExchangeResponseOptional.get();
        if (jwtPublicKeyPushResponse.getRawHttpStatusCode()==200) {
            return Optional.empty();
        }
        final String responseAuditString = jwtPublicKeyPushResponse.getPushResponse();
        // might add dedicated error Id for the key registered event in the future
        return Optional.of(OperationalError.builder()
        .text(UNABLE_TO_PUSH_PUBLIC_KEY_TO_DXGW)
        .developerText(responseAuditString)
        .build());
    }

    public static class Factory implements OperationalEventMapper.AspectEventFactory {

        @NonNull
        @Override
        public JwtPublicKeyRegisteredEventMapper create(final Object request,
                                                        final Object response) {
            return new JwtPublicKeyRegisteredEventMapper(Optional.ofNullable((JwtKeyModel) request),
                    Optional.ofNullable((JwtPublicKeyPushResponse) response));
        }
    }
}
