package com.capitalone.identity.identitybuilder.policycore.operational_audit.mapper;

import com.capitalone.chassis.engine.model.context.RequestContextHolder;
import com.capitalone.identity.identitybuilder.audit.util.TimeUtils;
import com.capitalone.identity.identitybuilder.policycore.model.*;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.model.*;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.model.policy.OperationalExecutionContext;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.model.policy.OperationalPolicyMetadata;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.model.policy.PolicyEvaluatedRequestAudit;
import com.capitalone.identity.identitybuilder.policycore.service.PolicyService;
import com.capitalone.identity.identitybuilder.policycore.service.constants.ApplicationConstants;
import com.capitalone.identity.identitybuilder.policycore.service.logging.PolicyAuditContext;
import com.capitalone.identity.identitybuilder.policycore.service.util.JsonUtil;
import com.google.common.collect.Lists;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.capitalone.identity.identitybuilder.policycore.operational_audit.util.OperationalAuditUtil.DEFAULT_POLICY_ERROR_TEXT;

@Log4j2
@AllArgsConstructor
public class PolicyEvaluatedEventMapper implements OperationalEventMapper {

    private final @NonNull PolicyEvaluatedRequestAudit requestAudit;
    private final @NonNull Optional<PolicyService.ExecutedPolicyResponse> executedPolicyResponseOptional;
    private final @NonNull Optional<PolicyService.Result> policyServiceResultOptional;
    private final @NonNull Optional<PolicyResponse> policyResponseOptional;
    private final @NonNull Optional<Exception> policyResponseExceptionOptional;
    private final @NonNull Optional<Throwable> nonResponseThrowableOptional;
    private final @NonNull String statusCode;
    private final String fieldsToFilter;

