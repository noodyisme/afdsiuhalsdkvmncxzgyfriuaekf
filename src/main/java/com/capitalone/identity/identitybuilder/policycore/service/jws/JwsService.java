package com.capitalone.identity.identitybuilder.policycore.service.jws;

import com.capitalone.chassis.engine.model.exception.ChassisBusinessException;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.model.event_models.JwkValidated;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.model.event_models.JwkValidated_Publisher;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.util.JSONObjectUtils;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import net.minidev.json.JSONObject;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import java.lang.invoke.MethodHandles;
import java.text.ParseException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static com.capitalone.identity.identitybuilder.policycore.service.jws.JWTExtractionException.JwsExceptionType.*;
import static java.time.Instant.now;
import static java.time.Instant.ofEpochSecond;
import static java.time.ZoneOffset.UTC;

@Service
public class JwsService {

    private static final String IAT = "iat";
    private static final String EXP = "exp";
    private static final String JWK_EXCHANGE_EXCEPTION = "JWK_EXCHANGE_EXCEPTION";
    private static final String PARSE_EXCEPTION_RESPONSE = "PARSE_EXCEPTION";
    private static final String JWT_INVALID_KEY_RESPONSE = "JWTInvalidKeyResponse";
    private static final String PROCESS_ID = "processID";
    private static final String JWT_KEY_PAIR_NOT_VALID = "JWT Key Pair is not valid";
    private static final String JWT_KEY_PAIR_EXPIRED = "JWT Key Pair is expired";
    private static final String JWT_PUBLIC_KEY_EXPIRED_STRING = "JWT public key is expired";
    private static final String JWT_SUCCESSFULLY_STORED_KEY_PAIR = "JWT Key Pair is stored successfully";
    private static final String SIGNED_JWT_IS_CREATED_SUCCESSFULLY = "Signed JWT is created successfully";
    private static final String JWT_ADDITIONAL_PAYLOAD_IS_NOT_EMPTY = "JWT Additional Payload is not empty, adding non-overlapping data to JWT Payload";
    private static final String JWT_ADDITIONAL_PAYLOAD_IS_EMPTY = "JWT Additional Payload is empty";
    private static final String JWT_USING_USER_DEFINED_TOKEN_TIME = "Using user defined jwtExpTimeInSecond: {} seconds";

    private static final String JWK_VALIDATION_PASSED = "JWK validation passed";
    private static final String UNABLE_TO_OBTAIN_JWK_FROM_EXCHANGE = "Unable to obtain JWK from Exchange";
    private static final String UNABLE_TO_OBTAIN_JWK_FROM_EXCHANGE_FOR_KID = "Unable to obtain JWK from Exchange for kid: {}";
    private static final String JWK_FOR_KID_IS_EXPIRED = "The JWK for kid %s is expired";
    private static final String JWK_FOR_KID_IS_EXPIRED_ERROR_MESSAGE = "The JWK for kid {} is expired";
    private static final String EXPECTED_PRODUCT_ID_DOES_NOT_MATCH = "ExpectedProductId does not match with productId in JWK";



    @Value("${env.gatewayURL}")
    private String env;
    @Value("${identity.identitybuilder.policycore.crypto.jwt.validateSourceProductId.enabled:true}")
    private boolean productIdValidationEnabledAtApplicationLevel;

    protected static final int JWT_TOKEN_EXP_TIME_LOWER_BOUND = 60;
    protected static final int JWT_TOKEN_EXP_TIME_UPPER_BOUND = 7200;


    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    static final String ENV_NON_PROD = "https://api-it.cloud.capitalone.com";
    static final String ENV_PROD = "https://api.cloud.capitalone.com";
    static final String KID_IS_NOT_AVAILABLE_SERVER_RESPONSE = "Kid %s is not available. Server response: %s";
    static final String REQUEST_NOT_UNDERSTOOD_SERVER_RESPONSE = "Request for %sis not understood. Server response: %s";
    static final String AUTHENTICATION_OR_AUTHORIZATION_SERVER_RESPONSE = "Request for %s failed due to authentication or authorization. Server response: %s";
    static final String OTHER_CLIENT_ERROR_SERVER_RESPONSE = "Request fo r%s failed with client error. Server response: %s";
    static final String SERVER_ERROR = "Not able to retrieve key for kid: %s, server response : %s";
    public static final String NOT_ABLE_TO_RETRIEVE_KEY_FOR_KID = "Not able to retrieve key for kid: %s";
    private final Map<String, String> kidToJWK = new ConcurrentHashMap<>();

