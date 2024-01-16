package com.capitalone.identity.identitybuilder.policycore.operational_audit;

import com.capitalone.chassis.engine.model.context.RequestContextHolder;
import com.capitalone.chassis.engine.model.error.ErrorResponse;
import com.capitalone.chassis.engine.model.exception.AbstractChassisException;
import com.capitalone.identity.identitybuilder.policycore.model.PolicyRequest;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.cyber.AWSHostContextCollector;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.model.*;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.model.type.OperationalEventType;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.util.OperationalAuditPublisher;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.util.OperationalAuditUtil;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.mapper.OperationalEventEnricher;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.mapper.PolicyEvaluatedEventMapper;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.model.OperationalError;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.model.policy.PolicyEvaluatedRequestAudit;
import com.capitalone.identity.identitybuilder.policycore.service.PolicyService;
import com.capitalone.identity.identitybuilder.policycore.service.logging.PolicyAuditContext;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class PolicyEvaluatedAuditor {

    private final InternalAuditor internalAuditor;

    PolicyEvaluatedAuditor(final @Autowired(required = false) InternalAuditor internalAuditor) {
        this.internalAuditor = internalAuditor;
    }

    public void audit(final PolicyRequest policyRequest,
            final PolicyService.ExecutedPolicyResponse executedPolicyResponse,
            final Exception nonResponseException, final Long startTimestamp,
            final PolicyAuditContext policyRequestContext) {
        if (internalAuditor == null) {
            return;
        }
        internalAuditor.audit(
                PolicyEvaluatedRequestAudit.builder()
                        .policyRequest(policyRequest)
                        .policyAuditContext(policyRequestContext)
                        .build(),
                executedPolicyResponse,
                nonResponseException,
                startTimestamp);
    }

    public void auditChassisError(final PolicyRequest policyRequest,
            final AbstractChassisException chassisException,
            final ErrorResponse chassisErrorResponse, final PolicyService.ExecutedPolicyResponse executedPolicyResponse,
            final Long startTimestamp, PolicyAuditContext policyAuditContext) {
        if (internalAuditor == null) {
            return;
        }
        internalAuditor.auditChassisError(
                PolicyEvaluatedRequestAudit.builder()
                        .policyRequest(policyRequest)
                        .policyAuditContext(policyAuditContext)
                        .build(),
                executedPolicyResponse,
                chassisException,
                Optional.ofNullable(chassisErrorResponse),
                startTimestamp);
    }

    @Log4j2
    @AllArgsConstructor
    public static class InternalAuditor {
        private final OperationalEventEnricher enrichmentHelper;
        private final OperationalAuditPublisher operationalAuditPublisher;
        private final OperationalEventType operationalEventType = OperationalEventType.POLICY_EVALUATED;
        private final AWSHostContextCollector awsHostContextCollector;
        private final PolicyEvaluatedEventMapper.Factory policyEvaluatedEventMapperFactory;

        void audit(final @NonNull PolicyEvaluatedRequestAudit request,
                final PolicyService.ExecutedPolicyResponse executedPolicyResponse,
                final Exception nonResponseException,
                final @NonNull Long startTimestamp) {
            final Long endTimestamp = System.currentTimeMillis();
            final OperationalRequestContext operationalRequestContext = OperationalAuditUtil.getChassisRequestContext();
            try {
                // Create event mapper
                final PolicyEvaluatedEventMapper policyEvaluatedEventMapper = policyEvaluatedEventMapperFactory.create(
                        request, executedPolicyResponse, nonResponseException);

                // Populate Common Attributes
                final OperationalEventOutcome outcome = policyEvaluatedEventMapper.toOperationalEventOutcome();
                final Optional<OperationalError> error = policyEvaluatedEventMapper.toOperationalError();
                final OperationalEventEnricher.CommonAttributes commonAttributes =
                        OperationalEventEnricher.CommonAttributes.builder()
                                .eventType(operationalEventType)
                                .errorOptional(error)
                                .eventOutcome(outcome)
                                .requestContextOptional(Optional.of(operationalRequestContext))
                                .eventOperationId(operationalRequestContext.getMessageId())
                                .startTimestamp(startTimestamp)
                                .endTimestamp(endTimestamp)
                                .build();
                final OperationalEventEnricher.CyberAttributes cyberAttributes =
                        awsHostContextCollector.setCyberAttributes(this.getClass().getName(),
                                RequestContextHolder.getRequestContextOrDefault());

                // Build and Publish the Event with entity-specific and common attributes combined
                operationalAuditPublisher.publish(enrichmentHelper.enrich(
                        policyEvaluatedEventMapper.buildWithEntitySpecificAttributes(),
                        commonAttributes, cyberAttributes));
            } catch (final Exception e) {
                log.error("Operational audit failure detected.", e);
            }
        }

        void auditChassisError(final @NonNull PolicyEvaluatedRequestAudit policyEvaluatedRequestAudit,
                final @NonNull PolicyService.ExecutedPolicyResponse executedPolicyResponse,
                final @NonNull AbstractChassisException chassisException,
                final Optional<ErrorResponse> chassisErrorResponse,
                final @NonNull Long startTimestamp) {
            final Long endTimestamp = System.currentTimeMillis();
            final OperationalRequestContext operationalRequestContext = OperationalAuditUtil.getChassisRequestContext();
            try {
                // Create event mapper
                final PolicyEvaluatedEventMapper policyEvaluatedEventMapper =
                        policyEvaluatedEventMapperFactory.create(policyEvaluatedRequestAudit, executedPolicyResponse, null);

                // Populate Common Attributes
                final OperationalEventOutcome eventOutcome = policyEvaluatedEventMapper.toOperationalEventOutcome();
                final OperationalError operationalError = OperationalAuditUtil.toChassisOperationalError(
                        chassisErrorResponse, chassisException);
                final OperationalEventEnricher.CommonAttributes commonAttributes =
                        OperationalEventEnricher.CommonAttributes.builder()
                                .eventType(operationalEventType)
                                .eventOutcome(eventOutcome)
                                .errorOptional(Optional.of(operationalError))
                                .requestContextOptional(Optional.of(operationalRequestContext))
                                .eventOperationId(operationalRequestContext.getMessageId())
                                .startTimestamp(startTimestamp)
                                .endTimestamp(endTimestamp)
                                .build();
                final OperationalEventEnricher.CyberAttributes cyberAttributes =
                        awsHostContextCollector.setCyberAttributes(this.getClass().getName(),
                                RequestContextHolder.getRequestContextOrDefault());

                // Build and Publish the Event with entity-specific and common attributes combined
                operationalAuditPublisher.publish(enrichmentHelper.enrich(
                        policyEvaluatedEventMapper.buildWithEntitySpecificAttributes(),
                        commonAttributes, cyberAttributes));
            } catch (final Exception e) {
                log.error("Operational audit failure detected.", e);
            }
        }
    }

}
