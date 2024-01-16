package com.capitalone.identity.identitybuilder.policycore.service.dao.impl;

import com.capitalone.chassis.cal1_5.builder.Cal1_5EventStreamDataBuilder;
import com.capitalone.chassis.engine.annotations.stream.EventStream;
import com.capitalone.chassis.engine.core.base.AbstractBaseService;
import com.capitalone.identity.identitybuilder.policycore.model.DevExchangeRequest;
import com.capitalone.identity.identitybuilder.policycore.model.DevExchangeResponse;
import com.capitalone.identity.identitybuilder.policycore.model.ErrorInfo;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.OperationalAudit;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.mapper.PipEvaluatedEventMapper;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.model.type.OperationalEventType;
import com.capitalone.identity.identitybuilder.policycore.service.constants.ApplicationConstants;
import com.capitalone.identity.identitybuilder.policycore.service.dao.DevExchangeDAO;
import com.capitalone.identity.identitybuilder.policycore.service.util.JsonUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.newrelic.api.agent.Trace;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.*;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import static com.capitalone.identity.identitybuilder.policycore.service.constants.ApplicationConstants.CLIENT_PROPS_MASK_PREFIX;

/**
 * This class exists because of audit requirements to log outgoing DevExchange calls.
 *
 * @author peg304
 * @deprecated
 */
@Component
@Deprecated
public class DevExchangeDAOImpl extends AbstractBaseService implements DevExchangeDAO {

    private final DevExchangeRestTemplateFactory devExchangeRestTemplateFactory;

    public static final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${logs.fieldstofilter}")
    private String fieldsToFilter;

    @Autowired
    private PartnerTokenGeneratorFactory tokenGeneratorFactory;

    private final Environment env;
    private final LogSetting logSetting = LogSetting.fromProperty();

    /**
     * Implicit injection via constructor
     *
     * @param devExchangeRestTemplateFactory updates OAuth2RestTemplate based on service/timeout
     * @param env                            gets fields to filter based on application property
     */
    public DevExchangeDAOImpl(DevExchangeRestTemplateFactory devExchangeRestTemplateFactory, Environment env) {
        this.devExchangeRestTemplateFactory = devExchangeRestTemplateFactory;
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
        int httpStatus;
        String responseStr;
        HttpHeaders headers = null;

        try {
            generatePartnerToken(devexchangeRequest);
            OAuth2RestTemplate oAuth2RestTemplate = devExchangeRestTemplateFactory.getDevExchangeRestTemplate(devexchangeRequest.getServiceName(), devexchangeRequest.getScheme());
            if (logger.isInfoEnabled() && logSetting != LogSetting.OFF) {
                logRequestAsCurl(devexchangeRequest);
            }
            long start = System.currentTimeMillis();
            ResponseEntity<String> response = callExchange(oAuth2RestTemplate, devexchangeRequest);
            timeRequest(start, response);
            responseStr = StringUtils.isNotBlank(response.getBody()) ? response.getBody() : "{}";
            httpStatus = response.getStatusCodeValue();
            headers = response.getHeaders();
        } catch (HttpStatusCodeException httpex) {
            headers = Optional.ofNullable(httpex.getResponseHeaders()).orElse(new HttpHeaders());
            httpStatus = httpex.getRawStatusCode();
            String serviceName = devexchangeRequest.getServiceName();
            String exceptionResponseBodyString = httpex.getResponseBodyAsString();
            // Modify the response if its not valid JSON.
            if (headers.getContentType() == null || !String.valueOf(headers.getContentType()).contains("json")) {
                logger.error("BACKEND SERVICE INVALID RESPONSE: {} headers: {} body: {} CorrelationId: {}",
                        serviceName, headers.toSingleValueMap(),
                        exceptionResponseBodyString, correlationId);
                responseStr = errorInfoToString(
                        new ErrorInfo("200099", String.format("Invalid response from API: %s", serviceName),
                                httpex.getClass().getSimpleName() + ": " + httpex.getMessage()));
            } else {

                logger.error("DevExchange Service Error Response: {} [Status: {}, Headers: {}, Body: {}] CorrelationId: {}",
                        serviceName, httpStatus,
                        headers.toSingleValueMap(),
                        exceptionResponseBodyString,
                        correlationId);
                responseStr = exceptionResponseBodyString;

            }
        } catch (RestClientException restClientException) {
            // Make sure this is JSON
            String serviceName = devexchangeRequest.getServiceName();
            logger.error(String.format("BACKEND SERVICE UNAVAILABLE: %s CorrelationId: %s",
                            serviceName, correlationId),
                    restClientException);
            httpStatus = HttpStatus.SERVICE_UNAVAILABLE.value();
            responseStr = errorInfoToString(
                    new ErrorInfo("200003", String.format("System is not available: %s Exception Cause: %s",
                            serviceName, exceptionCause(restClientException)),
                            restClientException.getClass().getSimpleName() + ": " + restClientException.getMessage()));
        }

        dxResponse.setHttpStatus(httpStatus);
        dxResponse.setHttpHeaders(headers != null ? headers : new HttpHeaders());
        dxResponse.setDxResponse(responseStr);
        filterFields(dxResponse, responseStr);
        return dxResponse;
    }

