package com.capitalone.identity.identitybuilder.policycore.camel.components.devexchange;

import com.capitalone.chassis.engine.model.context.RequestContext;
import com.capitalone.chassis.engine.model.context.RequestContextHolder;
import com.capitalone.chassis.engine.model.exception.ChassisBusinessException;
import com.capitalone.identity.identitybuilder.policycore.camel.PolicyConstants;
import com.capitalone.identity.identitybuilder.policycore.model.DevExchangeRequest;
import com.capitalone.identity.identitybuilder.policycore.model.DevExchangeResponse;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.model.policy.OperationalAuditExecutionData;
import com.capitalone.identity.identitybuilder.policycore.service.constants.ApplicationConstants;
import com.capitalone.identity.identitybuilder.policycore.service.util.JsonUtil;
import com.newrelic.api.agent.Trace;
import org.apache.camel.Endpoint;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.model.DataFormatDefinition;
import org.apache.camel.model.ModelCamelContext;
import org.apache.camel.spi.DataFormat;
import org.apache.camel.support.DefaultProducer;
import org.apache.camel.util.CastUtils;
import org.apache.camel.util.StringHelper;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * The Policy producer.
 */
public class DevExchangeProducer extends DefaultProducer {
    public static final String HEADER_CLIENT_IP = "Client-IP";
    public static final String CUSTOM_MULTIPART_HEADER_KEY = "customMultipartContent";
    private static final Logger logger = LoggerFactory.getLogger(DevExchangeProducer.class);
    private static final Set<HttpMethod> SUPPORTED_METHODS = new HashSet<>(
            Arrays.asList(HttpMethod.GET, HttpMethod.POST, HttpMethod.PUT,
                    HttpMethod.DELETE, HttpMethod.PATCH, HttpMethod.OPTIONS));
    private static final String CUSTOM_MULTIPART_NAME_KEY = "name";
    private static final String CUSTOM_MULTIPART_FILENAME_KEY = "filename";
    private static final Object CUSTOM_MULTIPART_MEDIA_TYPE = "mediatype";

    private final DataFormat jackson;
    private final HttpMethod method;

    public DevExchangeProducer(Endpoint endpoint) {
        super(endpoint);
        method = HttpMethod.valueOf(getEndpoint().getMethod().toUpperCase(Locale.US));
        if (!SUPPORTED_METHODS.contains(method)) {
            throw new UnsupportedOperationException(String.format(
                    "The DevExchange endpoint only supports the following methods: %s", SUPPORTED_METHODS.toString().toLowerCase()));
        }

        // Find the data formatter to parse returned JSON into a Map.
        jackson = findJacksonDataFormat(endpoint);
    }

    @Override
    @Trace
    public void process(Exchange exchange) throws Exception {
        DevExchangeEndpoint theEndpoint = getEndpoint();
        Message message = exchange.getMessage();
        URI finalTargetUrl = createTargetUri(theEndpoint, message.getHeaders());

        // Construct the HTTP headers for the request.
        HttpHeaders headers = buildHttpHeaders(message);

        Object requestBody;
        if (method != HttpMethod.POST && method != HttpMethod.PUT && method != HttpMethod.PATCH) {
            requestBody = "";
        } else if (message.getBody() == null) {
            throw new ChassisBusinessException("No body provided for DevExchange POST or PUT request");
        } else if (getEndpoint().getCustomMultipart()) {
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            requestBody = constructMultipartMap(message);
        } else {
            // Use the exchange message body as the request payload.  If it is not
            // already a string, marshal it into JSON format.
            requestBody = constructBody(exchange, message, headers);
        }

        String scheme = StringHelper.before(theEndpoint.getEndpointUri(), ":");

        String policyName = exchange.getProperty(ApplicationConstants.POLICY_NAME, String.class);
        String policyVersion = exchange.getProperty(ApplicationConstants.POLICY_VERSION, String.class);
        String policyInfo = policyName + "_v" + policyVersion;

        Optional.ofNullable(OperationalAuditExecutionData.fromMessage(message))
                .ifPresent(header -> header.getPipsEvaluated().add("{ scheme: " + scheme + ", service_name: "
                        + theEndpoint.getServiceName() + ", target_uri: " + finalTargetUrl + ", dx_version: "
                        + theEndpoint.getDxVersion() + ", http_method: " + method.toString() + " }"));

        // Submit the DX API request and handle the response.
        DevExchangeRequest dxRequest = createDxRequest(requestBody, headers, finalTargetUrl, method, theEndpoint.getServiceName(), scheme, policyInfo);
        DevExchangeResponse dxResponse = invoke(exchange, theEndpoint, dxRequest);

        message.setHeader(PolicyConstants.HEADER_HTTPSTATUS, dxResponse.getHttpStatus());
        message.setHeader(PolicyConstants.HEADER_DXRESPONSE_HEADERS, dxResponse.getHttpHeaders().toSingleValueMap());

        String correlationId = (null != dxRequest.getHttpHeaders()) ? dxRequest.getHttpHeaders().getFirst(ApplicationConstants.CLIENT_CORRELATION_ID) : StringUtils.EMPTY;
        logger.debug("Response body from devExchange Service ServiceName {} {} correlation {}", dxRequest.getServiceName(), dxResponse.getDxResponseAudit(),
                correlationId);

        // If an error occurred, throw an exception so policies don't have to check the status.
        if (dxResponse.getHttpStatus() < 200 || dxResponse.getHttpStatus() > 299) {
            exchange.setProperty(Exchange.FAILURE_HANDLED, true);
            throw new DevExchangeException(dxRequest, dxResponse);
        }

        // Unmarshal the result from JSON to a Map.
        if (dxResponse.getDxResponse() != null) {
            Object object = jackson.unmarshal(exchange, new ByteArrayInputStream(dxResponse.getDxResponse().getBytes()));
            message.setBody(object);
        } else {
            message.setBody(dxResponse.getDxResponse());
        }
    }

