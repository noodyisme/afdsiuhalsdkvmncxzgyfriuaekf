package com.capitalone.identity.identitybuilder.policycore.operational_audit.util;

import com.capitalone.identity.identitybuilder.audit.models.AuditEvent;
import com.capitalone.identity.identitybuilder.audit.sdpv4.AuditPublisher;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.model.OperationalEvent;
import lombok.AllArgsConstructor;
import lombok.NonNull;

/**
 * Audit publisher for operational events
 */
@AllArgsConstructor
public class OperationalAuditPublisher {

    private final @NonNull AuditPublisher auditPublisher;
    private final @NonNull String schemaName;

    /**
     * Publishes an operational audit event to onestream
     * @param operationalAuditEvent the operational audit event
     */
    public void publish(final OperationalEvent operationalAuditEvent) {

        // Build the event envelope
        final AuditEvent event = AuditEvent.builder()
                .message(operationalAuditEvent)
                .schemaName(schemaName)
                .build();
        // Publish
        auditPublisher.publish(event);
    }

}