    static final String EMPTY_TOKEN_EXP_TIME = "jwtTokenExpTimeInSecond is empty, setting it to default value {} second";
    static final String TOKEN_EXP_TIME_BELOW_LOWER_BOUND = "jwtTokenExpTimeInSecond is below threshold {} seconds, setting it to {} seconds";
    static final String TOKEN_EXP_TIME_ABOVE_UPPER_BOUND = "jwtTokenExpTimeInSecond is above threshold {} seconds, setting it to {} seconds";
    static final String PRODUCT_ID = "productId";

    private JwtKeyModel jwtKeyModel = null;

    private final JwtPublicKeyRetrievalService jwtPublicKeyRetrievalService;

    private final JwkValidated_Publisher jwkValidatedPublisher;

    public JwsService(JwtPublicKeyRetrievalService jwtPublicKeyRetrievalService, JwkValidated_Publisher jwkValidatedPublisher) {
        this.jwtPublicKeyRetrievalService = jwtPublicKeyRetrievalService;
        this.jwkValidatedPublisher = jwkValidatedPublisher;
    }

    public JwsServiceValidationResponse validate(String jwtToken,String expectedProductId, boolean validateProductId){
        if (Objects.isNull(jwtToken)) {
            throw new JWTExtractionException(JWT_NULL_TOKEN);
        }

        String payload = getJwtPayload(jwtToken);
        if (PARSE_EXCEPTION_RESPONSE.equals(payload)){
            throw new JWTExtractionException(JWT_MALFORMED_TOKEN);
        }
        Map<String, String> jwtPayload;
        try {
            jwtPayload = getPayloadAsMap(payload);

            if (!validateIssuedAt(jwtPayload.get(IAT))){
                throw new JWTExtractionException(JWS_INVALID_CLAIMS);
            }

            if (!validateExp(jwtPayload.get(EXP))){
                throw new JWTExtractionException(JWS_INVALID_CLAIMS);
            }

        } catch (JsonProcessingException e) {
            throw new JWTExtractionException(JWT_MALFORMED_TOKEN);
        }

        String kid = parseKeyId(jwtToken);

        if (null != kid && !JWT_INVALID_KEY_RESPONSE.equals(kid)) {
            String jwk = getJWK(kid);
            jwkValidCheck(kid, jwk, expectedProductId, validateProductId);
            if (verifyTokenSignature(jwk, jwtToken)) {
                return response();
            } else {
                logger.error("Token signature is not valid");
                throw new JWTExtractionException(JWT_INVALID_SIGNATURE);
            }
        } else {
            logger.error("kid header is not present in JWT");
            throw new JWTExtractionException(JWT_INVALID_KEY);
        }
    }

    private JwsServiceValidationResponse response() {
        return new JwsServiceValidationResponse();
    }

    boolean validateExp(String exp) {
        return  Objects.nonNull(exp) && ofEpochSecond(Long.parseLong(exp)).atZone(UTC).isAfter(now().atZone(UTC));
    }

    boolean validateIssuedAt(String iat) {
        return Objects.nonNull(iat) && ofEpochSecond(Long.parseLong(iat)).atZone(UTC).isBefore(now().atZone(UTC));

    }

    public void jwkValidCheck(String kid, String jwk, String expectedProductId, boolean validateProductId){
        final JWTExtractionException.JwsExceptionType exceptionType;
        if (JWK_EXCHANGE_EXCEPTION.equals(jwk)){
            logger.error(UNABLE_TO_OBTAIN_JWK_FROM_EXCHANGE_FOR_KID, kid);
            exceptionType = JWT_MISSING_CERTIFICATE;
            jwkValidatedPublisher.publishEvent(new JwkValidated(UNABLE_TO_OBTAIN_JWK_FROM_EXCHANGE,exceptionType,
                    productIdValidationEnabledAtApplicationLevel,validateProductId,null,expectedProductId));
            throw new JWTExtractionException(JWT_MISSING_CERTIFICATE);
        }
        if(JWT_PUBLIC_KEY_EXPIRED_STRING.equals(jwk)){
            logger.error(JWK_FOR_KID_IS_EXPIRED_ERROR_MESSAGE,kid);
            exceptionType = JWT_PUBLIC_KEY_EXPIRED;
            jwkValidatedPublisher.publishEvent(new JwkValidated(String.format(JWK_FOR_KID_IS_EXPIRED, kid),exceptionType,
                    productIdValidationEnabledAtApplicationLevel,validateProductId,null,expectedProductId));
            throw new JWTExtractionException(JWT_PUBLIC_KEY_EXPIRED);
        }
        final JSONObject jsonObject = parseJson(jwk);
        final String jwkProductId = getJwkProductId(jsonObject);
        if(productIdValidationEnabledAtApplicationLevel && validateProductId && !verifyProductIdMatches(jwkProductId, expectedProductId)){
            logger.error(EXPECTED_PRODUCT_ID_DOES_NOT_MATCH);
            exceptionType = JWT_PRODUCTID_MISMATCH;
            jwkValidatedPublisher.publishEvent(new JwkValidated(EXPECTED_PRODUCT_ID_DOES_NOT_MATCH,exceptionType,
                    productIdValidationEnabledAtApplicationLevel, true,jwkProductId,expectedProductId));
            throw new JWTExtractionException(JWT_PRODUCTID_MISMATCH);
        }
        if(!verifyJwkNotExpired(jsonObject)){
            // invalid the public key
            kidToJWK.put(kid, JWT_PUBLIC_KEY_EXPIRED_STRING);
            logger.error(JWK_FOR_KID_IS_EXPIRED_ERROR_MESSAGE,kid);
            exceptionType = JWT_PUBLIC_KEY_EXPIRED;
            jwkValidatedPublisher.publishEvent(new JwkValidated(String.format(JWK_FOR_KID_IS_EXPIRED, kid),exceptionType,
                    productIdValidationEnabledAtApplicationLevel,validateProductId,null,expectedProductId));
            throw new JWTExtractionException(JWT_PUBLIC_KEY_EXPIRED);
        }
        jwkValidatedPublisher.publishEvent(new JwkValidated(JWK_VALIDATION_PASSED,null,
                productIdValidationEnabledAtApplicationLevel,validateProductId,jwkProductId,expectedProductId));

    }

