package com.capitalone.identity.identitybuilder.policycore.operational_audit.util;

import com.capitalone.identity.identitybuilder.policycore.operational_audit.model.OperationalEventOutcome;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.model.OperationalStatus;

public class OperationalAuditConstants {

    public static final String UNDEFINED = "UNDEFINED";
    public static final OperationalEventOutcome OUTCOME_UNDEFINED = new OperationalEventOutcome(UNDEFINED, UNDEFINED);
    public static final OperationalEventOutcome OUTCOME_SUCCESS_INFO = OperationalEventOutcome.of(OperationalStatus.SUCCESS, "INFO");
    public static final OperationalEventOutcome OUTCOME_FAILURE_ERROR = OperationalEventOutcome.of(OperationalStatus.FAILURE, "ERROR");

    private OperationalAuditConstants() {
    }
}
