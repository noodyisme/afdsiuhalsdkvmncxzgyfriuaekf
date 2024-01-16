package com.capitalone.identity.identitybuilder.policycore.camel.util;

import com.capitalone.chassis.engine.model.context.RequestContext;
import com.capitalone.identity.identitybuilder.audit.models.AuditEvent;
import com.capitalone.identity.identitybuilder.audit.models.ConfigurableSettings;
import com.capitalone.identity.identitybuilder.audit.sdpv4.ProducerProperties;
import com.capitalone.identity.identitybuilder.audit.sdpv4.AuditPublisher;
import com.capitalone.identity.identitybuilder.policycore.camel.PolicyConstants;
import com.capitalone.identity.identitybuilder.policycore.service.constants.ApplicationConstants;
import com.newrelic.api.agent.Trace;

import org.apache.camel.Body;
import org.apache.camel.Consume;
import org.apache.camel.Exchange;
import org.apache.camel.Header;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * A utility route that sends an Identity CuRE event to the OneStream API.
 * <p>
 * The fields defined for the Identity CuRE envelop for SDPv4 events are
 * <a href="https://github.cloud.capitalone.com/CuRE/cure-topic-spec">documented here</a> 
 * <p>
 * The body of the message on input is a map of the event fields to send to SDPv4.
 * The following fields are required:
 * <ul>
 *     <li>cure_event_name</li>
 *     <li>cure_event_result</li>
 *     <li>cure_customer_id</li>
 *     <li>cure_customer_id_type</li>
 * </ul>
 * <p>
 * All other base fields have defaults that are supplied by this route from the environment or
 * request-specific information. You can override these other fields by specifying them
 * explicitly in the body.
 * <p>
 * Here is an example invocation where key1, key2, and key3 are custom fields for a specific event.
 * <pre>
 * &lt;setBody&gt;
 *     &lt;groovy&gt;
 * [
 *   'cure_event_name': 'your_schema_name',
 *   'cure_event_result': 'string',
 *   'cure_customer_id': 'string',
 *   'cure_customer_id_type': 'string',
 *
 *   'key1': 'value1',
 *   'key2': 'value2',
 *   'key3': 'value3'
 * ]
 *     &lt;groovy&gt;
 * &lt;/setBody&gt;
 * &lt;to uri="direct:onestreamIdentity"/&gt;
 * </pre>
 *
 * @author oqu271
 */
@Component
@ConditionalOnProperty(name=ApplicationConstants.ONESTREAM_HOST)
public class OneStream {
    protected final Logger logger = LogManager.getLogger(getClass());

    // Required CuRE event fields defined in https://github.cloud.capitalone.com/CuRE/cure-topic-spec
    public static final String CURE_BUSINESS_APPLICATION = "cure_business_application";
    public static final String CURE_CORRELATION_ID = "cure_correlation_id";
    public static final String CURE_CUSTOMER_ID = "cure_customer_id";
    public static final String CURE_CUSTOMER_ID_TYPE = "cure_customer_id_type";
    public static final String CURE_EVENT_NAME = "cure_event_name";
    public static final String CURE_EVENT_END_TIMESTAMP = "cure_event_end_timestamp";
    public static final String CURE_EVENT_RESULT = "cure_event_result";
    public static final String CURE_EVENT_START_TIMESTAMP = "cure_event_start_timestamp";
    public static final String CURE_MESSAGE_ID = "cure_message_id";

    // Optional Schema Name
    public static final String SCHEMA_NAME = "one_stream_schema_name";

    // Identity envelope fields that we need to interact with. Additional fields are
    // - identity_user_agent
    // - identity_single_sign_on_id
    // - identity_device_print
    // - identity_universal_device_id
    // - identity_single_sign_on_token_hash
    public static final String IDENTITY_PRODUCER_APPLICATION_API_KEY = "identity_producer_application_api_key";
    public static final String IDENTITY_CUSTOMER_IP_ADDRESS = "identity_customer_ip_address";
    public static final String IDENTITY_CHANNEL_TYPE = "identity_channel_type";
    public static final String IDENTITY_PRODUCER_APPLICATION_NAME = "identity_producer_application_name";
    public static final String JOURNEY_POLICY_NAME = "journey_policy_name";
    public static final String JOURNEY_POLICY_VERSION = "journey_policy_version";
    public static final String JOURNEY_POLICY_PROCESS_ID = "journey_policy_process_id";

    // Field definitions found in https://github.cloud.capitalone.com/CuRE/cure-topic-spec
    private static final List<String> validEventResults = Arrays.asList("Success", "Failure", "PartialSuccess");
    private static final List<String> validCustomerIdTypes = Arrays.asList("ESCID", "UCID", "SSOID", "ACCT3PK", "CIF", "MMID", "NDSP", "PRID", "CRID", "UVI_EMAIL", "UVI_PHONE", "UVI_PUSHTOKEN", null);
    private static final List<String> validChannelTypes = Arrays.asList("Web", "MobileWeb", "Mobile", "Agent");

    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ");