    @Nullable
    private String getJWK(String kid) {
        String jwk = kidToJWK.get(kid);
        if (null == jwk) {
            JwtPublicKeyRetrievalResponse jwtPublicKeyRetrievalResponse = jwtPublicKeyRetrievalService.fetchJwtPublicKey(kid);
            if(jwtPublicKeyRetrievalResponse.getJwk()==null){
                return JWK_EXCHANGE_EXCEPTION;
            }
            jwk = jwtPublicKeyRetrievalResponse.getJwk();
            kidToJWK.put(kid, jwk);
        }
        return jwk;
    }

    String parseKeyId(String aToken) {
        try {
            return SignedJWT.parse(aToken).getHeader().getKeyID();
        } catch (ParseException e) {
            logger.error("Error during JWT parse", e);
            return JWT_INVALID_KEY_RESPONSE;
        }
    }

    private JSONObject parseJson(final String jwk) {
        try {
            return JSONObjectUtils.parse(jwk);
        } catch (ParseException error) {
            throw new ChassisBusinessException("JWK Parse failed", error);
        }
    }

    private String getJwkProductId(JSONObject jsonObject){
        return String.valueOf(jsonObject.get(PRODUCT_ID));
    }

    private boolean verifyProductIdMatches(String jwkProductId, String expectedProductId){
        return expectedProductId != null && expectedProductId.equals(jwkProductId);
    }
    private boolean verifyJwkNotExpired(JSONObject jsonObject){
        String expVal = String.valueOf(jsonObject.get(EXP));
        long publicJwkExp = Long.parseLong(expVal);
        Instant expiredAt = Instant.ofEpochSecond(publicJwkExp);
        return !Instant.now().isAfter(expiredAt);
    }

    boolean verifyTokenSignature(String jwk, String aToken) {
        RSAKey rsaKey;
        JWSObject parsedJWS;
        JWSVerifier verifier;
        try {
            rsaKey = JWK.parse(jwk).toRSAKey();
            verifier = new RSASSAVerifier(rsaKey);
            parsedJWS = JWSObject.parse(aToken);
            return parsedJWS.verify(verifier);
        } catch (ParseException | JOSEException e) {
            logger.error("Error during JWK parse", e);
            return false;
        }
    }

    public Map<String, String> extractFields(String jwt, List<String> fields) {
        Map<String, String> jwtPayload = new HashMap<>();
        String payload = getJwtPayload(jwt);
        try {
            jwtPayload = getPayloadAsMap(payload);
        } catch (Exception e){
            logger.error("Error during JWT parse", e);
            throw new JWTExtractionException(JWT_MALFORMED_TOKEN);
        }
        if (fields.isEmpty()) {
            return jwtPayload;
        }
        Map<String, String> j = jwtPayload;
        return fields.stream().collect(HashMap::new, (m, v) -> m.put(v, j.get(v)), HashMap::putAll);
    }

    String getJwtPayload(String jwt) {
        JWSObject jwsObject;
        try {
            jwsObject = JWSObject.parse(jwt);
        } catch (ParseException e) {
            logger.error("Error occurred during JWT parse", e);
            throw new JWTExtractionException(JWT_MALFORMED_TOKEN);
        }
        return jwsObject.getPayload().toString();
    }

