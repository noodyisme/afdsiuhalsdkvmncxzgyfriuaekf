package com.capitalone.identity.identitybuilder.policycore.service.dao.impl;

import com.capitalone.chassis.cal1_5.builder.Cal1_5EventStreamDataBuilder;
import com.capitalone.chassis.engine.annotations.stream.EventStream;
import com.capitalone.chassis.engine.core.base.AbstractBaseService;
import com.capitalone.identity.identitybuilder.policycore.feature.prerelease.PreRelease;
import com.capitalone.identity.identitybuilder.policycore.model.DevExchangeRequest;
import com.capitalone.identity.identitybuilder.policycore.model.DevExchangeResponse;
import com.capitalone.identity.identitybuilder.policycore.model.ErrorInfo;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.OperationalAudit;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.mapper.PipEvaluatedEventMapper;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.model.type.OperationalEventType;
import com.capitalone.identity.identitybuilder.policycore.service.constants.ApplicationConstants;
import com.capitalone.identity.identitybuilder.policycore.service.dao.DevExchangeDAO;
import com.capitalone.identity.identitybuilder.policycore.service.util.JsonUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.newrelic.api.agent.Trace;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import static com.capitalone.identity.identitybuilder.policycore.service.constants.ApplicationConstants.CLIENT_PROPS_MASK_PREFIX;
import static com.capitalone.identity.identitybuilder.policycore.service.dao.impl.DevExchangeDAOImpl.reformatJSON;

/**
 * This class exists because of audit requirements to log outgoing DevExchange calls.
 *
 * @author peg304
 */
@Component
@Primary
@ConditionalOnProperty({
        "identitybuilder.policycore.feature.prerelease.enabled",
        DevExchangeDAOWebImpl.WEB_CLIENT_ENABLED_FLAG
})
@PreRelease
public class DevExchangeDAOWebImpl extends AbstractBaseService implements DevExchangeDAO {

    private final DevExchangeWebClientFactory devExchangeWebClientFactory;

    public static final String WEB_CLIENT_ENABLED_FLAG = "identity.identitybuilder.policycore.service.webclient.enabled";

    public static final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${logs.fieldstofilter}")
    private String fieldsToFilter;

    @Autowired
    private PartnerTokenWebClientGeneratorFactory tokenGeneratorFactory;

    private final Environment env;
    private final DevExchangeDAOImpl.LogSetting logSetting = DevExchangeDAOImpl.LogSetting.fromProperty();

    /**
     * Implicit injection via constructor
     *
     * @param devExchangeWebClientFactory updates OAuth2RestTemplate based on service/timeout
     * @param env                         gets fields to filter based on application property
     */
    public DevExchangeDAOWebImpl(DevExchangeWebClientFactory devExchangeWebClientFactory, Environment env) {
        this.devExchangeWebClientFactory = devExchangeWebClientFactory;
        this.env = env;
    }