    // The CuRE event fields that the caller MUST supply.
    private static final Set<String> REQUIRED_POLICY_CURE_FIELDS = new TreeSet<>(Arrays.asList(
            CURE_EVENT_NAME,
            CURE_EVENT_RESULT,
            CURE_CUSTOMER_ID,
            CURE_CUSTOMER_ID_TYPE));

    private final String apiHost;
    private final int apiPort;
    private final String applicationName;
    private final String clientId;
    private final String clientSecret;
    private final String businessApplication;
    private String devExURL;
    private final boolean isProd;

    private final AuditPublisher publisher = new AuditPublisher(makeSettings());
    private final Set<String> registeredSchemas = new TreeSet<>();

    /**
     * Constructor used for dependency injection.
     *
     * @param apiHost the target uri prefix
     */
    @SuppressWarnings("squid:S00107")
    public OneStream(
            @Value("${" + ApplicationConstants.ONESTREAM_HOST + "}") String apiHost,
            @Value("${" + ApplicationConstants.ONESTREAM_PORT + "}") int apiPort,
            @Value("${SERVICE_NAME}") String applicationName,
            @Value("${ba}") String businessApplication,
            @Value("${chassis.config.env:''}") String configEnv,
            @Value("${${SERVICE_NAME}_client_id}") String clientId,
            @Value("${${SERVICE_NAME}_client_secret}") String clientSecret,
            @Value("${env.gatewayURL}") String devExURL) {
        this.apiHost = apiHost;
        this.apiPort = apiPort;
        boolean useDollhouse = apiHost.equals("localhost") && apiPort == 4202;
        this.applicationName = applicationName;
        this.businessApplication = businessApplication;
        this.isProd = configEnv.toLowerCase(Locale.ROOT).contains("prod");
        this.clientId = (useDollhouse) ? "test1234" : clientId;
        this.clientSecret = (useDollhouse) ? "" : clientSecret;
        this.devExURL = (useDollhouse) ? "dollhouse" : devExURL;

        // When running Masterbuilder in a PERF environment, devExURL points to a
        // stub server which does not provide OAuth tokens.  This is a workaround
        // to always use the QA gateway for OAuth unless using the Dollhouse.
        if (!useDollhouse && !isProd) {
            this.devExURL = "https://api-it.cloud.capitalone.com";
        }
    }

    /**
     * A Camel route that sends a Consumer Identity CuRE event to OneStream.
     * <p>
     * The body of the message must be a map containing the CuRE event fields to send.
     *
     * @param  exchange the message exchange
     * @param  body the message to send
     * @param  dxHeaders the DX headers set on the exchange
     * @throws IllegalArgumentException if required keys are missing
     */
    @Consume("direct:onestreamIdentity")
    @Trace
    public void process(Exchange exchange, @Body Map<String, Object> body,
            @Header(PolicyConstants.HEADER_DXHEADERS) Map<String, String> dxHeaders) {
        // Validate all required CuRE event fields are present.
        if (!body.keySet().containsAll(REQUIRED_POLICY_CURE_FIELDS)) {
            throw new IllegalArgumentException("Events must contain the following required CuRE fields: " + REQUIRED_POLICY_CURE_FIELDS);
        }

        // Validate the event result value.
        validateCureFieldEnum(CURE_EVENT_RESULT, (String) body.get(CURE_EVENT_RESULT), validEventResults);

        // Validate the customer id type value
        validateCureFieldEnum(CURE_CUSTOMER_ID_TYPE, (String) body.get(CURE_CUSTOMER_ID_TYPE), validCustomerIdTypes);
        // ------------------------
        // BEGIN message enrichment
        // ------------------------

        // Access the request context data from the reserved message header
        // instead of the thread-local variable in case we're in a worker thread.
        RequestContext rc = exchange.getIn().getHeader(PolicyConstants.HEADER_AUDIT_CONTEXT, RequestContext.class);

        // Start with a copy of the body so the caller's message body remains untouched.
        Map<String, Object> cureData = new HashMap<>(body);

        // For required CuRE fields without a specific value, substitute defaults.
        cureData.putIfAbsent(CURE_BUSINESS_APPLICATION, businessApplication);
        cureData.putIfAbsent(CURE_CORRELATION_ID, rc.getCorrelationId());
        cureData.putIfAbsent(CURE_EVENT_START_TIMESTAMP,
                formatter.format(rc.getEventStartTime().atOffset(ZoneOffset.UTC)));
        cureData.putIfAbsent(CURE_EVENT_END_TIMESTAMP,
                formatter.format(Instant.now().atOffset(ZoneOffset.UTC)));
        cureData.putIfAbsent(CURE_MESSAGE_ID, rc.getMessageId());
        final String schemaName = (String) Optional.ofNullable(cureData.get(SCHEMA_NAME)).orElse(cureData.get(CURE_EVENT_NAME));
        cureData.remove(SCHEMA_NAME); // Do not include schema in message body

        // For identity envelope fields without a specific value, substitute defaults.
        cureData.putIfAbsent(IDENTITY_CUSTOMER_IP_ADDRESS, dxHeaders.get(ApplicationConstants.CUSTOMER_IP_ADDR));
        cureData.putIfAbsent(IDENTITY_PRODUCER_APPLICATION_API_KEY, dxHeaders.get(ApplicationConstants.API_KEY));
        cureData.putIfAbsent(IDENTITY_CHANNEL_TYPE, normalizeChannelType(dxHeaders.get(ApplicationConstants.CHANNEL_TYPE))); // channel-type header
        cureData.putIfAbsent(IDENTITY_PRODUCER_APPLICATION_NAME, applicationName);

        // Add in Journey policy information, if available.
        if (dxHeaders.containsKey(ApplicationConstants.X_JOURNEY_POLICY)) {
            String[] split = dxHeaders.get(ApplicationConstants.X_JOURNEY_POLICY).split(":");
            if (split.length >= 2) {
                cureData.put(JOURNEY_POLICY_NAME, split[0]);
                cureData.put(JOURNEY_POLICY_VERSION, split[1]);
                if (split.length > 2) {
                    cureData.put(JOURNEY_POLICY_PROCESS_ID, split[2]);
                }
            }
        }

        // ----------------------
        // END message enrichment
        // ----------------------

        // Validate the channel type value.
        if (!validChannelTypes.contains((String) cureData.get(IDENTITY_CHANNEL_TYPE))) {
            throw new IllegalArgumentException("CuRE field " + IDENTITY_CHANNEL_TYPE + " must have one of the following values: " + validChannelTypes);
        }

        // Invoke the Audit library.
        registerSchemaIfNecessary((String) cureData.get(CURE_BUSINESS_APPLICATION), schemaName);
        publisher.publish(AuditEvent.builder().message(cureData).schemaName(schemaName).build());
    }

