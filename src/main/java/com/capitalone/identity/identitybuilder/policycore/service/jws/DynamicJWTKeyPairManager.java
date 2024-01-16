package com.capitalone.identity.identitybuilder.policycore.service.jws;

import com.capitalone.chassis.engine.model.exception.ChassisBusinessException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.Algorithm;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.lang.invoke.MethodHandles;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;


@Service
public class DynamicJWTKeyPairManager {

    static final String RS256 = "RS256";
    private static final String EXP = "exp";
    private static final String PROD = "prod";
    static final String PRODUCT_ID = "productId";

    private static final String JWT_PARAMETERS_NOT_SET = "JWT required parameters not set in application properties in domain repository, required parameters are: identity.identitybuilder.policycore.crypto.jwt.productId";
    private static final String JWT_KEY_PAIR_OUT_OF_RANGE_LOWER_BOUND = "Domain Key Pair Expiration Time is set to {} days, overwriting it to lower bound {} days";
    private static final String JWT_KEY_PAIR_OUT_OF_RANGE_UPPER_BOUND = "Domain Key Pair Expiration Time is set to {} days, overwriting it to upper bound {} days";
    private static final String JWT_KEY_PAIR_EXPIRATION_TIME = "JWT Key Pair Expiration Time is set to {} days";
    private static final String JWT_GENERATION_DISABLED = "Jwt Key Pair Generation Disabled";
    private static final String JWT_PERFORMING_STATUS_CHECKING = "Performing Dynamic Key Rotation status checking";
    private static final String CREATING_NEW_KEY_PAIR = "Creating new JWT Key Pair";
    private static final String JWT_PUBLIC_KEY_PUSH_RESULT = "JWT public key push result: {}";
    private static final String DXGW_ERROR_RESPONSE = "DXGW error response detail: {}";
    private static final String JWT_GENERATED_KID = "Generated jwt kid: {}";
    private static final String JWT_REVERTING_HEALTH_CHECK = "Unable to push keys to DXGW, inverting health check";
    private static final String JWT_HEALTH_CHECK_AT_START_UP = "KeyPair Health Check At Start Up: {}";
    private static final String JWT_HEALTH_CHECK_FAILED = "JWT Key Pair is not valid, shutting down application";
    private static final String JWT_DOMAIN_PRODUCT_ID = "Domain's product Id is: {}";
    private static final String JWT_GENERATION_FOR_KEY_PAIR = "JWT Key Pair generation is enabled: {}";
    private static final String JWT_MISSING_PRODUCT_ID_AND_DISABLE_KEY_ROTATION = "Domain product Id is missing, exiting key pair generation and disabling key pair rotation";
    private static final String JWT_MISSING_PRODUCT_ID_AND_INVERTING_HEALTH_CHECK = "Domain product Id is missing, health check is required, shutting down application";
    private static final String JWT_PERIODIC_CHECK_RESULT = "JWT Key Pair is still valid for more than {} hours";
    private static final String JWT_KEY_PAIR_NOT_STORED = "JWT Key Pair is not stored because key registration is not successful";
    private static final String FAILED_TO_TRANSFORM_PUBLIC_JWK = "Failed to transform public JWK: ";
    private static final String FAILED_TO_CREATE_KEY_MODEL = "Unable to create new key model: ";

    // expiration time range: 3-7
    @Value("${identity.identitybuilder.policycore.crypto.jwt.keyPair.expirationTimeInDays:7}")
    private int jwtKeyPairExpirationTimeInDays;
    @Value("${identity.identitybuilder.policycore.crypto.jwt.productId:#{null}}")
    private String jwtProductId;
    @Value(("${identity.identitybuilder.policycore.crypto.jwt.keyPair.generateAndRuntimeRotation:false}"))
    private boolean jwtKeyPairEnabled;
    @Value(("${identity.identitybuilder.policycore.crypto.jwt.keyPair.requireHealthCheckAtStartUp:false}"))
    private boolean jwtKeyPairHealthCheckAtStartUp;

    private static final int KEYPAIR_GENERATION_FAILURE_RETRY_STARTUP = 3;

    // health check (Initially set to true so that does not block domain that are not using JWT component)
    private boolean isKeyPairReady = true;

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    // default to 4 hours in ms
    // perform rotation checking based on this interval
    private long rotationInterval = 14400000;
    // default to 48 hours
    // usage: perform rotation if key is expiring in this offset hour
    private int rotationExpiringOffsetInHours = 48;

