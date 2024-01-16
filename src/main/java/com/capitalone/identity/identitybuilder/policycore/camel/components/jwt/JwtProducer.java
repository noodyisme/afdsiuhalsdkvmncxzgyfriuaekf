package com.capitalone.identity.identitybuilder.policycore.camel.components.jwt;

import com.capitalone.chassis.engine.model.exception.ChassisBusinessException;
import com.capitalone.identity.identitybuilder.policycore.camel.PolicyConstants;
import com.capitalone.identity.identitybuilder.policycore.service.jws.JWTExtractionException;
import com.capitalone.identity.identitybuilder.policycore.service.jws.JwsService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.support.DefaultProducer;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.HashMap;

import static com.capitalone.identity.identitybuilder.policycore.service.jws.JWTExtractionException.JwsExceptionType.JWT_MALFORMED_TOKEN;
import static java.util.Collections.emptyList;

public class JwtProducer extends DefaultProducer {

    private final JwtEndpoint endpoint;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final String JWT_ADDITIONAL_PAYLOAD_PARSE_ERROR = "Unable to parse object from jwtAdditionalPayload Header";
    private static final String JWT_ADDITIONAL_PAYLOAD = "jwtAdditionalPayload";
    private static final String CACHE_KEY = "cacheKey";
    private static final String JWT_EXP_TIME_IN_SECOND = "jwtExpTimeInSecond";
    private static final String JWT_EXPECTED_PRODUCT_ID = "jwtExpectedProductId";

    public JwtProducer(JwtEndpoint endpoint) {
        super(endpoint);
        this.endpoint = endpoint;
    }

    @Override
    public void process(Exchange exchange) throws Exception {
        Optional<String> requestedSrcTokenHeader = Optional.ofNullable(this.endpoint.getSrcTokenHeader());
        Optional<String> requestedSrcTokenPath = Optional.ofNullable(this.endpoint.getSrcTokenPath());
        String fieldsToParse = Optional.ofNullable(this.endpoint.getFieldsToExtract()).map(String::trim).orElse("");
        List<String> fieldsToExtract = fieldsToParse.isEmpty() ? emptyList() : Arrays.asList(this.endpoint.getFieldsToExtract().trim().split(","));
        boolean validateProductId = Optional.ofNullable(this.endpoint.getValidateProductId()).orElse(true);

        JwsService jwsService = this.endpoint.getJwsService();

        if (endpoint.getJwtOperation().equals(JwtOperation.GENERATE)) {
            doGenerate(exchange, jwsService);
            return;
        }

        if (!requestedSrcTokenHeader.isPresent() && !requestedSrcTokenPath.isPresent()) {
            String body = exchange.getIn().getBody(String.class);
            doProcessJwtOperation(exchange, fieldsToExtract, validateProductId, jwsService, body);
            return;
        }
        if (requestedSrcTokenHeader.isPresent() && requestedSrcTokenPath.isPresent()){
            requestedSrcTokenHeader.ifPresent( tokenHeader -> requestedSrcTokenPath.ifPresent(tokenPath ->
                    processTokenFromJson(exchange, fieldsToExtract, validateProductId, jwsService, tokenPath,
                    jsonStringToMap(exchange.getIn().getHeader(tokenHeader, String.class)))));
            return;
        }
        if (requestedSrcTokenHeader.isPresent()) {
            requestedSrcTokenHeader.ifPresent( tokenHeader ->
                    doProcessJwtOperation(exchange, fieldsToExtract, validateProductId, jwsService, exchange.getIn()
                    .getHeader(requestedSrcTokenHeader.get(), String.class)));
            return;
        }
        requestedSrcTokenPath.ifPresent( tokenPath -> processTokenFromJson(exchange, fieldsToExtract, validateProductId, jwsService,
                tokenPath, exchange.getIn().getBody(Map.class)));
    }

    public Map jsonStringToMap(String json) {
        try {
            return objectMapper.readValue(json, Map.class);
        } catch (JsonProcessingException e){
            throw new JWTExtractionException(JWT_MALFORMED_TOKEN);
        }
    }

    private void doProcessJwtOperation(Exchange exchange, List<String> fieldsToExtract, boolean validateProductId, JwsService jwsService, String source) {
        if (endpoint.getJwtOperation().equals(JwtOperation.VALIDATE)) {
            doValidate(exchange, validateProductId, jwsService, source);
        } else if (endpoint.getJwtOperation().equals(JwtOperation.EXTRACT)) {
            doExtract(exchange, fieldsToExtract, validateProductId, jwsService, source);
        }
    }

    private void processTokenFromJson(Exchange exchange, List<String> fieldsToExtract, boolean validateProductId, JwsService jwsService, String tokenPath, Map jsonObjectAsAMap) {
        String jsonString;
        try {
            jsonString = objectMapper.writeValueAsString(jsonObjectAsAMap);
        } catch (Exception e) {
            throw new JWTExtractionException(JWT_MALFORMED_TOKEN);
        }
        String jwtTokenToProcess;
        try {
            jwtTokenToProcess = JsonPath.read(jsonString, tokenPath);
        } catch (Exception e){
            throw new JWTExtractionException(JWT_MALFORMED_TOKEN);
        }
        doProcessJwtOperation(exchange, fieldsToExtract, validateProductId, jwsService, jwtTokenToProcess);
    }

    private void doGenerate(Exchange exchange, JwsService jwsService){
        Message message = exchange.getMessage();
        String processID = message.getHeader(CACHE_KEY, String.class);
        Map<String, String> jwtAdditionalPayload = null;
        if(message.getHeader(JWT_ADDITIONAL_PAYLOAD)!=null){
            String val = message.getHeader(JWT_ADDITIONAL_PAYLOAD,String.class);
            try {
                TypeReference<HashMap<String,String>> typeRef
                        = new TypeReference<HashMap<String,String>>() {};
                jwtAdditionalPayload = objectMapper.readValue(val,typeRef);
            } catch (JsonProcessingException e) {
                throw new ChassisBusinessException(JWT_ADDITIONAL_PAYLOAD_PARSE_ERROR);
            }
        }
        String jwtExpTimeInSecond = message.getHeader(JWT_EXP_TIME_IN_SECOND, String.class);
        exchange.getMessage()
                .setHeader(PolicyConstants.JWT_SIGNED_TOKEN,
                        jwsService.generate(processID, jwtAdditionalPayload,jwtExpTimeInSecond));
    }

    private void doExtract(Exchange exchange, List<String> fieldsToExtract, boolean validateProductId, JwsService jwsService, String token) {
        doValidate(exchange, validateProductId, jwsService, token);
        exchange.getMessage()
                .setHeader(PolicyConstants.JWT_EXTRACTED_FIELDS, jwsService.extractFields(token, fieldsToExtract));
    }

    private void doValidate(Exchange exchange, boolean validateProductId, JwsService jwsService, String token) {
        String expectedProductId = exchange.getMessage().getHeader(JWT_EXPECTED_PRODUCT_ID, String.class);
        exchange.getMessage()
                .setHeader(PolicyConstants.JWT_TOKEN_VALIDATION_RESULT,
                        jwsService.validate(token, expectedProductId, validateProductId).result());
    }
}
