package com.capitalone.identity.identitybuilder.policycore.camel.util;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EmptySource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mockito;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.test.util.ReflectionTestUtils;

import com.capitalone.chassis.engine.model.context.RequestContext;
import com.capitalone.identity.identitybuilder.audit.models.AuditEvent;
import com.capitalone.identity.identitybuilder.audit.sdpv4.AuditPublisher;
import com.capitalone.identity.identitybuilder.audit.sdpv4.ProducerProperties;
import com.capitalone.identity.identitybuilder.policycore.camel.PolicyConstants;
import com.capitalone.identity.identitybuilder.policycore.camel.routes.PolicyTestSupportExchange;
import com.capitalone.identity.identitybuilder.policycore.service.constants.ApplicationConstants;
import com.capitalone.identity.identitybuilder.policycore.utils.CamelSpringBootContextAwareTest;
import com.capitalone.identity.identitybuilder.policycore.utils.XmlDslCamelTestContextConfiguration;

@CamelSpringBootContextAwareTest(
        contextConfigClasses = OneStreamTest.ContextConfigCamel.class,
        excludeFromComponentScan = @Filter(type = FilterType.ASPECTJ,
                pattern = "com.capitalone.identity.identitybuilder.policycore.camel.util.*"))
public class OneStreamTest implements PolicyTestSupportExchange {

    @Autowired
    private CamelContext camelContext;

    private static final String API_HOST = "apiHost";
    private static final int    API_PORT = 8675309;
    private static final String APPLICATION_NAME = "applicationName";
    private static final String BA = "businessApplicationName";
    private static final String ENV = "devint-test";
    private static final String CLIENT_ID = "clientId";
    private static final String CLIENT_SECRET = "clientSecret";
    private static final String DEVEX_URL = "devexUrl";
    private static final String CUSTOMER_ID = "customerId";
    private static final String CUSTOMER_ID_TYPE = "SSOID";
    private static final String EVENT_NAME = "eventName";
    private static final String EVENT_RESULT_SUCCESS = "Success";
    private static final String CORRELATION_ID = "correlationId";
    private static final String MESSAGE_ID = "messageId";
    private static final String POLICY_NAME = "policy_name";
    private static final String POLICY_VERSION = "policy_version";
    private static final String POLICY_PROCESS_ID = "policy_process_id";

    private Exchange exchange;
    private Map<String, Object> body;
    private Map<String, String> dxHeaders;

    private OneStream onestream;
    private AuditPublisher publisherSpy;
    private final Instant endTime = Instant.now();
    private final Instant startTime = endTime.minusSeconds(1);

