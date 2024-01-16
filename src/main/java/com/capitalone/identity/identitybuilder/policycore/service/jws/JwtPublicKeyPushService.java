package com.capitalone.identity.identitybuilder.policycore.service.jws;

import com.capitalone.chassis.cal1_5.builder.Cal1_5EventStreamDataBuilder;
import com.capitalone.chassis.engine.annotations.stream.EventStream;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.OperationalAudit;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.mapper.JwtPublicKeyRegisteredEventMapper;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.model.type.OperationalEventType;
import com.newrelic.api.agent.Trace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.lang.invoke.MethodHandles;

@Service
public class JwtPublicKeyPushService {
    private static final String KMS_URL = "/developer-platform/key-management/certificates/keys/{kid}";
    static final String HEADER_ACCPET = "Accept";
    static final String HEADER_ACCPET_VALUE = "application/json;v=1";
    private static final String PUBLIC_KEY_STORED_TO_LOCAL_MAP = "Stored public key into local map";
    private static final String DXGW_ERROR_STATUS_CODE = "HttpStatus code for JWT public key pushing to DXGW: {}";
    private static final String DXGW_ERROR_RESPONSE = "Response body for JWT public key pushing to DXGW: {}";
    private static final String DXGW_SUCCESSFULLY_REGISTERED = "Successfully pushed JWT public key to DXGW with status code: {}";
    private static final String DEV_MODE_STATUS = "JWT Dev Mode is enabled: {}";


    @Value("${identity.identitybuilder.policycore.crypto.jwt.devMode.enabled:true}")
    private boolean devModeEnabled;

    private int dxgwResponseStatus = 0;

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());


    private final WebClient webClient;
    private final JwsService jwsService;
    public JwtPublicKeyPushService(WebClient webClient, JwsService jwsService) {
        this.webClient = webClient;
        this.jwsService = jwsService;
    }

    @OperationalAudit(eventType = OperationalEventType.JWT_PUBLIC_KEY_REGISTERED, mapperFactory = JwtPublicKeyRegisteredEventMapper.Factory.class)
    @EventStream(eventName = "JWT_PUBLIC_KEY_REGISTERED", customEventStreams = {
            @EventStream.CustomEventStream(type = EventStream.EventStreamType.AUDIT, emitters = "sdpv3", eventStreamDataBuilder = Cal1_5EventStreamDataBuilder.BUILDER_TYPE) })
    @Trace
    public JwtPublicKeyPushResponse pushPublicKey(JwtKeyModel jwtKeyModel){
        logger.info(DEV_MODE_STATUS, devModeEnabled);
        if(devModeEnabled){
            // store to jwsService local map
            jwsService.storeJwkToMap(jwtKeyModel.getKeyPairKid(),jwtKeyModel.getTransformedPublicJWK());
            dxgwResponseStatus = 200;
            return new JwtPublicKeyPushResponse(PUBLIC_KEY_STORED_TO_LOCAL_MAP,dxgwResponseStatus);
        }else{
            return pushToDXGW(jwtKeyModel.getKeyPairKid(), jwtKeyModel.getTransformedPublicJWK());
        }
    }

    protected JwtPublicKeyPushResponse pushToDXGW(String keyPairKid, String jwtPublicKey){
        String resultBody = this.webClient.put()
                .uri(KMS_URL, keyPairKid)
                .contentType(MediaType.APPLICATION_JSON)
                .header(HEADER_ACCPET,HEADER_ACCPET_VALUE)
                .body(BodyInserters.fromValue(jwtPublicKey))
                .exchangeToMono(
                        clientResponse -> (
                                clientResponse.bodyToMono(String.class)
                                        .doOnSuccess(body->{
                                            if(clientResponse.statusCode().isError()){
                                                logger.error(DXGW_ERROR_STATUS_CODE, clientResponse.statusCode());
                                                logger.error(DXGW_ERROR_RESPONSE, body);
                                            }else{
                                                logger.info(DXGW_SUCCESSFULLY_REGISTERED, clientResponse.statusCode());
                                            }
                                            dxgwResponseStatus = clientResponse.rawStatusCode();
                                        })
                        )
                )
                .block();
        return new JwtPublicKeyPushResponse(resultBody,dxgwResponseStatus);

    }


}