    private MultiValueMap<String, Object> constructMultipartMap(Message message) {
        List<Map<String,String>> multipartConfigList = message.getHeader(CUSTOM_MULTIPART_HEADER_KEY, List.class);
        List<Object> multipartContentList = message.getBody() instanceof List ? (List<Object>) message.getBody() : Collections.singletonList(message.getBody());
        if (multipartConfigList == null) {
            throw new IllegalArgumentException(
                    "required camel header 'customMultipartContent' missing when customMultipart=true");
        } else {
            MultiValueMap<String, Object> multipartBody = new LinkedMultiValueMap<>();
            for (int i = 0; i < multipartConfigList.size(); i++) {
                Map<String, String> multipartConfig = multipartConfigList.get(i);
                if (!multipartConfig.containsKey(CUSTOM_MULTIPART_NAME_KEY)
                        || !multipartConfig.containsKey(CUSTOM_MULTIPART_FILENAME_KEY)
                        || ObjectUtils.isEmpty(multipartConfig.get(CUSTOM_MULTIPART_NAME_KEY))
                        || ObjectUtils.isEmpty(multipartConfig.get(CUSTOM_MULTIPART_FILENAME_KEY))) {
                    throw new IllegalArgumentException(String.format(
                            "name or filename entry missing from 'customMultipartContent' map [name=%s,filename=%s]",
                            multipartConfig.get(CUSTOM_MULTIPART_NAME_KEY),
                            multipartConfig.get(CUSTOM_MULTIPART_FILENAME_KEY)));
                } else {
                   HttpEntity<Object> entity = constructCustomMultipartBody(multipartContentList.get(i),
                            multipartConfigList.get(i).get(CUSTOM_MULTIPART_NAME_KEY),
                            multipartConfigList.get(i).get(CUSTOM_MULTIPART_FILENAME_KEY), multipartConfigList.get(i).get(CUSTOM_MULTIPART_MEDIA_TYPE));
                    multipartBody.add(multipartConfigList.get(i).get(CUSTOM_MULTIPART_NAME_KEY), entity);
                }
            }
            return multipartBody;
        }
    }

    @NotNull
    private URI createTargetUri(DevExchangeEndpoint theEndpoint, Map<String, Object> headers) {
        String targetUrl = theEndpoint.getTargetUrl();
        // Update URL to mock target if running in Mock Mode
        Object mockModeFlag = headers.get(PolicyConstants.HEADER_MOCKMODE);
        if(mockModeFlag != null && (boolean) mockModeFlag) {
            targetUrl = theEndpoint.getMockTargetUrl();
        }

        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(targetUrl);

        // Any extra query parameters that the endpoint doesn't use should be added to the downstream
        // query.  We want to support a {xxx} syntax for parameter substitution but the endpoint
        // has already escaped the braces so we will unescape them if they are found.
        if (theEndpoint.getQueryParameters() != null) {
            builder.query(theEndpoint.getQueryParameters().replaceAll("%7B", "{").replaceAll("%7D", "}"));
        }
        URI finalTargetUrl;
        try {
            finalTargetUrl = builder.buildAndExpand(headers).toUri();
        } catch (IllegalArgumentException ex) {
            throw new ChassisBusinessException(ex.getMessage() + " in URI: " + theEndpoint.getTargetUrl());
        }
        return finalTargetUrl;
    }