    public Map<String, String> getPayloadAsMap(String payload) throws JsonProcessingException {
        return new ObjectMapper().readValue(payload, new TypeReference<Map<String, String>>() {});
    }

    public static int getJwtTokenExpTimeUpperBound(){
        return JWT_TOKEN_EXP_TIME_UPPER_BOUND;
    }

    public String generate(String processId, Map<String,String> jwtAdditionalPayload, String tokenValidTime){
        JwtKeyModel currentKeyModel = jwtKeyModel;
        if(jwtKeyModel==null){
            throw new ChassisBusinessException(JWT_KEY_PAIR_NOT_VALID);
        }
        if(isKeyPairExpired(currentKeyModel.getKeyPairExp())){
            throw new ChassisBusinessException(JWT_KEY_PAIR_EXPIRED);
        }

        if(tokenValidTime==null){
            logger.info(EMPTY_TOKEN_EXP_TIME, JWT_TOKEN_EXP_TIME_UPPER_BOUND);
        }
        long tokenExpTime = tokenValidTime==null? JWT_TOKEN_EXP_TIME_UPPER_BOUND : Long.parseLong(tokenValidTime);

        return createSignedJWT(processId,jwtAdditionalPayload,tokenExpTime, currentKeyModel);
    }


    private String createSignedJWT(String processId, Map<String, String> jwtAdditionalPayload, long tokenExpTime, JwtKeyModel currentKeyModel){
        try {
            RSAKey jwtPrivateKey = currentKeyModel.getJwtKeyPair();
            JWSSigner signer = new RSASSASigner(jwtPrivateKey);
            JWTClaimsSet claimsSet = buildClaims(processId,jwtAdditionalPayload, tokenExpTime);

            SignedJWT signedJWT = new SignedJWT(
                    new JWSHeader.Builder(JWSAlgorithm.RS256)
                            .keyID(currentKeyModel.getKeyPairKid())
                            .build(),
                            claimsSet);
            signedJWT.sign(signer);
            logger.info(SIGNED_JWT_IS_CREATED_SUCCESSFULLY);
            return signedJWT.serialize();
        }catch(JOSEException error){
            throw new ChassisBusinessException(error.getMessage());
        }
    }

    // store the jwk to local map
    public void storeJwkToMap(String kid, String jwk){
        kidToJWK.put(kid,jwk);
        logger.info(JWT_SUCCESSFULLY_STORED_KEY_PAIR);
    }

    // store the key pair into memory
    public void storeKeyModel(JwtKeyModel jwtKeyModel){
        this.jwtKeyModel = jwtKeyModel;
        logger.info(JWT_SUCCESSFULLY_STORED_KEY_PAIR);
    }


    private JWTClaimsSet buildClaims(@NotNull String processId, Map<String,String> jwtAdditionalPayload, long tokenExpTime){
        JWTClaimsSet.Builder resultBuilder = new JWTClaimsSet.Builder();
        if(jwtAdditionalPayload!=null){
            logger.info(JWT_ADDITIONAL_PAYLOAD_IS_NOT_EMPTY);
            jwtAdditionalPayload.forEach(resultBuilder::claim);
        }else{
            logger.info(JWT_ADDITIONAL_PAYLOAD_IS_EMPTY);
        }
        if(tokenExpTime< JWT_TOKEN_EXP_TIME_LOWER_BOUND){
            logger.warn(TOKEN_EXP_TIME_BELOW_LOWER_BOUND, JWT_TOKEN_EXP_TIME_LOWER_BOUND, JWT_TOKEN_EXP_TIME_LOWER_BOUND);
            tokenExpTime = JWT_TOKEN_EXP_TIME_LOWER_BOUND;
        }else if(tokenExpTime> JWT_TOKEN_EXP_TIME_UPPER_BOUND){
            logger.warn(TOKEN_EXP_TIME_ABOVE_UPPER_BOUND, JWT_TOKEN_EXP_TIME_UPPER_BOUND, JWT_TOKEN_EXP_TIME_UPPER_BOUND);
            tokenExpTime = JWT_TOKEN_EXP_TIME_UPPER_BOUND;
        }else{
            logger.info(JWT_USING_USER_DEFINED_TOKEN_TIME,tokenExpTime);
        }
        resultBuilder
                .issueTime(Date.from(Instant.now()))
                .claim(PROCESS_ID, processId)
                .expirationTime(Date.from(Instant.now().plusSeconds(tokenExpTime)));
        return resultBuilder.build();
    }

    private boolean isKeyPairExpired(long keyPairExp){
        Instant expiredAt = Instant.ofEpochSecond(keyPairExp);
        return Instant.now().isAfter(expiredAt);
    }
}