    public OperationalEvent buildWithEntitySpecificAttributes() {
        // Initiate builder
        final OperationalEvent.OperationalEventBuilder builder = OperationalEvent.builder();

        // Initialize output objects
        final Optional<ProcessMetadata> processMetadataOptional = policyResponseOptional
                .map(PolicyResponse::getMetadata);
        final Optional<String> policyVersionExecutedOptional = executedPolicyResponseOptional
                .map(PolicyService.ExecutedPolicyResponse::getExecutedPolicyVersion);

        // Initialize input objects
        final PolicyRequest policyRequest = requestAudit.getPolicyRequest();
        final PolicyAuditContext policyAuditContext = requestAudit.getPolicyAuditContext();
        final String policyVersionRequested = policyRequest.getPolicyVersionRequested();
        final String policyName = policyRequest.getPolicyName();

        // eventEntityName
        final String nameVersion = policyVersionExecutedOptional.orElse(policyVersionRequested);
        if (StringUtils.isNotEmpty(policyName) && StringUtils.isNotEmpty(nameVersion)) {
            builder.eventEntityName(String.format("%s_%s", policyName, nameVersion));
        }

        // policy metadata
        final OperationalPolicyMetadata.OperationalPolicyMetadataBuilder policyMetadataBuilder = OperationalPolicyMetadata.builder();
        processMetadataOptional.ifPresent(p -> {
            policyMetadataBuilder.processId(p.getResourceId());
            policyMetadataBuilder.effectiveNextStep(p.getEffectiveNextStep());
            policyMetadataBuilder.stepExpiration(TimeUtils.ISO8601AssumedUTC(p.getStepExpiration()));
            policyMetadataBuilder.availableNextSteps(Lists.newArrayList(p.getAvailableNextSteps()));
            policyMetadataBuilder.stepsCompleted(p.getStepsCompleted());
        });
        if (!processMetadataOptional.isPresent()) {
            policyMetadataBuilder.processId(policyRequest.getProcessId());
        }
        policyMetadataBuilder.policyName(policyName);
        policyMetadataBuilder.policyVersionRequested(policyVersionRequested);
        policyMetadataBuilder.step(policyRequest.getStep());
        policyMetadataBuilder.requestType(policyRequest.getPolicyRequestType());
        executedPolicyResponseOptional.ifPresent(r -> policyMetadataBuilder.versionExecuted(r.getExecutedPolicyVersion()));
        executedPolicyResponseOptional.map(PolicyService.ExecutedPolicyResponse::getExecutionData).ifPresent(headerInfo -> {
            policyMetadataBuilder.pipsEvaluated(headerInfo.getPipsEvaluated());
            policyMetadataBuilder.dmnsEvaluated(headerInfo.getDmnsEvaluated());
        });
        // P2P attributes
        if (policyAuditContext.getParentPolicyName() != null) {
            policyMetadataBuilder.p2PChildrenPolicyNames(policyName);
            policyMetadataBuilder.p2PParentPolicyName(policyAuditContext.getParentPolicyName());
        }
        policyMetadataBuilder.clientId(policyRequest.getClientId());
        builder.policyMetadata(policyMetadataBuilder.build());

        // execution context
        final OperationalExecutionContext.OperationalExecutionContextBuilder executionContextBuilder = OperationalExecutionContext.builder();
        final Map<String, String> dxHeaders = policyRequest.getDxHeaders();
        executionContextBuilder.apiKey(dxHeaders.get(ApplicationConstants.API_KEY));
        executionContextBuilder.businessEvent(policyRequest.getBusinessEvent());
        executionContextBuilder.contentType(policyAuditContext.getContentType());
        executionContextBuilder.channelType(dxHeaders.get(ApplicationConstants.CHANNEL_TYPE));
        executionContextBuilder.countryCode(dxHeaders.get(ApplicationConstants.COUNTRY_CODE));
        executionContextBuilder.acceptLanguage(policyAuditContext.getAcceptLanguage());
        executionContextBuilder.xJourneyPolicy(policyRequest.getXJourneyPolicy());
        executionContextBuilder.domain(policyAuditContext.getDomain());
        executionContextBuilder.subDomain(policyAuditContext.getSubDomain());
        executionContextBuilder.userAgent(policyAuditContext.getUserAgent());
        executionContextBuilder.accept(policyAuditContext.getAccept());
        builder.executionContext(executionContextBuilder.build());

        // supplemental attributes list initialization
        final List<OperationalSupplementalAttribute> supplementalAttributeList = new ArrayList<>();

        // abac
        executedPolicyResponseOptional.map(PolicyService.ExecutedPolicyResponse::getAbacResponse).ifPresent(abacResponse -> {
            supplementalAttributeList.add(new OperationalSupplementalAttribute("abac_is_strict_enforcement",
                    String.valueOf(abacResponse.isStrictEnforcementFlag())));
            supplementalAttributeList.add(new OperationalSupplementalAttribute("abac_is_authorized",
                    String.valueOf(abacResponse.isAuthorized())));
            Optional.ofNullable(abacResponse.getErrorCode()).ifPresent(errorCode -> supplementalAttributeList.add(
                    new OperationalSupplementalAttribute("abac_error_code", abacResponse.getErrorCode())));
            Optional.ofNullable(abacResponse.getText()).ifPresent(text -> supplementalAttributeList.add(
                    new OperationalSupplementalAttribute("abac_text", abacResponse.getText())));
        });

        // mock mode attribute
        executedPolicyResponseOptional.map(PolicyService.ExecutedPolicyResponse::getMockModeConfig)
                .ifPresent(mockModeConfig -> supplementalAttributeList.add(new OperationalSupplementalAttribute("mock_mode_configuration_value", String.valueOf(mockModeConfig))));

        // chassis http status
        supplementalAttributeList.add(new OperationalSupplementalAttribute("http_status", statusCode));

        // enrich builder with any supplemental attributes
        builder.supplementalMetadata(supplementalAttributeList);

        // Parsing policy request and response
        try {
            builder.protocolRequestDetail(policyRequest.getPolicyParametersAudit());
            executedPolicyResponseOptional.ifPresent(executedPolicyResponse -> {
                if (executedPolicyResponse.getResult() instanceof PolicyService.Result.SystemError)
                    builder.protocolResponseDetail(JsonUtil.getFilteredResponse(executedPolicyResponse.getResult().getException().getApiError(), fieldsToFilter));
                else if (executedPolicyResponse.getResult() instanceof PolicyService.Result.PolicySuccess)
                    builder.protocolResponseDetail(JsonUtil.getFilteredResponse(executedPolicyResponse.getResult().getPolicyResponse().getResults(), fieldsToFilter));
            });
        } catch (Exception ex) {
            log.error("Unexpected error during request/response serialization {}", ex);
        }

        // Return the event for further enrichment
        return builder.build();
    }