    /**
     * Registers a new schema and properties with the publisher (if necessary).
     *
     * @param businessApplication the business application
     * @param schemaName the schema name
     */
    private void registerSchemaIfNecessary(final String businessApplication, final String schemaName) {
        synchronized (registeredSchemas) {
            if (!registeredSchemas.contains(schemaName)) {
                publisher.newEntry(makeProps(businessApplication, schemaName));
                registeredSchemas.add(schemaName);
            }
        }
    }

    /**
     * Create producer properties for a specific schema name.
     *
     * @param businessApplication the business application
     * @param schemaName the schema name
     * @return a new producer properties object for the schema name.
     */
    private ProducerProperties makeProps(final String businessApplication, final String schemaName) {
        return ProducerProperties.builder()
                .businessApplication(businessApplication)
                .schemaName(schemaName)
                // Oauth
                .clientId(clientId)
                .clientSecret(clientSecret)
                .devexUrl(devExURL)
                .devexEnvConfig(isProd ? "PROD" : "IT")
                // SDP target URL
                .fanInHost(apiHost)
                .fanInPort(apiPort)
                .build();
    }

    /**
     * Create the runtime-configurable settings for the publisher.
     *
     * @return a new settings object
     */
    private ConfigurableSettings makeSettings() {
        return ConfigurableSettings.builder()
                .concurrentConnections(4)
                .batchSize(100)
                .waitBetweenBatchesMs(150)
                .retryTimeoutMs(3000)
                .maxAllowedRetries(3)
                .preemptiveFallback(false)
                .maxAllowedQueueSize(100000) // ?
                .delayBetweenRetriesMs(250) // ?
                .build();
    }

    /**
     * Normalize the Channel-Type value.
     *
     * @param  text the value from the Channel-Type
     * @return the normalized value (unchanged if not supported)
     */
    public static String normalizeChannelType(String text) {
        switch(text.toLowerCase(Locale.US)) {
            case "web": return "Web";
            case "mobileweb": return "MobileWeb";
            case "mobile": return "Mobile";
            case "agent": return "Agent";
            default: return text;
        }
    }

    /**
     * Make sure the given field value matches the known list of acceptable values.
     *
     * @param fieldName the name of the field being checked
     * @param value the value of the field being checked
     * @param validValues the list of acceptable values
     * @throws IllegalArgumentException if <code>value</code> is not found in <code>validValues</code>
     */
    private static void validateCureFieldEnum(String fieldName, String value, List<String> validValues) {
        // Validate the event result value.
        if (!validValues.contains(value)) {
            throw new IllegalArgumentException("CuRE field " + fieldName + " must have one of the following values: " + validValues);
        }
    }
}

/*
 * Copyright 2021 Capital One Financial Corporation All Rights Reserved.
 *
 * This software contains valuable trade secrets and proprietary information of
 * Capital One and is protected by law. It may not be copied or distributed in
 * any form or medium, disclosed to third parties, reverse engineered or used in
 * any manner without prior written authorization from Capital One.
 */