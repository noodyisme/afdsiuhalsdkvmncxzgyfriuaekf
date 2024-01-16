package com.capitalone.identity.identitybuilder.policycore.operational_audit.mapper;

import com.capitalone.chassis.engine.model.context.RequestContext;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.config.OperationalAuditConfigTestUtil;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.model.OperationalEvent;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.model.OperationalSystemContext;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.cyber.AWSHostContextCollector;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.util.MockHostContext;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.util.MockRequestContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static com.capitalone.identity.identitybuilder.policycore.operational_audit.util.OperationalAuditConstants.UNDEFINED;

class OperationalEventEnricherTest {

    private final AWSHostContextCollector mockAWSHostContextCollector = new AWSHostContextCollector(new MockHostContext());
    private final RequestContext mockRequestContext = new MockRequestContext().MockRequestContext();
    // Dependencies
    private OperationalSystemContext operationalSystemContext;
    // Object under test
    private OperationalEventEnricher operationalEventEnricher;
    private OperationalEventEnricher.CyberAttributes cyberAttributes;


    @BeforeEach
    void setup() {
        operationalSystemContext = Mockito.mock(OperationalSystemContext.class);
        operationalEventEnricher = new OperationalEventEnricher(operationalSystemContext, false);

        cyberAttributes = mockAWSHostContextCollector.setCyberAttributes(this.getClass().getName(), mockRequestContext);
    }

    @Test
    void enrichTimeEventSuccess() {
        // Arrange
        final OperationalEventEnricher.CommonAttributes commonAttributes = OperationalAuditConfigTestUtil.buildDefaultCommonEnrichmentInput();
        // Act
        final OperationalEvent result = operationalEventEnricher.enrich(OperationalEvent.builder().build(), commonAttributes, cyberAttributes);

        // Assert
        // (common attributes)
        Assertions.assertEquals(commonAttributes.getEventType().name(), result.getEventName());
        Assertions.assertEquals(commonAttributes.getEventType().getEntity().name(), result.getEventEntity());
        Assertions.assertEquals(commonAttributes.getEventType().getAction().name(), result.getEventAction());
        Assertions.assertEquals(commonAttributes.getEventOutcome(), result.getEventOutcome());
        Assertions.assertEquals(operationalSystemContext, result.getSystemContext());
        Assertions.assertNull(result.getErrorMetadata());
        Assertions.assertNull(result.getRequestMessageId());
        Assertions.assertNull(result.getRequestCorrelationId());
        Assertions.assertEquals(commonAttributes.getEventOperationId(), result.getEventOperationId());
        Assertions.assertEquals(commonAttributes.getStartTimestamp(), result.getEventStartTimestamp());
        Assertions.assertEquals(commonAttributes.getEndTimestamp(), result.getEventEndTimestamp());
        Assertions.assertEquals(
                Long.toString(commonAttributes.getEndTimestamp() - commonAttributes.getStartTimestamp()),
                result.getEventDurationMs());
        // (cyber attributes)
        Assertions.assertEquals(cyberAttributes.getThreadId(), result.getThreadId());
        Assertions.assertEquals(cyberAttributes.getArn(), result.getAmazonResourceName());
        Assertions.assertEquals(cyberAttributes.getMacAddress(), result.getHostMacAddress());
        Assertions.assertEquals(cyberAttributes.getProcessId(), result.getProcessId());
        Assertions.assertEquals(cyberAttributes.getMessageId(), result.getRequestId());
        Assertions.assertEquals(cyberAttributes.getSessionId(), result.getSessionId());
        Assertions.assertEquals(cyberAttributes.getUserId(), result.getUserId());
        Assertions.assertEquals(operationalEventEnricher.defaultProtocolHeaders(result.getEventName()), result.getProtocolHeaders());
        Assertions.assertEquals(cyberAttributes.getClientIpAddress(), result.getClientIpAddress());
        // (cyber event attributes)
        Assertions.assertEquals(commonAttributes.getEventType().name(), result.getEventReason());
        Assertions.assertEquals(UNDEFINED, result.getEventDetail());
    }

    @Test
    void enrichUntimedEventSuccess() {
        // Arrange
        final OperationalEventEnricher.CommonAttributes commonAttributes = OperationalAuditConfigTestUtil.buildDefaultCommonEnrichmentInput();

        // Act
        final OperationalEvent result = operationalEventEnricher.enrich(OperationalEvent.builder().build(), commonAttributes, cyberAttributes);

        // Assert
        // (common attributes)
        Assertions.assertEquals(commonAttributes.getEventType().name(), result.getEventName());
        Assertions.assertEquals(commonAttributes.getEventType().getEntity().name(), result.getEventEntity());
        Assertions.assertEquals(commonAttributes.getEventType().getAction().name(), result.getEventAction());
        Assertions.assertEquals(commonAttributes.getEventOutcome(), result.getEventOutcome());
        Assertions.assertEquals(operationalSystemContext, result.getSystemContext());
        Assertions.assertNull(result.getErrorMetadata());
        Assertions.assertNull(result.getRequestMessageId());
        Assertions.assertNull(result.getRequestCorrelationId());

        // (cyber attributes)
        Assertions.assertEquals(cyberAttributes.getThreadId(), result.getThreadId());
        Assertions.assertEquals(cyberAttributes.getArn(), result.getAmazonResourceName());
        Assertions.assertEquals(cyberAttributes.getMacAddress(), result.getHostMacAddress());
        Assertions.assertEquals(cyberAttributes.getProcessId(), result.getProcessId());
        Assertions.assertEquals(cyberAttributes.getMessageId(), result.getRequestId());
        Assertions.assertEquals(cyberAttributes.getSessionId(), result.getSessionId());
        Assertions.assertEquals(cyberAttributes.getUserId(), result.getUserId());
        Assertions.assertEquals(operationalEventEnricher.defaultProtocolHeaders(result.getEventName()), result.getProtocolHeaders());
        Assertions.assertEquals(cyberAttributes.getClientIpAddress(), result.getClientIpAddress());
        // (cyber event attributes)
        Assertions.assertEquals(commonAttributes.getEventType().name(), result.getEventReason());
        Assertions.assertEquals(UNDEFINED, result.getEventDetail());
    }

}