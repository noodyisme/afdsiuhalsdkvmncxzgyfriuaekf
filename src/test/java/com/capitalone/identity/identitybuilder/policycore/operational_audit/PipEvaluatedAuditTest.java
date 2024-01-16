package com.capitalone.identity.identitybuilder.policycore.operational_audit;

import com.capitalone.identity.identitybuilder.audit.models.AuditEvent;
import com.capitalone.identity.identitybuilder.audit.sdpv4.AuditPublisher;
import com.capitalone.identity.identitybuilder.audit.sdpv4.ProducerProperties;
import com.capitalone.identity.identitybuilder.policycore.model.DevExchangeRequest;
import com.capitalone.identity.identitybuilder.policycore.model.DevExchangeResponse;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.cyber.AWSHostContextCollector;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.mapper.OperationalEventEnricher;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.model.*;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.model.OperationalError;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.model.OperationalEvent;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.model.pip.OperationalPipMetadata;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.model.OperationalSystemContext;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.util.MockHostContext;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.util.OperationalAuditConstants;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.util.OperationalAuditPublisher;
import com.capitalone.identity.identitybuilder.policycore.service.dao.DevExchangeDAO;
import com.capitalone.identity.identitybuilder.policycore.service.dao.impl.DevExchangeDAOImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import static com.capitalone.identity.identitybuilder.policycore.operational_audit.util.AuditTestConstants.*;

class PipEvaluatedAuditTest {

    private static final HttpHeaders DX_HTTP_HEADERS_CURE_INCLUDED = httpHeadersAllIncluded();

    // Dependencies
    private AuditPublisher auditPublisher;
    private OperationalSystemContext operationalSystemContext;

    // Test-only object
    private DevExchangeDAO testOnlyPipEvaluator;

    // Argument captor
    private ArgumentCaptor<AuditEvent> auditEventArgumentCaptor;

    // Object under test
    private OperationalAuditAspect operationalAuditAspect;
    private final AWSHostContextCollector mockAWSHostContextCollector = new AWSHostContextCollector(new MockHostContext());
    @BeforeEach
    void setup() {
        // Initialize the aspect
        auditPublisher = Mockito.mock(AuditPublisher.class);
        ProducerProperties producerProperties = Mockito.mock(ProducerProperties.class);
        Mockito.lenient().when(producerProperties.getSchemaName()).thenReturn("testSchema");
        operationalSystemContext = Mockito.mock(OperationalSystemContext.class);
        operationalAuditAspect = new OperationalAuditAspect(
                new OperationalEventEnricher(operationalSystemContext, true),
                new OperationalAuditPublisher(auditPublisher, producerProperties.getSchemaName()),
                new HashMap<>(), mockAWSHostContextCollector);

        // Initialize the test only object
        testOnlyPipEvaluator = Mockito.mock(DevExchangeDAOImpl.class);

        // Initialize the argument captor used for verification
        auditEventArgumentCaptor = ArgumentCaptor.forClass(AuditEvent.class);
    }

    @ParameterizedTest
    @ValueSource(ints = {200, 201, 202, 203, 204})
    void auditPipEvaluated200(final int statusCode) {
        // Arrange
        final DevExchangeResponse devExchangeResponseExpected = defaultDevExchangeResponse(statusCode);
        Mockito.when(testOnlyPipEvaluator.invokeDxAPI(Mockito.any())).thenReturn(devExchangeResponseExpected);
        final DevExchangeRequest devExchangeRequestExpected = defaultDevExchangeRequest();
        testOnlyPipEvaluator = OperationalAuditTestUtil.registerWithAspect(testOnlyPipEvaluator, operationalAuditAspect);

        // Act
        testOnlyPipEvaluator.invokeDxAPI(devExchangeRequestExpected);

        // Assert
        expectedPipEvaluatedEvent(OperationalEventOutcome.of(OperationalStatus.SUCCESS, String.valueOf(statusCode)),
                devExchangeRequestExpected,
                devExchangeResponseExpected,
                null);
    }

