package com.capitalone.identity.identitybuilder.policycore.operational_audit.mapper;

import com.capitalone.identity.identitybuilder.policycore.operational_audit.model.*;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.model.exchange_schema.OperationalProtocolHeader;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.model.type.OperationalEventType;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.util.OperationalAuditUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.*;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static com.capitalone.chassis.engine.model.constants.Constants.UNDEFINED;

/**
 * Enriches operational events with common operational attributes
 */
@AllArgsConstructor
public class OperationalEventEnricher {

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private final @NonNull OperationalSystemContext operationalSystemContext;
    private final boolean isDebugErrorInfoSuppressed;

    /**
     * Enriches the provided event with the attributes that are common across all timed operational events
     *
     * @param unenrichedOperationalEvent the unenriched operational event
     * @param commonAttributes           the common data required for enrichment
     * @return the enriched event
     */
    public OperationalEvent enrich(final @NonNull OperationalEvent unenrichedOperationalEvent,
                                   final @NonNull OperationalEventEnricher.CommonAttributes commonAttributes,
                                   final @NonNull OperationalEventEnricher.CyberAttributes cyberAttributes) {
        final OperationalEventType auditType = commonAttributes.getEventType();
        final String eventName = auditType.toString();
        EventAttributes eventAttributes = getEventAttributes(commonAttributes, Optional.empty());
        return unenrichedOperationalEvent.toBuilder()
                .eventName(eventName)
                .eventEntity(auditType.getEntity().toString())
                .eventAction(auditType.getAction().toString())
                .eventOutcome(commonAttributes.getEventOutcome())
                .systemContext(operationalSystemContext)
                .requestMessageId(commonAttributes.getRequestContextOptional()
                        .map(OperationalRequestContext::getMessageId).orElse(null))
                .requestCorrelationId(commonAttributes.getRequestContextOptional()
                        .map(OperationalRequestContext::getCorrelationId).orElse(null))
                .errorMetadata(suppressDebugInfoIfNeeded(commonAttributes.getErrorOptional()).orElse(null))
                .protocolHeaders(cyberAttributes.getProtocolHeaders().isEmpty() ?
                        defaultProtocolHeaders(eventName) : cyberAttributes.getProtocolHeaders())
                .protocolType(cyberAttributes.getProtocolType())
                .eventOperationId(commonAttributes.getEventOperationId())
                .eventStartTimestamp(commonAttributes.getStartTimestamp())
                .eventEndTimestamp(commonAttributes.getEndTimestamp())
                .eventDurationMs(Long.toString(
                        commonAttributes.getEndTimestamp() - commonAttributes.getStartTimestamp()))
                .amazonResourceName(cyberAttributes.getArn())
                .hostMacAddress(cyberAttributes.getMacAddress())
                .threadId(cyberAttributes.getThreadId())
                .sessionId(cyberAttributes.getSessionId())
                .processId(cyberAttributes.getProcessId())
                .requestId(cyberAttributes.getMessageId())
                .clientIpAddress(cyberAttributes.getClientIpAddress())
                .userId(cyberAttributes.getUserId())
                .eventReason(eventAttributes.getEventReason())
                .eventDetail(eventAttributes.getEventDetail())
                .build();
    }

    @SneakyThrows
    private EventAttributes getEventAttributes(CommonAttributes commonAttributes,  Optional<String> overflow) {
        EventAttributes attributes;

        Optional<OperationalError> optionalError = commonAttributes.getErrorOptional();
        if (optionalError.isPresent()) {
            OperationalError error = optionalError.get();
            attributes = new EventAttributes((isDebugErrorInfoSuppressed) ? "REDACTED" : error.getDeveloperText(),
                    (isDebugErrorInfoSuppressed) ? "REDACTED" : objectMapper.writeValueAsString(error));
        } else {
            attributes = new EventAttributes(commonAttributes.getEventType().name(),
                    overflow.isPresent() ? overflow.get() : UNDEFINED);
        }
        return attributes;
    }

    public List<OperationalProtocolHeader> defaultProtocolHeaders(final String eventName) {
        return Collections.singletonList(new OperationalProtocolHeader(eventName,
                OperationalAuditUtil.headerValueStopGap("N/A")));
    }

    private Optional<OperationalError> suppressDebugInfoIfNeeded(
            final Optional<OperationalError> operationalAuditError) {
        if (isDebugErrorInfoSuppressed && operationalAuditError.isPresent()) { // suppress if needed
            return Optional.of(operationalAuditError.get().toBuilder()
                    .stackTrace(null)
                    .build());
        } else {
            return operationalAuditError; // no-op
        }
    }

    /**
     * The inputs required or optional for common operational event audit enrichment
     */
    @Value
    @Builder(toBuilder = true)
    public static class CommonAttributes {
        @NonNull OperationalEventType eventType;
        @NonNull OperationalEventOutcome eventOutcome;
        @NonNull @Builder.Default Optional<OperationalError> errorOptional = Optional.empty(); // NOSONAR
        @NonNull @Builder.Default Optional<OperationalRequestContext> requestContextOptional = Optional.empty(); // NOSONAR
        @NonNull Long startTimestamp;
        @NonNull Long endTimestamp;
        String eventOperationId;
    }

    @Value
    @Builder(toBuilder = true)
    public static class CyberAttributes {
        String arn;
        String macAddress;
        String threadId;
        List<OperationalProtocolHeader> protocolHeaders;
        String processId;
        String clientIpAddress;
        String userId;
        String sessionId;
        String messageId;
        String protocolType;
    }

    @Value
    @Builder(toBuilder = true)
    static class EventAttributes {
        String eventReason;
        String eventDetail;
    }
}