    private static final int KEY_PAIR_EXP_LOWER_BOUND = 3;
    private static final int KEY_PAIR_EXP_UPPER_BOUND = 7;

    private int currentKeyStatus = 0;
    private long currentKeyPairExp = 0;

    private final JwsService jwsService;
    private final JwtPublicKeyPushService jwtPublicKeyPushService;
    public DynamicJWTKeyPairManager(Environment environment, JwsService jwsService, JwtPublicKeyPushService jwtPublicKeyPushService) {
        this.jwsService = jwsService;
        this.jwtPublicKeyPushService = jwtPublicKeyPushService;
        boolean isProdEnvironment = Arrays.stream(environment.getActiveProfiles())
                .map(String::toLowerCase)
                .anyMatch(o -> o.contains(PROD));
        if(!isProdEnvironment){
            // qa interval: every 5 minutes
            // qa rotation offset: 12 hours
            rotationInterval = 300000;
            rotationExpiringOffsetInHours = 12;
        }
    }

    // only allow 3 days to 7 days
    private void keyPairExpirationRangeCheck(){
        if(jwtKeyPairExpirationTimeInDays < KEY_PAIR_EXP_LOWER_BOUND){
            logger.warn(JWT_KEY_PAIR_OUT_OF_RANGE_LOWER_BOUND,jwtKeyPairExpirationTimeInDays,KEY_PAIR_EXP_LOWER_BOUND);
            jwtKeyPairExpirationTimeInDays = KEY_PAIR_EXP_LOWER_BOUND;
        }else if(jwtKeyPairExpirationTimeInDays > KEY_PAIR_EXP_UPPER_BOUND){
            logger.warn(JWT_KEY_PAIR_OUT_OF_RANGE_UPPER_BOUND,jwtKeyPairExpirationTimeInDays,KEY_PAIR_EXP_UPPER_BOUND);
            jwtKeyPairExpirationTimeInDays = KEY_PAIR_EXP_UPPER_BOUND;
        }
        logger.info(JWT_KEY_PAIR_EXPIRATION_TIME, jwtKeyPairExpirationTimeInDays);
    }

    private boolean isKeyPairExpiring(long keyPairExp, int offSetHours){
        Instant expiredAt = Instant.ofEpochSecond(keyPairExp);
        return Instant.now().isAfter(expiredAt.minus(offSetHours, ChronoUnit.HOURS));
    }

    private RSAKey generateRSAKey(String keyPairKid){
        try {
            return new RSAKeyGenerator(2048)
                    .keyUse(KeyUse.SIGNATURE)
                    .keyID(keyPairKid)
                    .algorithm(new Algorithm(RS256))
                    .generate();
        }catch (JOSEException error){
            throw new ChassisBusinessException(error.getMessage());
        }
    }