    @Test
    void auditPipEvaluatedSystemError() {
        // Arrange
        Mockito.when(testOnlyPipEvaluator.invokeDxAPI(Mockito.any(DevExchangeRequest.class)))
                .thenThrow(new Error("Oops."));
        final OperationalError operationalErrorExpected = OperationalError.builder()
                .developerText("Error: Oops.")
                .build();
        final DevExchangeRequest devExchangeRequest = defaultDevExchangeRequest();
        testOnlyPipEvaluator = OperationalAuditTestUtil.registerWithAspect(testOnlyPipEvaluator, operationalAuditAspect);


        // Act
        final Throwable throwable = Assertions.assertThrows(Error.class,
                () -> testOnlyPipEvaluator.invokeDxAPI(devExchangeRequest));

        // Assert
        Assertions.assertEquals("Oops.", throwable.getMessage());
        expectedPipEvaluatedEvent(OperationalEventOutcome.of(OperationalStatus.FAILURE, OperationalAuditConstants.UNDEFINED),
                devExchangeRequest,
                null,
                operationalErrorExpected);
    }

    @ParameterizedTest
    @ValueSource(ints = {400, 401, 402, 403, 404})
    void auditPipEvaluatedInvalidInput(final int statusCode) {
        // Arrange
        final DevExchangeResponse devExchangeResponseExpected = defaultDevExchangeResponse(statusCode);
        devExchangeResponseExpected.setDxResponseAudit("{\"type\":\"errorResponse\",\"id\":\"201188\","
                + "\"text\":\"User profile is locked\",\"developerText\":\"User profile is locked\","
                + "\"actions\":[]}");
        Mockito.when(testOnlyPipEvaluator.invokeDxAPI(Mockito.any(DevExchangeRequest.class)))
                .thenReturn(devExchangeResponseExpected);
        final OperationalError operationalErrorExpected = OperationalError.builder()
                .errorId("201188")
                .text("User profile is locked")
                .developerText("{\"type\":\"errorResponse\",\"id\":\"201188\","
                        + "\"text\":\"User profile is locked\",\"developerText\":\"User profile is locked\","
                        + "\"actions\":[]}")
                .build();
        final DevExchangeRequest devExchangeRequestExpected = defaultDevExchangeRequest();
        testOnlyPipEvaluator = OperationalAuditTestUtil.registerWithAspect(testOnlyPipEvaluator, operationalAuditAspect);

        // Act
        testOnlyPipEvaluator.invokeDxAPI(devExchangeRequestExpected);

        // Assert
        expectedPipEvaluatedEvent(OperationalEventOutcome.of(OperationalStatus.INVALID, String.valueOf(statusCode)),
                devExchangeRequestExpected,
                devExchangeResponseExpected,
                operationalErrorExpected);
    }

    @ParameterizedTest
    @ValueSource(ints = {500, 501, 502, 503, 506, 100, 300})
    void auditPipEvaluatedResponseError(final int statusCode) {
        // Arrange
        final DevExchangeResponse devExchangeResponseExpected = defaultDevExchangeResponse(statusCode);
        devExchangeResponseExpected.setDxResponseAudit("{\"id\":\"200003\",\"text\":\"System is not available : identityProfilesSendPin::socketTimedOut\"}");
        Mockito.when(testOnlyPipEvaluator.invokeDxAPI(Mockito.any(DevExchangeRequest.class)))
                .thenReturn(devExchangeResponseExpected);
        final OperationalError operationalErrorExpected = OperationalError.builder()
                .errorId("200003")
                .text("System is not available : identityProfilesSendPin::socketTimedOut")
                .developerText("{\"id\":\"200003\",\"text\":\"System is not available : identityProfilesSendPin::socketTimedOut\"}")
                .build();
        final DevExchangeRequest devExchangeRequestExpected = defaultDevExchangeRequest();
        testOnlyPipEvaluator = OperationalAuditTestUtil.registerWithAspect(testOnlyPipEvaluator, operationalAuditAspect);

        // Act
        testOnlyPipEvaluator.invokeDxAPI(devExchangeRequestExpected);

        // Assert
        expectedPipEvaluatedEvent(OperationalEventOutcome.of(OperationalStatus.FAILURE, String.valueOf(statusCode)),
                devExchangeRequestExpected,
                devExchangeResponseExpected,
                operationalErrorExpected);
    }