    private static final String DATE_TIME_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DATE_TIME_PATTERN);

    private final Map<String, Object> expectedResult = buildDefaultExpectedEventJson();
    private Map<String, Object> buildDefaultExpectedEventJson() {
        final Map<String, Object> retVal = new HashMap<>();
        retVal.put(OneStream.CURE_CUSTOMER_ID, CUSTOMER_ID);
        retVal.put(OneStream.CURE_CUSTOMER_ID_TYPE, CUSTOMER_ID_TYPE);
        retVal.put(OneStream.CURE_BUSINESS_APPLICATION, BA);
        retVal.put(OneStream.CURE_EVENT_NAME, EVENT_NAME);
        retVal.put(OneStream.CURE_EVENT_RESULT, EVENT_RESULT_SUCCESS);
        retVal.put(OneStream.CURE_CORRELATION_ID, CORRELATION_ID);
        retVal.put(OneStream.CURE_MESSAGE_ID, MESSAGE_ID);
        retVal.put(OneStream.CURE_EVENT_START_TIMESTAMP, formatter.format(startTime.atOffset(ZoneOffset.UTC)));
        retVal.put(OneStream.CURE_EVENT_END_TIMESTAMP, formatter.format(endTime.atOffset(ZoneOffset.UTC)));
        retVal.put(OneStream.IDENTITY_CHANNEL_TYPE, "Web");
        retVal.put(OneStream.IDENTITY_CUSTOMER_IP_ADDRESS, "Customer-IP-value");
        retVal.put(OneStream.IDENTITY_PRODUCER_APPLICATION_NAME, "applicationName");
        retVal.put(OneStream.IDENTITY_PRODUCER_APPLICATION_API_KEY, "Api-Key-value");
        return retVal;
    }

    @Captor
    ArgumentCaptor<ProducerProperties> producerPropertiesCaptor;
    @Captor
    ArgumentCaptor<AuditEvent> eventCaptor;

    @SuppressWarnings("unchecked")
    @BeforeEach
    public void setup() {
        onestream = new OneStream(API_HOST, API_PORT, APPLICATION_NAME, BA, ENV, CLIENT_ID, CLIENT_SECRET, DEVEX_URL);
        publisherSpy = (AuditPublisher) Mockito.spy(Objects.requireNonNull(ReflectionTestUtils.getField(onestream, "publisher")));
        ReflectionTestUtils.setField(onestream, "publisher", publisherSpy);
        doNothing().when(publisherSpy).newEntry(any(ProducerProperties.class));
        doNothing().when(publisherSpy).publish(any(AuditEvent.class));

        body = new TreeMap<>();
        body.put(OneStream.CURE_CUSTOMER_ID, CUSTOMER_ID);
        body.put(OneStream.CURE_CUSTOMER_ID_TYPE, CUSTOMER_ID_TYPE);
        body.put(OneStream.CURE_EVENT_NAME, EVENT_NAME);
        body.put(OneStream.CURE_EVENT_RESULT, EVENT_RESULT_SUCCESS);

        RequestContext rc = new RequestContext();
        rc.setCorrelationId(CORRELATION_ID);
        rc.setEventStartTime(startTime);
        rc.setEventEndTime(endTime);
        rc.setMessageId(MESSAGE_ID);

        exchange = policyExchange(body, null);
        exchange.getIn().setHeader(PolicyConstants.HEADER_AUDIT_CONTEXT, rc);
        exchange.getIn().setHeader(PolicyConstants.HEADER_POLICYNAME, POLICY_NAME);
        exchange.getIn().setHeader(PolicyConstants.HEADER_POLICYVERSION, POLICY_VERSION);
        exchange.getIn().setHeader(PolicyConstants.HEADER_STEPNAME, "start");
        dxHeaders = (Map<String, String>) exchange.getIn().getHeader(PolicyConstants.HEADER_DXHEADERS, Map.class);
        dxHeaders.put(ApplicationConstants.CHANNEL_TYPE, "Web");
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testCuREAndIdentityDefaults() {
        // Our test setup sets a specific end timestamp so data comparisons are easier.
        // For this test, we will default to a value of "now" and do a fuzzy comparison instead.
        body.remove(OneStream.CURE_EVENT_END_TIMESTAMP);

        Instant start = Instant.now();
        onestream.process(exchange, body, dxHeaders);
        verify(publisherSpy, times(1)).newEntry(producerPropertiesCaptor.capture());
        ProducerProperties producerProperties = producerPropertiesCaptor.getValue();
        verify(publisherSpy).publish(eventCaptor.capture());
        AuditEvent event = eventCaptor.getValue();

        // Check that the correct schema was utilized
        Assertions.assertEquals(body.get(OneStream.CURE_EVENT_NAME), producerProperties.getSchemaName());

        // Check that the result end time is very close to the time we called process().
        // Then reset the value to the known value so the expected result will compare.
        TreeMap<String, Object> result = new TreeMap<>((Map<String, Object>) event.getMessage());
        Instant resultEndTime = OffsetDateTime.parse(result.get(OneStream.CURE_EVENT_END_TIMESTAMP).toString(),formatter).toInstant();
        assertTrue(Duration.between(start, resultEndTime).toMillis() < 100);
        result.put(OneStream.CURE_EVENT_END_TIMESTAMP, formatter.format(endTime.atOffset(ZoneOffset.UTC)));

        assertEquals(expectedResult, result);
    }

    @Test
    public void testXJourneyPolicyHeaderSingleStep() {
        dxHeaders.put(ApplicationConstants.X_JOURNEY_POLICY, POLICY_NAME + ":" + POLICY_VERSION);
        expectedResult.put(OneStream.JOURNEY_POLICY_NAME, POLICY_NAME);
        expectedResult.put(OneStream.JOURNEY_POLICY_VERSION, POLICY_VERSION);

        assertTrue(publishAndTest(expectedResult));
    }

    @Test
    public void testXJourneyPolicyHeaderMultiStep() {
        dxHeaders.put(ApplicationConstants.X_JOURNEY_POLICY, POLICY_NAME + ":" + POLICY_VERSION + ":" + POLICY_PROCESS_ID);
        expectedResult.put(OneStream.JOURNEY_POLICY_NAME, POLICY_NAME);
        expectedResult.put(OneStream.JOURNEY_POLICY_VERSION, POLICY_VERSION);
        expectedResult.put(OneStream.JOURNEY_POLICY_PROCESS_ID, POLICY_PROCESS_ID);

        assertTrue(publishAndTest(expectedResult));
    }

    @ParameterizedTest
    @ValueSource(strings = { "Success", "Failure", "PartialSuccess" })
    public void testValidEventResults(String result) {
        body.put(OneStream.CURE_EVENT_RESULT, result);
        expectedResult.put(OneStream.CURE_EVENT_RESULT, result);

        assertTrue(publishAndTest(expectedResult));
    }

    @ParameterizedTest
    @NullSource
    @EmptySource
    @ValueSource(strings = { "invalid_value", "SUCCESS" })
    public void testInvalidEventResult(String result) {
        body.put(OneStream.CURE_EVENT_RESULT, result);
        assertThrows(IllegalArgumentException.class, () -> onestream.process(exchange, body, dxHeaders));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = { "ESCID", "UCID", "SSOID", "ACCT3PK", "CIF", "MMID", "NDSP", "PRID", "CRID", "UVI_EMAIL", "UVI_PHONE", "UVI_PUSHTOKEN" })
    public void testValidCustomerIdType(String idType) {
        body.put(OneStream.CURE_CUSTOMER_ID_TYPE, idType);
        expectedResult.put(OneStream.CURE_CUSTOMER_ID_TYPE, idType);

        assertTrue(publishAndTest(expectedResult));
    }

    @ParameterizedTest
    @EmptySource
    @ValueSource(strings = { "invalid_value", "escid" })
    public void testInvalidCustomerIdType(String idType) {
        body.put(OneStream.CURE_CUSTOMER_ID_TYPE, idType);
        assertThrows(IllegalArgumentException.class, () -> onestream.process(exchange, body, dxHeaders));
    }

    @ParameterizedTest
    @ValueSource(strings = { "Web", "MobileWeb", "Mobile", "Agent" })
    public void testValidExplicitChannelType(String channelType) {
        body.put(OneStream.IDENTITY_CHANNEL_TYPE, channelType);
        expectedResult.put(OneStream.IDENTITY_CHANNEL_TYPE, OneStream.normalizeChannelType(channelType));

        assertTrue(publishAndTest(expectedResult));
    }

    @ParameterizedTest
    @EmptySource
    @ValueSource(strings = { "invalid_value", "MOBILE" })
    public void testInvalidExplicitChannelType(String channelType) {
        body.put(OneStream.IDENTITY_CHANNEL_TYPE, channelType);
        assertThrows(IllegalArgumentException.class, () -> onestream.process(exchange, body, dxHeaders));
    }

    @ParameterizedTest
    @ValueSource(strings = { "Web", "WEB", "MobileWeb", "mobileweb", "Mobile", "MoBiLe", "Agent", "aGeNt" })
    public void testValidImplicitChannelType(String channelType) {
        dxHeaders.put(ApplicationConstants.CHANNEL_TYPE, channelType);
        expectedResult.put(OneStream.IDENTITY_CHANNEL_TYPE, OneStream.normalizeChannelType(channelType));

        assertTrue(publishAndTest(expectedResult));
    }

    @ParameterizedTest
    @EmptySource
    @ValueSource(strings = { "invalid_value", "MOBILEx" })
    public void testInvalidImplicitChannelType(String channelType) {
        dxHeaders.put(ApplicationConstants.CHANNEL_TYPE, channelType);
        assertThrows(IllegalArgumentException.class, () -> onestream.process(exchange, body, dxHeaders));
    }

    @ParameterizedTest
    @ValueSource(strings = { OneStream.CURE_EVENT_NAME, OneStream.CURE_EVENT_RESULT, OneStream.CURE_CUSTOMER_ID, OneStream.CURE_CUSTOMER_ID_TYPE })
    public void testMissingCureEvent(String missingField) {
        body.remove(missingField);
        assertThrows(IllegalArgumentException.class, () -> onestream.process(exchange, body, dxHeaders));
    }

    @Test
    public void testSchemaNameSpecified() {
        // Arrange
        final String expectedSchemaName = "mySchema";
        body.put(OneStream.SCHEMA_NAME, expectedSchemaName);

        // Act & Assert
        Assertions.assertTrue(publishAndTest(expectedResult));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            OneStream.CURE_BUSINESS_APPLICATION, OneStream.CURE_CORRELATION_ID, OneStream.CURE_EVENT_START_TIMESTAMP, OneStream.CURE_EVENT_END_TIMESTAMP, OneStream.CURE_MESSAGE_ID,
            OneStream.IDENTITY_CHANNEL_TYPE, OneStream.IDENTITY_CUSTOMER_IP_ADDRESS, OneStream.IDENTITY_PRODUCER_APPLICATION_API_KEY, OneStream.IDENTITY_PRODUCER_APPLICATION_NAME })
    public void testFieldOverrides(String fieldName) {
        String override = OneStream.IDENTITY_CHANNEL_TYPE.equals(fieldName) ? "Mobile" : "foo";
        body.put(fieldName, override);
        expectedResult.put(fieldName, override);

        assertTrue(publishAndTest(expectedResult));
    }

    /**
     * Sends a single event and performs common assertions against the producer.
     *
     * @param expectedResult the expected result to compare against
     * @return always returns <code>true<code> so the caller can do an assertion
     *         on the result (SonarCube frowns on test methods with no assertion)
     */
    @SuppressWarnings("unchecked")
    private boolean publishAndTest(Map<String, Object> expectedResult) {
        onestream.process(exchange, body, dxHeaders);


        verify(publisherSpy, times(1)).newEntry(producerPropertiesCaptor.capture());
        ProducerProperties producerProperties = producerPropertiesCaptor.getValue();
        verify(publisherSpy).publish(eventCaptor.capture());
        AuditEvent event = eventCaptor.getValue();

        // Check that the correct schema was utilized
        if (body.containsKey(OneStream.SCHEMA_NAME)) {
            Assertions.assertEquals(body.get(OneStream.SCHEMA_NAME), producerProperties.getSchemaName());
        } else {
            Assertions.assertEquals(body.get(OneStream.CURE_EVENT_NAME), producerProperties.getSchemaName());
        }

        TreeMap<String, Object> actual = new TreeMap<>((Map<String, Object>) event.getMessage());
        // remove end timestamps, unpredictable for test execution
        assertNotNull(expectedResult.remove(OneStream.CURE_EVENT_END_TIMESTAMP));
        assertNotNull(actual.remove(OneStream.CURE_EVENT_END_TIMESTAMP));
        assertEquals(expectedResult, actual);

        return true;
    }

    @Override
    public CamelContext camelContext() {
        return this.camelContext;
    }

    @Configuration
    public static class ContextConfigCamel extends XmlDslCamelTestContextConfiguration {
    }
}