    @Trace
    private ResponseEntity<String> callExchange(OAuth2RestTemplate oAuth2RestTemplate, DevExchangeRequest devExchangeRequest) {
        return oAuth2RestTemplate.exchange(devExchangeRequest.getUri(),
                devExchangeRequest.getHttpMethod(), new HttpEntity<>(devExchangeRequest.getRequestBody(),
                        devExchangeRequest.getHttpHeaders()),
                String.class);
    }

    @Trace
    private void timeRequest(long start, ResponseEntity<String> response) {
        long elapsed = System.currentTimeMillis() - start;
        if (logger.isInfoEnabled() && logSetting != LogSetting.OFF) {
            logResponse(response, elapsed);
        }
    }

    @Trace
    private void filterFields(DevExchangeResponse dxResponse, String responseStr) {
        //TODO: Replace with new client id set by devexchange in new header work
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
        if (Scheme.PDX.name().equalsIgnoreCase(devexchangeRequest.getScheme())) {
            PartnerTokenGenerator tokenGenerator = tokenGeneratorFactory.getPartnerTokenGenerator(devexchangeRequest.getServiceName());
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
    private String exceptionCause(RestClientException e) {
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
                body = (logSetting == LogSetting.PRETTY) ? reformatJSON(body) : body;
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
        boolean prettyPrint = logSetting == LogSetting.PRETTY || logSetting == LogSetting.PRETTYRESP;
        String responseStr = StringUtils.isNotBlank(response.getBody()) ? response.getBody() : "{}";
        if (logger.isInfoEnabled()) {
            logger.info("DevExchange response  status= {}  elapsed= {} ms  headers= {}  body= {}",
                    response.getStatusCodeValue(),
                    elapsed,
                    response.getHeaders().toSingleValueMap(),
                    prettyPrint ? reformatJSON(responseStr) : responseStr);
        }
    }

    /**
     * Reformat JSON with pretty-printing.
     *
     * @param text the JSON to reformat
     * @return the reformatted JSON text
     */
    @Trace
    static String reformatJSON(String text) {
        try {
            Object json = objectMapper.readValue(text, Object.class);
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(json);
        } catch (JsonProcessingException e) {
            return text;
        }
    }

    /**
     * A private enum type to handle parsing the system property that controls logging
     * of Dev Exchange requests and responses.
     * </p>
     * Here are the various options and their effects:
     * </p>
     * <table border="1" cellPadding="5">
     * <tr><th>Property</th><th>Request Format</th><th>Response Format</th></tr>
     * <tr><td>not set</td><td align="center" colspan="2">no logging</td></tr>
     * <tr><td>-Dcore.dxtrace</td><td align="center">as-is</td><td align="center">as-is</td></tr>
     * <tr><td>-Dcore.dxtrace=pretty</td><td align="center">pretty-printed</td><td align="center">pretty-printed</td></tr>
     * <tr><td>-Dcore.dxtrace=prettyresp</td><td align="center">as-is</td><td align="center">pretty-printed</td></tr>
     * <tr><td>-Dcore.dxtrace=&lt;anything_else&gt;</td><td align="center">as-is</td><td align="center">as-is</td></tr>
     * </table>
     * </br>
     *
     * @author oqu271
     */
    protected enum LogSetting {
        OFF,        // The default - no logging
        RAW,        // No reformatting
        PRETTY,        // Pretty-print both request and response
        PRETTYRESP; // Pretty-print only the response

        @Trace
        static LogSetting fromProperty() {
            String value = System.getProperty("core.dxtrace");
            if (value == null) {
                return OFF;
            } else if (value.equalsIgnoreCase("pretty")) {
                return PRETTY;
            } else if (value.equalsIgnoreCase("prettyresp")) {
                return PRETTYRESP;
            } else {
                return RAW;
            }
        }
    }

    @SneakyThrows
    static String errorInfoToString(ErrorInfo errorInfo) {
        return objectMapper.writeValueAsString(errorInfo);
    }

    /**
     * A private enum type to identify the type of gateway. It can be internal or any one of the external gateways.
     *
     * @author dyt170
     */
    protected enum Scheme {
        DX,        // Internal Gateway
        PDX        // External Gateway One
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