    @Test
    void auditPipEvaluatedResponseErrorUnexpectedFormat() {
        // Arrange
        final int httpStatusExpected = 503;
        final DevExchangeResponse devExchangeResponseExpected = defaultDevExchangeResponse(httpStatusExpected);
        devExchangeResponseExpected.setDxResponseAudit("foo bar!");
        Mockito.when(testOnlyPipEvaluator.invokeDxAPI(Mockito.any(DevExchangeRequest.class)))
                .thenReturn(devExchangeResponseExpected);
        final OperationalError operationalErrorExpected = OperationalError.builder()
                .developerText("foo bar!")
                .build();
        final DevExchangeRequest devExchangeRequestExpected = defaultDevExchangeRequest();
        testOnlyPipEvaluator = OperationalAuditTestUtil.registerWithAspect(testOnlyPipEvaluator, operationalAuditAspect);

        // Act
        testOnlyPipEvaluator.invokeDxAPI(devExchangeRequestExpected);

        // Assert
        expectedPipEvaluatedEvent(OperationalEventOutcome.of(OperationalStatus.FAILURE, String.valueOf(httpStatusExpected)),
                devExchangeRequestExpected,
                devExchangeResponseExpected,
                operationalErrorExpected);
    }

    private void expectedPipEvaluatedEvent(final OperationalEventOutcome expectedEventOutcome,
            final DevExchangeRequest expectedDevExchangeRequest,
            final DevExchangeResponse expectedDevExchangeResponse,
            final OperationalError expectedErrorMetadata) {

        final OperationalEvent operationalEvent = OperationalAuditTestUtil.captureOperationalEvent(
                auditPublisher, auditEventArgumentCaptor);

        // Entity naming assertions
        Assertions.assertEquals("PIP_EVALUATED", operationalEvent.getEventName());
        Assertions.assertEquals("PIP", operationalEvent.getEventEntity());
        Assertions.assertEquals("EVALUATED", operationalEvent.getEventAction());

        // Operation id
        Assertions.assertNotNull(operationalEvent.getEventOperationId());

        // Event Entity Name
        Assertions.assertEquals(expectedDevExchangeRequest.getServiceName(), operationalEvent.getEventEntityName());

        // Status
        Assertions.assertEquals(expectedEventOutcome, operationalEvent.getEventOutcome());

        // Timestamp/duration assertions
        Assertions.assertTrue(operationalEvent.getEventStartTimestamp() <= System.currentTimeMillis());
        Assertions.assertTrue(operationalEvent.getEventEndTimestamp() >= operationalEvent.getEventStartTimestamp());
        Assertions.assertEquals(operationalEvent.getEventEndTimestamp() - operationalEvent.getEventStartTimestamp(),
                Long.valueOf(operationalEvent.getEventDurationMs()));

        // System context
        Assertions.assertEquals(operationalSystemContext, operationalEvent.getSystemContext());

        // Error metadata
        Assertions.assertEquals(!expectedEventOutcome.getResult().equals(OperationalStatus.SUCCESS.name()), operationalEvent.getErrorMetadata() != null);
        if (operationalEvent.getErrorMetadata() != null) {
           Assertions.assertEquals(expectedErrorMetadata, operationalEvent.getErrorMetadata());
        }

        // Request context
        Assertions.assertNotNull(operationalEvent.getRequestMessageId());
        Assertions.assertNotNull(operationalEvent.getRequestCorrelationId());

        // Policy metadata assertions
        Assertions.assertNull(operationalEvent.getPolicyMetadata());

        // Pip Metadata Assertions
        Assertions.assertNotNull(operationalEvent.getPipMetadata());
        final OperationalPipMetadata operationalPipMetadata = operationalEvent.getPipMetadata();
        Assertions.assertEquals(expectedDevExchangeRequest.getPolicyInfo(), operationalPipMetadata.getPolicyInfo());
        Assertions.assertEquals(expectedDevExchangeRequest.getHttpMethod().toString(), operationalPipMetadata.getHttpMethod());
        Assertions.assertEquals(expectedDevExchangeRequest.getServiceName(), operationalPipMetadata.getEndpointServiceName());
        Assertions.assertEquals(expectedDevExchangeRequest.getUri().toString(), operationalPipMetadata.getEndpointTargetUri());
        Assertions.assertEquals(expectedDevExchangeRequest.getScheme(), operationalPipMetadata.getEndpointScheme());
        if (expectedDevExchangeResponse != null) {
            Assertions.assertEquals(String.valueOf(expectedDevExchangeResponse.getHttpStatus()),
                    operationalPipMetadata.getResponseHttpStatus());
        } else {
            Assertions.assertNull(operationalPipMetadata.getResponseHttpStatus());
        }

        // Policy Execution Context Assertions
        Assertions.assertNotNull(operationalEvent.getExecutionContext());
        Assertions.assertEquals(DX_API_KEY, operationalEvent.getExecutionContext().getApiKey());
        Assertions.assertEquals(DX_CONTENT_TYPE, operationalEvent.getExecutionContext().getContentType());
        Assertions.assertEquals(DX_CHANNEL_TYPE, operationalEvent.getExecutionContext().getChannelType());
        Assertions.assertNull(operationalEvent.getExecutionContext().getCountryCode());
        Assertions.assertNull(operationalEvent.getExecutionContext().getAcceptLanguage());
        Assertions.assertNull(operationalEvent.getExecutionContext().getSubDomain());
        Assertions.assertNull(operationalEvent.getExecutionContext().getUserAgent());
        Assertions.assertEquals(DX_ACCEPT, operationalEvent.getExecutionContext().getAccept());
        Assertions.assertEquals(DX_BUSINESS_EVENT, operationalEvent.getExecutionContext().getBusinessEvent());
        Assertions.assertEquals(DX_DOMAIN, operationalEvent.getExecutionContext().getDomain());
        Assertions.assertEquals(DX_X_JOURNEY, operationalEvent.getExecutionContext().getXJourneyPolicy());

        // Config Store Metadata
        Assertions.assertNull(operationalEvent.getConfigStoreMetadata());

        // Dmn Metadata
        Assertions.assertNull(operationalEvent.getDmnMetadata());

        // Supplemental Metadata
        Assertions.assertEquals(new ArrayList<>(), operationalEvent.getSupplementalMetadata());
    }

