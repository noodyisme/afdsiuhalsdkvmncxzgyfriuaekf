package com.capitalone.identity.identitybuilder.policycore.operational_audit.model.policy;

import com.capitalone.identity.identitybuilder.policycore.camel.PolicyConstants;
import lombok.NoArgsConstructor;
import lombok.Value;
import org.apache.camel.Message;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Metadata gathered from policy header for operational audit purposes.
 */
@Value
@NoArgsConstructor
public class OperationalAuditExecutionData {
    List<String> pipsEvaluated = Collections.synchronizedList(new ArrayList<>());
    List<String> dmnsEvaluated = Collections.synchronizedList(new ArrayList<>());

    public static OperationalAuditExecutionData fromMessage(final Message message) {
        if (message == null) {
            return null;
        }
        return (OperationalAuditExecutionData) message.getHeader(PolicyConstants.OPERATIONAL_AUDIT_EXECUTION_DATA);
    }

}
