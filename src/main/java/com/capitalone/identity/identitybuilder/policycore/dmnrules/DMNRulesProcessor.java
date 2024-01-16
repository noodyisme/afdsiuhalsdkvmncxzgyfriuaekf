package com.capitalone.identity.identitybuilder.policycore.dmnrules;

import com.capitalone.chassis.engine.model.context.RequestContext;
import com.capitalone.chassis.engine.model.context.RequestContextHolder;
import com.capitalone.identity.identitybuilder.decisionengine.service.api.DecisionEvaluateRequest;
import com.capitalone.identity.identitybuilder.policycore.camel.PolicyConstants;
import com.capitalone.identity.identitybuilder.policycore.camel.external.dynamic.DynamicPolicyLoaderImpl;
import com.capitalone.identity.identitybuilder.policycore.decisionengine.DecisionResult;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.model.policy.OperationalAuditExecutionData;
import com.capitalone.identity.identitybuilder.policycore.service.constants.ApplicationConstants;
import com.capitalone.identity.identitybuilder.policycore.service.dmn.PolicyRulesProcessor;
import com.newrelic.api.agent.Trace;
import lombok.extern.log4j.Log4j2;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.logging.log4j.CloseableThreadContext;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * <p>Processor that executes business rules, with arguments set in the exchange body, and saves the result in
 * {@link ApplicationConstants#POLICY_RULE_RESULT} header, overwriting whatever is there.
 * </p>
 * <p>
 * If {@code writeToBody} constructor is used and set to {@code true}, the result will also be written to the body
 * of exchange.
 * </p>
 * <br>
 * <h3>The argument body is expected to consist of the following entries in a body object
 * of type {@link Map<String,Object>}</h3>
 * <ul>
 * <li>
 *     key={@link #BR_TRANSACTION_NAME}, value = name of the DMN rule file as a string.
 * </li>
 * <li>
 *      key={@link #BUSINESS_RULES_INPUT}, value = {@link HashMap<String,Object>} of arguments submitted to decision engine
 * </li>
 * <li>
 *     key={@link #BR_DECISION_OUTPUTNAME}, value = where to look for the decision engine engine library response
 *     in the map returned by decision engine library, i.e. key of returned Map<String,Object>.
 * </li>
 * </ul>
 *
 * <h3>Example body argument from a policy</h3>
 * ['initialPhoneNumber' : headers.mobilePhoneNumber,
 * 'customerPhoneNumberDetails' : ['entries':entries] ,
 * 'currentDate' : sdf.format(date)]
 */
@Log4j2
public class DMNRulesProcessor implements Processor {

    private static final String BR_TRANSACTION_NAME = "brTransactionName";
    private static final String BUSINESS_RULES_INPUT = "businessRulesInput";
    private static final String BR_DECISION_OUTPUTNAME = "brDecisionOutputname";

    private final PolicyRulesProcessor rulesProcessor;
    final boolean writeToBody;
    final boolean useLegacyMapResultParsing;

    public DMNRulesProcessor(PolicyRulesProcessor rulesProcessor, boolean writeToBody, boolean useLegacyResultParsing) {
        this.rulesProcessor = rulesProcessor;
        this.writeToBody = writeToBody;
        this.useLegacyMapResultParsing = useLegacyResultParsing;
    }

    @Override
    @Trace
    public void process(Exchange exchange) {
        runWithRequestContext(exchange, () -> {
            String policyName = exchange.getIn().getHeader(PolicyConstants.HEADER_POLICYNAME, String.class);
            String policyVersionPatch = exchange.getIn().getHeader(PolicyConstants.HEADER_POLICYVERSION_PATCH, String.class);

            Map<String, Object> body = (Map<String, Object>) exchange.getMessage().getBody();
            String runtimeId = DynamicPolicyLoaderImpl.getPatchIdentifier(policyName, policyVersionPatch);
            String dmnName = (String) body.get(BR_TRANSACTION_NAME);
            Map<String, Object> dmnInputObject = (Map<String, Object>) body.get(BUSINESS_RULES_INPUT);//defined in policy
            DecisionEvaluateRequest decisionEvaluateRequest = DecisionEvaluateRequest.builder()
                    .runtimeId(runtimeId)
                    .dmnName(dmnName)
                    .input(dmnInputObject)
                    .build();
            String dmnDecisionOutputName = (String) body.get(BR_DECISION_OUTPUTNAME);
            //where to put result
            DecisionResult result = rulesProcessor.execute(PolicyRulesProcessor.DecisionEngineRequest
                            .builder()
                            .decisionEvaluateRequest(decisionEvaluateRequest)
                            .dmnDecisionOutputName(dmnDecisionOutputName)
                            .exchangeHeaders(exchange.getIn().getHeaders())
                            .build(),
                    useLegacyMapResultParsing);
            exchange.getIn().setHeader(ApplicationConstants.POLICY_RULE_RESULT, result);
            Optional.ofNullable(OperationalAuditExecutionData.fromMessage(exchange.getMessage()))
                    .ifPresent(header -> header.getDmnsEvaluated().add(String.join("/", runtimeId, dmnName)));
            if (writeToBody) {
                exchange.getIn().setBody(result);
            }
        });

    }

    private void runWithRequestContext(Exchange exchange, Runnable runnable) {
        RequestContext currentContext = RequestContextHolder.getRequestContextOrDefault();
        try (final CloseableThreadContext.Instance ctc = CloseableThreadContext.put("DecisionEngineRequestId", UUID.randomUUID().toString())) {
            if (currentContext.isNotNull()) {
                runnable.run();
            } else {
                RequestContext ctx = exchange.getIn().getHeader(PolicyConstants.HEADER_AUDIT_CONTEXT, RequestContext.class);
                RequestContextHolder.put(ctx);
                try {
                    runnable.run();
                } finally {
                    RequestContextHolder.clearRequestContext();
                }
            }
        }
    }

    public boolean isWriteToBody() {
        return writeToBody;
    }

    public boolean isUseLegacyMapResultParsing() {
        return useLegacyMapResultParsing;
    }
}