    private static DevExchangeRequest defaultDevExchangeRequest() {
        final DevExchangeRequest retVal = new DevExchangeRequest();
        retVal.setHttpHeaders(DX_HTTP_HEADERS_CURE_INCLUDED);
        retVal.setHttpMethod(HttpMethod.POST);
        retVal.setRequestBody(DX_REQUEST_BODY);
        retVal.setServiceName(DX_SERVICE_NAME);
        retVal.setUri(Mockito.mock(URI.class));
        Mockito.when(retVal.getUri().toString()).thenReturn(DX_URI);
        retVal.setPolicyInfo(DX_POLICY_INFO);
        retVal.setScheme(DX_SCHEME);
        return retVal;
    }

    private static DevExchangeResponse defaultDevExchangeResponse(final int statusCode) {
        final DevExchangeResponse retVal = new DevExchangeResponse();
        retVal.setDxResponse("{\"results\":{\"foo\":\"bar\"}}");
        retVal.setHttpStatus(statusCode);
        retVal.setHttpHeaders(DX_HTTP_HEADERS_CURE_INCLUDED);
        return retVal;
    }

    private static HttpHeaders httpHeadersAllIncluded() {
        final HttpHeaders retVal = new HttpHeaders();
        retVal.put("Channel-Type", Collections.singletonList(DX_CHANNEL_TYPE));
        retVal.put("Customer-IP-Address", Collections.singletonList("127.0.0.1"));
        retVal.put("Business-Event", Collections.singletonList(DX_BUSINESS_EVENT));
        retVal.put("Domain", Collections.singletonList(DX_DOMAIN));
        retVal.put("x-journey-policy", Collections.singletonList(DX_X_JOURNEY));
        retVal.put("Api-key", Collections.singletonList(DX_API_KEY));
        retVal.put("Client-Api-Key", Collections.singletonList("Mobile"));
        retVal.put("Client-Correlation-Id", Collections.singletonList("xxxxx-yyyyy-zzzz"));
        retVal.put("foo_client", Collections.singletonList("foo1")); // ASSUMED REDACTED B/C CONTAINS 'CLIENT'
        retVal.put("foo_customer", Collections.singletonList("foo2")); // ASSUMED REDACTED B/C CONTAINS 'CUSTOMER'
        retVal.put("Foo-Supplemental-Not-Redacted", Collections.singletonList("foo3"));
        retVal.put("Accept", Collections.singletonList(DX_ACCEPT));
        retVal.put("Content-Type", Collections.singletonList(DX_CONTENT_TYPE));
        retVal.put("x-upstream-env", Collections.singletonList("bar_upstream"));
        return retVal;
    }

}