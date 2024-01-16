package com.capitalone.identity.identitybuilder.policycore.decisionengine;

public interface DecisionAuditLogger {

    /**
     * @param transactionName        name of dmn transaction
     * @param ruleName             name of decision that was made
     * @param result decision engine response
     * @return responseDecisionEngine
     */
    int auditDecisionResults(String transactionName,
                             String ruleName,
                             Object result);
}