    public OperationalEventOutcome toOperationalEventOutcome() {
        final Optional<PolicyStatus> policyStatus = policyServiceResultOptional.map(
                PolicyService.Result::getPolicyResponse).map(PolicyResponse::getPolicyStatus);
        if (!policyStatus.isPresent()) {
            return OperationalEventOutcome.of(OperationalStatus.FAILURE, statusCode);
        }
        switch (policyStatus.get()) {
            case SUCCESS:
                return OperationalEventOutcome.of(OperationalStatus.SUCCESS, statusCode);
            case INVALID:
                return OperationalEventOutcome.of(OperationalStatus.INVALID, statusCode);
            case FAILURE:
                return OperationalEventOutcome.of(OperationalStatus.FAILURE, statusCode);
            default:
                log.error("Unexpected PolicyStatus {} processed during operational audit. Assumed status of failure", policyStatus);
                return OperationalEventOutcome.of(OperationalStatus.FAILURE, statusCode);
        }
    }

    public Optional<OperationalError> toOperationalError() {
        if (!executedPolicyResponseOptional.isPresent()) {
            return Optional.of(defaultPolicyOperationalError(nonResponseThrowableOptional));
        }
        final Optional<ErrorInfo> errorInfoOptional = policyResponseOptional.map(PolicyResponse::getErrorInfo);
        final Optional<Throwable> throwableOptional = Optional.ofNullable(
                policyResponseExceptionOptional
                        .map(Throwable.class::cast)
                        .orElse(nonResponseThrowableOptional.orElse(null)));

        if (errorInfoOptional.isPresent()) {
            final ErrorInfo errorInfo = errorInfoOptional.get();
            return Optional.of(OperationalError.builder()
                    .text(errorInfo.getText())
                    .errorId(errorInfo.getId())
                    .developerText(errorInfo.getDeveloperText())
                    .stackTrace(throwableOptional.map(ExceptionUtils::getStackTrace).orElse(null))
                    .build());
        }
        if (throwableOptional.isPresent()) {
            return Optional.of(defaultPolicyOperationalError(nonResponseThrowableOptional));
        }
        return Optional.empty();
    }



    private static OperationalError defaultPolicyOperationalError(final Optional<Throwable> throwableOptional) {
        return OperationalError.builder()
                .developerText(throwableOptional.map(Throwable::getMessage).orElse(null))
                .stackTrace(throwableOptional.map(ExceptionUtils::getStackTrace).orElse(null))
                .text(DEFAULT_POLICY_ERROR_TEXT)
                .build();
    }

    @AllArgsConstructor
    public static class Factory {
        private final ChassisStatusCodeResolver chassisStatusCodeResolver;

        private final String fieldsToFilter;

        public @NonNull PolicyEvaluatedEventMapper create(
                final PolicyEvaluatedRequestAudit policyEvaluatedRequestAudit,
                final PolicyService.ExecutedPolicyResponse executedPolicyResponse,
                final Throwable nonResponseThrowable) {

            // Extract main attributes for auditing
            final Optional<PolicyService.ExecutedPolicyResponse> executedPolicyResponseOptional =
                    Optional.ofNullable(executedPolicyResponse);
            final Optional<PolicyService.Result> policyServiceResultOptional =
                    executedPolicyResponseOptional.map(PolicyService.ExecutedPolicyResponse::getResult);
            final Optional<PolicyResponse> policyResultSuccessOptional =
                    policyServiceResultOptional.map(PolicyService.Result::getPolicyResponse);
            final Optional<Exception> policyResultExceptionOptional =
                    policyServiceResultOptional.map(PolicyService.Result::getException);
            final Optional<Throwable> nonResponseThrowableOptional = Optional.ofNullable(nonResponseThrowable);
            final Optional<Throwable> throwableOptional = policyResultExceptionOptional
                    .map(throwable -> Optional.of((Throwable) throwable))
                    .orElse(nonResponseThrowableOptional);
            final Integer statusCode = chassisStatusCodeResolver.resolveHttpStatusCode(
                    RequestContextHolder.getRequestContextOrDefault(),
                    throwableOptional);

            return new PolicyEvaluatedEventMapper(
                    policyEvaluatedRequestAudit,
                    executedPolicyResponseOptional,
                    policyServiceResultOptional,
                    policyResultSuccessOptional,
                    policyResultExceptionOptional,
                    nonResponseThrowableOptional,
                    String.valueOf(statusCode),
                    fieldsToFilter);
        }
    }
}