    @Override
    @OperationalAudit(eventType = OperationalEventType.PIP_EVALUATED, mapperFactory = PipEvaluatedEventMapper.Factory.class)
    @EventStream(eventName = ApplicationConstants.POLICY_CORE_DOWNSTREAM_API_CALL, customEventStreams = {
            @EventStream.CustomEventStream(type = EventStream.EventStreamType.AUDIT, emitters = "sdpv3", eventStreamDataBuilder = Cal1_5EventStreamDataBuilder.BUILDER_TYPE)})
    @Trace
    public DevExchangeResponse invokeDxAPI(DevExchangeRequest devexchangeRequest) {
        DevExchangeResponse dxResponse = new DevExchangeResponse();
        String correlationId = (null != devexchangeRequest.getHttpHeaders()) ? devexchangeRequest.getHttpHeaders().getFirst(ApplicationConstants.CLIENT_CORRELATION_ID) : StringUtils.EMPTY;
        String responseStr;
        int httpStatus;
        HttpHeaders headers = null;
        try {
            generatePartnerToken(devexchangeRequest);
            WebClient webClient = devExchangeWebClientFactory.getDevExchangeWebClient(devexchangeRequest.getServiceName(), devexchangeRequest.getScheme());
            if (logger.isInfoEnabled() && logSetting != DevExchangeDAOImpl.LogSetting.OFF) {
                logRequestAsCurl(devexchangeRequest);
            }

            long start = System.currentTimeMillis();
            ResponseEntity<String> response = callExchange(webClient, devexchangeRequest);
            timeRequest(start, response);
            responseStr = StringUtils.isNotBlank(response.getBody()) ? response.getBody() : "{}";
            httpStatus = response.getStatusCodeValue();
            headers = response.getHeaders();
        } catch (WebClientResponseException ex) {
            headers = ex.getHeaders();
            httpStatus = ex.getRawStatusCode();
            String serviceName = devexchangeRequest.getServiceName();
            String exceptionResponseBodyString = ex.getResponseBodyAsString();
            // Modify the response if its not valid JSON.
            if (headers.getContentType() == null || !String.valueOf(headers.getContentType()).contains("json")) {
                logger.error("BACKEND SERVICE INVALID RESPONSE: {} headers: {} body: {} CorrelationId: {}",
                        serviceName, headers.toSingleValueMap(),
                        exceptionResponseBodyString, correlationId);
                responseStr = DevExchangeDAOImpl.errorInfoToString(
                        new ErrorInfo("200099",
                                String.format("Invalid response from API: %s", devexchangeRequest.getServiceName()),
                                null));
            } else {
                logger.error("DevExchange Service Error Response: {} [Status: {}, Headers: {}, Body: {}] CorrelationId: {}",
                        serviceName, httpStatus,
                        headers.toSingleValueMap(),
                        exceptionResponseBodyString,
                        correlationId);

                responseStr = exceptionResponseBodyString;
            }


        } catch (WebClientException ex) {
            // Make sure this is JSON
            String serviceName = devexchangeRequest.getServiceName();
            logger.error(String.format("BACKEND SERVICE UNAVAILABLE: %s CorrelationId: %s",
                            serviceName, correlationId),
                    ex);
            httpStatus = HttpStatus.SERVICE_UNAVAILABLE.value();
            responseStr = DevExchangeDAOImpl.errorInfoToString(
                    new ErrorInfo("200003", String.format("System is not available: %s Exception Cause: %s",
                            serviceName, exceptionCause(ex)), null));
        }

        dxResponse.setHttpStatus(httpStatus);
        dxResponse.setHttpHeaders(headers != null ? headers : new HttpHeaders());
        dxResponse.setDxResponse(responseStr);
        filterFields(dxResponse, responseStr);
        return dxResponse;
    }

    @Trace
    private ResponseEntity<String> callExchange(WebClient webClient, DevExchangeRequest devExchangeRequest) {
        return webClient
                .method(devExchangeRequest.getHttpMethod())
                .uri(devExchangeRequest.getUri())
                .headers(httpHeaders -> httpHeaders.addAll(devExchangeRequest.getHttpHeaders()))
                .bodyValue(devExchangeRequest.getRequestBody())
                .retrieve()
                .toEntity(String.class)
                .block();
    }


    @Trace
    private void timeRequest(long start, ResponseEntity<String> response) {
        long elapsed = System.currentTimeMillis() - start;
        if (logger.isInfoEnabled() && logSetting != DevExchangeDAOImpl.LogSetting.OFF) {
            logResponse(response, elapsed);
        }
    }

    @Trace
    private void filterFields(DevExchangeResponse dxResponse, String responseStr) {
        String clientPropId2Mask = CLIENT_PROPS_MASK_PREFIX;
        String clientProps2Mask = env.getProperty(clientPropId2Mask);
        logger.debug("clientPropsId2Mask: {}, clientProps2Mask: {}", clientPropId2Mask, clientProps2Mask);
        if (StringUtils.isNotBlank(clientProps2Mask)) {
            fieldsToFilter += clientProps2Mask;
        }
        logger.debug("fieldsToFilter: {}", fieldsToFilter);
        dxResponse.setDxResponseAudit(JsonUtil.getFilteredResponse(responseStr, fieldsToFilter, "dxService"));
        logger.debug("dxResponse With Audit fields: {}", dxResponse.getDxResponseAudit());
    }

    private void generatePartnerToken(DevExchangeRequest devexchangeRequest) {
        // Check if the request is through partner gateway, generate partner token and set the token as a http header in the request.
        if (DevExchangeDAOImpl.Scheme.PDX.name().equalsIgnoreCase(devexchangeRequest.getScheme())) {
            PartnerWebClientTokenGenerator tokenGenerator = tokenGeneratorFactory.getPartnerTokenGenerator(devexchangeRequest.getServiceName());
            if (tokenGenerator != null) {
                tokenGenerator.generateToken(devexchangeRequest);
            }
        }
    }