    public String transformJWK(String publicJWK, long exp, String productId, int jwtTokenExpTimeUpperBound){
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            Map<String, String> map = objectMapper.readValue(publicJWK, new TypeReference<Map<String, String>>() {});
            // Upper bound token valid time + 10 second between signer and validator
            // Example: Signer node private key expires in 2 hours, then validator public key will expire in 2 hours + token valid time + 10 seconds
            map.put(EXP, String.valueOf(exp + jwtTokenExpTimeUpperBound + 10));
            map.put(PRODUCT_ID, productId);
            return objectMapper.writeValueAsString(map);
        }catch (JsonProcessingException error){
            throw new ChassisBusinessException(FAILED_TO_TRANSFORM_PUBLIC_JWK, error);
        }
    }

    private JwtKeyModel createKeyModel(){
        if(jwtProductId==null){
            logger.error(JWT_PARAMETERS_NOT_SET);
            throw new ChassisBusinessException(JWT_PARAMETERS_NOT_SET);
        }
        String keyPairKid = UUID.randomUUID().toString();
        logger.info(JWT_GENERATED_KID, keyPairKid);
        long keyPairExp = Instant.now().plus(jwtKeyPairExpirationTimeInDays,ChronoUnit.DAYS).getEpochSecond();

        RSAKey rsaKeyPair = generateRSAKey(keyPairKid);

        String originalJWK = rsaKeyPair.toPublicJWK().toJSONString();
        String transformedPublicJWK = transformJWK(originalJWK, keyPairExp, jwtProductId, JwsService.getJwtTokenExpTimeUpperBound());

        return JwtKeyModel.builder()
                .jwtKeyPair(rsaKeyPair)
                .keyPairExp(keyPairExp)
                .transformedPublicJWK(transformedPublicJWK)
                .keyPairKid(keyPairKid)
                .build();
    }

    public void dynamicKeyRotation(){
        if(!jwtKeyPairEnabled) {
            logger.info(JWT_GENERATION_DISABLED);
            return;
        }
        logger.info(JWT_PERFORMING_STATUS_CHECKING);
        if(currentKeyStatus !=200 || isKeyPairExpiring(currentKeyPairExp, rotationExpiringOffsetInHours)){
            logger.info(CREATING_NEW_KEY_PAIR);
            JwtKeyModel newJwtKeyModel = null;
            try{
                newJwtKeyModel = createKeyModel();
            }catch(Exception error) {
                logger.error(FAILED_TO_CREATE_KEY_MODEL , error);
                return;
            }
            JwtPublicKeyPushResponse response = jwtPublicKeyPushService.pushPublicKey(newJwtKeyModel);
            logger.info(JWT_PUBLIC_KEY_PUSH_RESULT, response.getPushResponse());
            if(response.getRawHttpStatusCode()==200){
                currentKeyStatus=200;
                currentKeyPairExp = newJwtKeyModel.getKeyPairExp();
                jwsService.storeKeyModel(newJwtKeyModel);
            }else{
                logger.info(JWT_KEY_PAIR_NOT_STORED);
            }
        }else{
            logger.info(JWT_PERIODIC_CHECK_RESULT, rotationExpiringOffsetInHours);
        }
    }

    public boolean isKeyPairReady(){
        return isKeyPairReady;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void generateJWTKeyPairAfterStartUp(){
        logger.info(JWT_GENERATION_FOR_KEY_PAIR, jwtKeyPairEnabled);
        if(jwtProductId==null || !jwtKeyPairEnabled){
            logger.info(JWT_MISSING_PRODUCT_ID_AND_DISABLE_KEY_ROTATION);
            jwtKeyPairEnabled=false;
            if(jwtKeyPairHealthCheckAtStartUp){
                logger.info(JWT_MISSING_PRODUCT_ID_AND_INVERTING_HEALTH_CHECK);
                isKeyPairReady = false;
                throw new ChassisBusinessException(JWT_HEALTH_CHECK_FAILED);
            }
            return;
        }
        keyPairExpirationRangeCheck();
        logger.info(JWT_DOMAIN_PRODUCT_ID, jwtProductId);
        logger.info(JWT_HEALTH_CHECK_AT_START_UP, jwtKeyPairHealthCheckAtStartUp);
        logger.info(CREATING_NEW_KEY_PAIR);
        JwtKeyModel newJwtKeyModel = createKeyModel();
        JwtPublicKeyPushResponse response = jwtPublicKeyPushService.pushPublicKey(newJwtKeyModel);

        //retry up to 2 more times
        for(int i = 1; i< KEYPAIR_GENERATION_FAILURE_RETRY_STARTUP; i++){
            if(response.getRawHttpStatusCode() !=200){
                newJwtKeyModel = createKeyModel();
                response = jwtPublicKeyPushService.pushPublicKey(newJwtKeyModel);
            }
        }
        currentKeyStatus = response.getRawHttpStatusCode();
        logger.info(JWT_PUBLIC_KEY_PUSH_RESULT, response.getPushResponse());
        if(response.getRawHttpStatusCode() == 200){
            currentKeyStatus=200;
            currentKeyPairExp = newJwtKeyModel.getKeyPairExp();
            jwsService.storeKeyModel(newJwtKeyModel);
        }
        else{
            logger.error(DXGW_ERROR_RESPONSE, response.getPushResponse());
            if(jwtKeyPairHealthCheckAtStartUp){
                logger.error(JWT_REVERTING_HEALTH_CHECK);
                isKeyPairReady = false;
                throw new ChassisBusinessException(JWT_HEALTH_CHECK_FAILED);
            }
        }
    }

    public long getDelay(){
        return rotationInterval;
    }
}