    /**
     * Invokes the DevExchange component, ensuring that the chassis
     * <code>RequestContext</code> is set properly if this is a worker thread.
     * <p>
     * Chassis auditing depends on data in a RequestContext which is held in a
     * thread-local variable. When using multi-threaded EIPs like Splitter, the
     * worker thread does not have access to this object.
     * <p>
     * We solve this by passing the parent RequestContext in a standard message
     * header and then overriding the current request context if needed.
     *
     * @param exchange the exchange making the request
     * @param endpoint the Dev Exchange endpoint to invoke
     * @param request  the request to send
     * @return the response fromo the Dev Exchange component
     */
    @Trace
    private DevExchangeResponse invoke(Exchange exchange, DevExchangeEndpoint endpoint, DevExchangeRequest request) {
        RequestContext currentContext = RequestContextHolder.getRequestContextOrDefault();
        boolean modifiedThreadLocal = false;
        try {
            RequestContext overrideContext = exchange.getIn().getHeader(PolicyConstants.HEADER_AUDIT_CONTEXT, RequestContext.class);
            if (overrideContext != currentContext) {
                RequestContextHolder.put(overrideContext);
                modifiedThreadLocal = true;
            }
            return endpoint.getComponent().getDevExchangeDAO().invokeDxAPI(request);
        } finally {
            if (modifiedThreadLocal) {
                RequestContextHolder.put(currentContext);
            }
        }
    }

    /**
     * Builds the HTTP entity body from the message body.
     * <p>
     * For multipart support, it may also modify the accept header in the <code>headers</code> provided.
     *
     * @throws ChassisBusinessException if no body is provided for a POST or PUT
     * @throws Exception                if unable to marshal the message body to a JSON message
     */
    @Trace
    public String constructBody(Exchange exchange, Message message, HttpHeaders headers) throws Exception {

        String body;
        if (message.getBody() instanceof String) {
            body = message.getBody(String.class);
        } else {
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            jackson.marshal(exchange, message.getBody(), os);
            body = os.toString(StandardCharsets.UTF_8.name());
        }

        // The request body is properly formatted as JSON, but if it is to be sent 
        // as a multipart message, there are some changes that need to be made.
        if (!getEndpoint().getMultipartId().isEmpty()) {
            // Create a multipart boundary code.
            String boundary = RandomStringUtils.random(22, true, true);

            // Override the content type to indicate multi-part and define the boundary.
            headers.set(HttpHeaders.CONTENT_TYPE, "multipart/form-data; boundary=" + boundary);

            // Wrap the request in a multipart message (as per RFC-1341).
            body = "\r\n" +
                    "--" + boundary + "\r\n" +
                    "Content-Disposition: form-data; name=\"" + getEndpoint().getMultipartId() + "\"\r\n" +
                    "Content-Type: application/json\r\n" +
                    "\r\n" +
                    body + "\r\n" +
                    "--" + boundary + "--\r\n";
        }

        return body;
    }

    /**
     * Builds a multipart file upload request. Message body must be in format of base64 encoded string or a byte[].
     */
    @Trace
    public HttpEntity<Object> constructCustomMultipartBody(Object content, String name, String filename, String mediatype) {
        if (ObjectUtils.isEmpty(name) || ObjectUtils.isEmpty(filename) || Objects.isNull(content)) {
            throw new IllegalArgumentException(String.format("Blank multipart filename or name or null content [name=%s,filename=%s, contentIsNull=%s]", name, filename, content==null));
        } else {
            MultiValueMap<String, String> fileMap = new LinkedMultiValueMap<>();
            fileMap.add(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.builder("form-data")
                    .name(name).filename(filename).build().toString());
            if(mediatype != null) {
                fileMap.add(HttpHeaders.CONTENT_TYPE, mediatype);
            }
            return new HttpEntity<>(content, fileMap);

        }
    }