    /**
     * Returns a descriptive string based on the type of exception that
     * caused the client exception.
     *
     * @param e the exception to examine
     * @return the descriptive string for the cause of the exception
     * (defaults to an empty string)
     */
    @Trace
    private String exceptionCause(WebClientException e) {
        // connection + read timeout
        if (e.getCause() instanceof java.net.SocketTimeoutException) {
            return "socketTimedOut";
        } else if (e.getCause() instanceof java.net.ConnectException) {
            return "connectionTimedOut";
        } else if (e.getCause() instanceof javax.net.ssl.SSLException) {
            return "sslException";
        } else return "";
    }

    /**
     * Logs the DevExchange request in cURL format.
     * <p>
     * Because the bearer token is handled at a lower level and is inaccessible to us,
     * a placeholder header is added to remind the user to supply the token if targeting
     * a server through the DevExchange gateway (as opposed to internal testing).
     * <p>
     * <b>Note</b>: This is intended for use in a local development environment.  If used
     * in any environment other than devint-test, the body of the request will be sanitized
     * to the audit log version and may not accurately reflect the request.
     *
     * @param req the request to log in a cURL request format
     */
    @Trace
    private void logRequestAsCurl(DevExchangeRequest req) {
        // Add in an authorization header as a placeholder since we don't know the token or
        // even whether it is needed (e.g., if we are hitting a service directly).
        Map<String, String> headers = new TreeMap<>(req.getHttpHeaders().toSingleValueMap());
        headers.put("Authorization", "Bearer replace-this-with-token-if-using-DevExchange-endpoint");

        StringBuilder sb = new StringBuilder("\nDevExchange request (formatted as cURL):\n");
        sb.append("curl -k -X ").append(req.getHttpMethod()).append(" \\\n");
        sb.append("  ").append(req.getUri());
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            sb.append(" \\\n  -H '").append(entry.getKey()).append(": ").append(entry.getValue()).append("'");
        }

        // If there are tabs in the body we run into problems when pasting it into
        // a shell, as the tab is interpreted as an auto-complete request. We'll convert
        // tabs to spaces to avoid this.
        if (req.getHttpMethod() == HttpMethod.POST || req.getHttpMethod() == HttpMethod.PUT) {
            String body;
            if (req.getRequestBody() instanceof String) {
                body = Arrays.stream(env.getActiveProfiles()).anyMatch(
                        e -> e.equalsIgnoreCase("devint-test")) ? req.getRequestBody().toString() : req.getRequestBodyAudit();
                body = (logSetting == DevExchangeDAOImpl.LogSetting.PRETTY) ? reformatJSON(body) : body;
                body = body.replace("\t", "    ");

            } else {
                Optional<String> className = Optional.ofNullable(req.getRequestBody()).map(Object::getClass).map(Object::toString);
                body = String.format("<<%s>>", className.orElse("null"));
            }
            if (!StringUtils.isEmpty(body)) {
                sb.append(" \\\n  -d '").append(body).append("'");
            }
        }
        sb.append("\n");

        if (logger.isInfoEnabled()) {
            logger.info(sb.toString());
        }
    }

    /**
     * Logs the DevExchange response (assuming logging is turned on).
     * <p>
     * <b>Note</b>: This is intended for use in a local development environment.
     *
     * @param response the response
     * @param elapsed  the response time, in milliseconds
     */
    @Trace
    private void logResponse(ResponseEntity<String> response, long elapsed) {
        boolean prettyPrint = logSetting == DevExchangeDAOImpl.LogSetting.PRETTY || logSetting == DevExchangeDAOImpl.LogSetting.PRETTYRESP;
        String responseStr = StringUtils.isNotBlank(response.getBody()) ? response.getBody() : "{}";
        if (logger.isInfoEnabled()) {
            logger.info("DevExchange response  status= {}  elapsed= {} ms  headers= {}  body= {}",
                    response.getStatusCodeValue(),
                    elapsed,
                    response.getHeaders().toSingleValueMap(),
                    prettyPrint ? reformatJSON(responseStr) : responseStr);
        }
    }
}





/*
 * Copyright 2018 Capital One Financial Corporation All Rights Reserved.
 *
 * This software contains valuable trade secrets and proprietary information of
 * Capital One and is protected by law. It may not be copied or distributed in
 * any form or medium, disclosed to third parties, reverse engineered or used in
 * any manner without prior written authorization from Capital One.
 */