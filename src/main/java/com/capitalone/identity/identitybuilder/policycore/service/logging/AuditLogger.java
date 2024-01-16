package com.capitalone.identity.identitybuilder.policycore.service.logging;

import com.capitalone.chassis.cal1_5.builder.Cal1_5EventStreamDataBuilder;
import com.capitalone.chassis.engine.annotations.stream.EventStream;
import com.capitalone.chassis.engine.model.context.RequestContextHolder;
import com.capitalone.chassis.engine.model.error.ErrorResponse;
import com.capitalone.chassis.engine.model.exception.AbstractChassisException;
import com.capitalone.chassis.engine.model.exception.ChassisSystemException;
import com.capitalone.identity.identitybuilder.policycore.model.ChassisErrorAuditResponse;
import com.capitalone.identity.identitybuilder.policycore.model.ExecutePolicyAuditResponse;
import com.capitalone.identity.identitybuilder.policycore.model.PolicyRequest;
import com.capitalone.identity.identitybuilder.policycore.model.PolicyResponse;
import com.capitalone.identity.identitybuilder.policycore.service.PolicyService;
import com.capitalone.identity.identitybuilder.policycore.service.constants.ApplicationConstants;
import com.capitalone.identity.identitybuilder.policycore.service.util.JsonUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.newrelic.api.agent.Trace;
import org.apache.camel.PropertyInject;
import org.apache.camel.spi.Metadata;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class AuditLogger {

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Metadata(label = "configuration", description = "Policy Service Logging fields to filter")
    @PropertyInject("policy.service.logs.fieldstofilter:")
    private String fieldsToFilter;

    @EventStream(eventName = ApplicationConstants.POLICY_CORE_API_REQUEST, standardEventStreams = {}, customEventStreams = {
            @EventStream.CustomEventStream(type = EventStream.EventStreamType.AUDIT, emitters = "sdpv3", eventStreamDataBuilder = Cal1_5EventStreamDataBuilder.BUILDER_TYPE)})
    @Trace
    public ExecutePolicyAuditResponse logAudits(PolicyRequest policyRequest, String policyName, String version,
                                                String stepName, PolicyService.ExecutedPolicyResponse executedPolicyResponse, String parentPolicyName) { //NOSONAR
        return auditResponse(executedPolicyResponse, stepName);
    }

    @EventStream(eventName = ApplicationConstants.POLICY_CORE_API_REQUEST, standardEventStreams = {}, customEventStreams = {
            @EventStream.CustomEventStream(type = EventStream.EventStreamType.AUDIT, emitters = "sdpv3", eventStreamDataBuilder = Cal1_5EventStreamDataBuilder.BUILDER_TYPE)})
    @Trace
    public ChassisErrorAuditResponse logChassisAudits(PolicyRequest policyRequest, String policyName, String version, String stepName,
                                                      ErrorResponse response, PolicyService.ExecutedPolicyResponse executedPolicyResponse, AbstractChassisException exception) {

        Optional<AbstractChassisException> executedPolicyError = Optional.ofNullable(executedPolicyResponse.getResult().getException());
        final int httpStatus = executedPolicyError.isPresent() ? executedPolicyError.get().getHttpStatusCode() : exception.getHttpStatusCode();
        RequestContextHolder.getRequestContextOrDefault().getResponseData().setStatusPreference(httpStatus);
        return new ChassisErrorAuditResponse(response);
    }

    private String convertObjectToAuditString(Object object) {
        try {
            String resultsString = object != null ? objectMapper.writeValueAsString(object) : "";
            return JsonUtil.getFilteredResponse(resultsString, this.fieldsToFilter, "policyService");
        } catch (Exception e) {
            throw new ChassisSystemException("Error converting the object to string", e);
        }
    }

    public String getFieldsToFilter() {
        return fieldsToFilter;
    }

    public void setFieldsToFilter(String fieldsToFilter) {
        this.fieldsToFilter = fieldsToFilter;
    }

    private ExecutePolicyAuditResponse auditResponse(final PolicyService.ExecutedPolicyResponse executedPolicyResponse,
                                                     final String stepName) {
        final PolicyResponse policyResponseNullable = executedPolicyResponse.getResult().getPolicyResponse();
        if (policyResponseNullable != null) {
            return ExecutePolicyAuditResponse.create(policyResponseNullable,
                    convertObjectToAuditString(policyResponseNullable.getResults()),
                    stepName);
        }
        return null;
    }
}