    /**
     * Construct the HTTP headers for the request.
     * <p>
     * The headers are constructed in the following order:
     * <ol>
     * <li>The headers supplied by the client's request (from <code>dxHeaders</code>)</li>
     * <li>Duplicate <code>Customer-IP-Addr</code> as <code>Client-IP</code> for older DX APIs (if desired)</li>
     * <li>Convert a <code>Channel-Type</code> of "mobileweb" to "mobile (if desired)</li>
     * <li>Add our default client headers (<code>Content-Type</code> and <code>Accept</code>)
     *     and set the requested API version</li>
     * <li>Overwrite or add any extra headers (from <code>customHeaders</code>)</li>
     * </ol>
     *
     * @param message the message to get headers from
     * @return the HTTP headers for the request
     */
    private HttpHeaders buildHttpHeaders(Message message) {
        DevExchangeEndpoint endpoint = getEndpoint();
        HttpHeaders headers = new HttpHeaders();

        // Start with the client request's DX headers.
        Map<String, String> dxHeaderMap = CastUtils.cast(message.getHeader(PolicyConstants.HEADER_DXHEADERS, Map.class));
        headers.setAll(dxHeaderMap != null ? dxHeaderMap : new HashMap<String, String>());

        // Some older DX APIs use Client-IP instead of Customer-IP-Address so duplicate it.
        if (endpoint.isCopyClientIP() && headers.getFirst(ApplicationConstants.CUSTOMER_IP_ADDR) != null) {
            headers.set(HEADER_CLIENT_IP, headers.getFirst(ApplicationConstants.CUSTOMER_IP_ADDR));
        }

        // Some DX APIs don't support a Channel-Type of mobileweb so convert it to mobile as needed.
        if (endpoint.isMobileWebToMobile() && "mobileweb".equalsIgnoreCase(headers.getFirst(ApplicationConstants.CHANNEL_TYPE))) {
            headers.set(ApplicationConstants.CHANNEL_TYPE, "mobile");
        }

        // DevExchange has its own (nonstandard) convention for declaring which version of an
        // API should be invoked.  The convention is to append a version number to the Accept header
        // Some APIs don't support this convention so we use "0" to indicate we should omit the version.
        String versionSuffix = endpoint.getDxVersion().equals("0") ? "" : (";v=" + endpoint.getDxVersion());
        headers.set(HttpHeaders.ACCEPT, "application/json" + versionSuffix);

        // Set the Content-Type to JSON.
        headers.set(HttpHeaders.CONTENT_TYPE, "application/json" + (endpoint.isVersionContentType() ? versionSuffix : ""));

        // Finally, if the client has supplied its own custom headers, they will be added
        // (and possibly overwrite) the headers we've already constructed.
        if (message.getHeader(PolicyConstants.HEADER_CUSTOMHEADERS) != null) {
            Map<String, String> customHeaderMap = CastUtils.cast(message.getHeader(PolicyConstants.HEADER_CUSTOMHEADERS, Map.class));
            headers.setAll(customHeaderMap);
        }

        return headers;
    }

    private static DataFormat findJacksonDataFormat(Endpoint endpoint) {
        DataFormatDefinition dataFormatDefinition = endpoint.getCamelContext().adapt(ModelCamelContext.class).resolveDataFormatDefinition("json");
        if (dataFormatDefinition == null) {
            DataFormat dataFormat = endpoint.getCamelContext().resolveDataFormat("jackson");
            if (dataFormat == null) {
                throw new ChassisBusinessException("Cannot unmarshal! No data format named 'json-jackson' found.");
            }
            return dataFormat;
        }

        return dataFormatDefinition.getDataFormat();
    }

    @Override
    public DevExchangeEndpoint getEndpoint() {
        return (DevExchangeEndpoint) super.getEndpoint();
    }

    @Trace
    private DevExchangeRequest createDxRequest(Object requestBody, HttpHeaders headers, URI uri, HttpMethod httpMethod, String serviceName, String scheme, String policyInfo) {
        DevExchangeRequest dxRequest = new DevExchangeRequest();
        dxRequest.setUri(uri);
        dxRequest.setHttpHeaders(headers);
        dxRequest.setHttpMethod(httpMethod);
        dxRequest.setRequestBody(requestBody);
        dxRequest.setServiceName(serviceName);
        dxRequest.setScheme(scheme);
        dxRequest.setPolicyInfo(policyInfo);
        if ((httpMethod == HttpMethod.POST || httpMethod == HttpMethod.PUT || httpMethod == HttpMethod.PATCH) &&
                !headers.getContentType().includes(MediaType.MULTIPART_FORM_DATA)) {
            dxRequest.setRequestBodyAudit(JsonUtil.getFilteredResponse((String) requestBody, getEndpoint().getComponent().getFieldsToFilter(), "dxService"));
        }
        return dxRequest;
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