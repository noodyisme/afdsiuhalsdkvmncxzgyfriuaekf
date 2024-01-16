package com.capitalone.identity.identitybuilder.policycore.decisionengine;

import com.capitalone.chassis.cal1_5.builder.Cal1_5EventStreamDataBuilder;
import com.capitalone.chassis.engine.annotations.stream.EventStream;
import com.capitalone.identity.identitybuilder.policycore.service.constants.ApplicationConstants;
import com.newrelic.api.agent.Trace;
import org.springframework.stereotype.Component;

@Component
public class DecisionAuditLoggerImpl implements DecisionAuditLogger {

    @EventStream(eventName = ApplicationConstants.MASTERBUILDER_RULES_AUDIT_EVENT_NAME, standardEventStreams = {},
            customEventStreams = {@EventStream.CustomEventStream(type = EventStream.EventStreamType.AUDIT,
                    emitters = "sdpv3", eventStreamDataBuilder = Cal1_5EventStreamDataBuilder.BUILDER_TYPE)})
    @Trace
    public int auditDecisionResults(String transactionName,
                                    String ruleName,
                                    Object result) {
        return 0;
    }

}
