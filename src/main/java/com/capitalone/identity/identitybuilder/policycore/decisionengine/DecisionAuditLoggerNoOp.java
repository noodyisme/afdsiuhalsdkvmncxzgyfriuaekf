package com.capitalone.identity.identitybuilder.policycore.decisionengine;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Component
@Primary
@ConditionalOnProperty(
        value = "identitybuilder.policycore.feature.decisionengine.output-audit-logger.enabled",
        havingValue = "false"
)
public class DecisionAuditLoggerNoOp implements DecisionAuditLogger {

    public int auditDecisionResults(String transactionName, String ruleName, Object result) {
        return 0;
    }

}
