package com.capitalone.identity.identitybuilder.policycore.service;

import com.capitalone.chassis.cal1_5.builder.Cal1_5EventStreamDataBuilder;
import com.capitalone.chassis.engine.annotations.stream.EventStream;
import com.capitalone.chassis.engine.model.context.RequestContextHolder;
import com.capitalone.chassis.engine.model.exception.AbstractChassisException;
import com.capitalone.chassis.engine.model.exception.ChassisBusinessException;
import com.capitalone.chassis.engine.model.exception.ChassisErrorCode;
import com.capitalone.chassis.engine.model.exception.ChassisSystemException;
import com.capitalone.chassis.engine.model.exception.NotFoundException;
import com.capitalone.chassis.engine.model.exception.RequestValidationException;
import com.capitalone.identity.identitybuilder.model.PolicyInfo;
import com.capitalone.identity.identitybuilder.policycore.abac.model.AbacResponse;
import com.capitalone.identity.identitybuilder.policycore.abac.model.PolicyAuthAction;
import com.capitalone.identity.identitybuilder.policycore.abac.model.PolicyAuthResource;
import com.capitalone.identity.identitybuilder.policycore.abac.model.PolicyAuthSubject;
import com.capitalone.identity.identitybuilder.policycore.abac.service.AuthorizationEnforcer;
import com.capitalone.identity.identitybuilder.policycore.camel.PolicyConstants;
import com.capitalone.identity.identitybuilder.policycore.camel.util.CamelCacheUtil;
import com.capitalone.identity.identitybuilder.policycore.configmanagement.UnsupportedBusinessEventException;
import com.capitalone.identity.identitybuilder.policycore.model.APIRequest;
import com.capitalone.identity.identitybuilder.policycore.model.APIResponse;
import com.capitalone.identity.identitybuilder.policycore.model.ErrorInfo;
import com.capitalone.identity.identitybuilder.policycore.model.PolicyMetadata;
import com.capitalone.identity.identitybuilder.policycore.model.PolicyRequest;
import com.capitalone.identity.identitybuilder.policycore.model.PolicyResponse;
import com.capitalone.identity.identitybuilder.policycore.model.PolicyState;
import com.capitalone.identity.identitybuilder.policycore.model.PolicyStatus;
import com.capitalone.identity.identitybuilder.policycore.model.ProcessMetadata;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.model.policy.OperationalAuditExecutionData;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.model.policy.PolicyRequestType;
import com.capitalone.identity.identitybuilder.policycore.policytopolicy.camel.PolicyFailureException;
import com.capitalone.identity.identitybuilder.policycore.policytopolicy.camel.PolicyToPolicyException;
import com.capitalone.identity.identitybuilder.policycore.schema.SchemaManager;
import com.capitalone.identity.identitybuilder.policycore.service.exception.CustomPolicyException;
import com.capitalone.identity.identitybuilder.policycore.service.exception.DownstreamException;
import com.capitalone.identity.identitybuilder.policycore.service.exception.PolicyCacheException;
import com.capitalone.identity.identitybuilder.policycore.service.logging.AuditLogger;
import com.capitalone.identity.identitybuilder.policycore.service.logging.PolicyAuditContext;
import com.capitalone.identity.identitybuilder.policycore.service.logging.PolicyServiceExecutor;
import com.capitalone.identity.identitybuilder.policycore.service.util.PathParamUtil;
import com.capitalone.identity.identitybuilder.policycore.service.versions.PolicyVersionService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.networknt.schema.ValidationMessage;
import com.newrelic.api.agent.Trace;
import lombok.NonNull;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.support.DefaultExchange;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import static com.capitalone.identity.identitybuilder.policycore.camel.util.CamelCacheUtil.CACHE_EXPIRATION;
import static com.capitalone.identity.identitybuilder.policycore.camel.util.CamelCacheUtil.CACHE_VALUE;
import static com.capitalone.identity.identitybuilder.policycore.service.constants.ApplicationConstants.ABAC_CLIENT_UNAUTHORIZED_DEV_TEXT;
import static com.capitalone.identity.identitybuilder.policycore.service.constants.ApplicationConstants.ABAC_CLIENT_UNAUTHORIZED_TEXT;
import static com.capitalone.identity.identitybuilder.policycore.service.constants.ApplicationConstants.CACHE_KEY;
import static com.capitalone.identity.identitybuilder.policycore.service.constants.ApplicationConstants.DEFAULT_CACHE_EXPIRATION;
import static com.capitalone.identity.identitybuilder.policycore.service.constants.ApplicationConstants.ENABLE_CACHE_PROPERTY;
import static com.capitalone.identity.identitybuilder.policycore.service.constants.ApplicationConstants.INVALID_BUSINESS_EVENT_DEV_TEXT;
import static com.capitalone.identity.identitybuilder.policycore.service.constants.ApplicationConstants.INVALID_BUSINESS_EVENT_ID;
import static com.capitalone.identity.identitybuilder.policycore.service.constants.ApplicationConstants.INVALID_BUSINESS_EVENT_TEXT;
import static com.capitalone.identity.identitybuilder.policycore.service.constants.ApplicationConstants.MISSING_BUSINESS_EVENT_DEV_TEXT;
import static com.capitalone.identity.identitybuilder.policycore.service.constants.ApplicationConstants.MISSING_CACHE_PROPERTY;
import static com.capitalone.identity.identitybuilder.policycore.service.constants.ApplicationConstants.MISSING_STEP_PARAMETER;
import static com.capitalone.identity.identitybuilder.policycore.service.constants.ApplicationConstants.MOCK_MODE_CONFIG;
import static com.capitalone.identity.identitybuilder.policycore.service.constants.ApplicationConstants.NO_AVAILABLE_NEXT_STEPS;
import static com.capitalone.identity.identitybuilder.policycore.service.constants.ApplicationConstants.POLICY_NAME;
import static com.capitalone.identity.identitybuilder.policycore.service.constants.ApplicationConstants.POLICY_TO_POLICY_ERROR_CODE;
import static com.capitalone.identity.identitybuilder.policycore.service.constants.ApplicationConstants.POLICY_TO_POLICY_TEXT;
import static com.capitalone.identity.identitybuilder.policycore.service.constants.ApplicationConstants.POLICY_VERSION;
import static com.capitalone.identity.identitybuilder.policycore.service.constants.ApplicationConstants.RESOURCE_ID_NOT_FOUND;
import static com.capitalone.identity.identitybuilder.policycore.service.constants.ApplicationConstants.STEP_NOT_AVAILABLE;
import static java.util.Optional.ofNullable;

@Service
public class PolicyService {
    private static final Logger logger = LoggerFactory.getLogger(PolicyService.class);
    private static final Pattern businessEventNamePattern = Pattern.compile("^[a-zA-Z0-9._-]++$");
    private static final String START = "start";
    private final PolicyServiceExecutor policyServiceExecutor;
    private final String appName;
    private final SchemaManager schemaManager;
    private final PolicyStateCacheService cacheService;
    private final ProcessMetadataHelper processMetadataHelper;
    private final PolicyVersionService policyVersionService;
    final PolicyRequest.SystemParams systemParams;

    private static final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    public PolicyService(PolicyServiceExecutor policyServiceExecutor,
                         @Value("${chassis.app.name}") String appName, //NOSONAR
                         SchemaManager schemaManager,
                         @Autowired(required = false) PolicyStateCacheService cacheService,
                         @Autowired(required = false) AuditLogger auditLogger,
                         ProcessMetadataHelper processMetadataHelper,
                         PolicyVersionService policyVersionService) {
        this.policyServiceExecutor = policyServiceExecutor;
        this.schemaManager = schemaManager;
        this.appName = appName;
        this.cacheService = cacheService;
        this.processMetadataHelper = processMetadataHelper;
        this.policyVersionService = policyVersionService;
        this.systemParams = new PolicyRequest.SystemParams(auditLogger.getFieldsToFilter(), appName, null);
    }

    @EventStream(eventName = "idb-policy-metadata", standardEventStreams = {}, customEventStreams = {
            @EventStream.CustomEventStream(type = EventStream.EventStreamType.AUDIT, emitters = "sdpv3", eventStreamDataBuilder = Cal1_5EventStreamDataBuilder.BUILDER_TYPE)})
    @Trace
    public PolicyMetadata getPolicyMetadata(String policyName, String version, String step) {
        PolicyInfo.Patch versionInfo = policyVersionService.getPolicyVersion(policyName, version);
        PolicyMetadata policyMetadata = new PolicyMetadata();
        policyMetadata.setDomain(appName);
        policyMetadata.setPolicyName(policyName);
        policyMetadata.setPolicyVersion(version);
        policyMetadata.setStep(step);
        String policyRouteName = getPolicyRouteName(versionInfo, step);
        JsonNode node = schemaManager.getEntry(policyRouteName).getSchema().getSchemaNode();
        Map<String, Object> map = objectMapper.convertValue(node, new TypeReference<Map<String, Object>>() {
        });
        policyMetadata.setPolicyJsonSchema(map);

        return policyMetadata;
    }

    public APIResponse createProcess(APIRequest apiRequest, Map<String, Serializable> requestBody) {
        final ExecutedPolicyResponse executionResponse = policyServiceExecutor.executePolicyWithAuditing(
                () -> new PolicyRequest(apiRequest, PolicyRequestType.CREATE, requestBody, systemParams),
                PolicyAuditContext.of(apiRequest, PolicyRequestType.CREATE));
        return createResponse(executionResponse);
    }

    public APIResponse resumeProcess(APIRequest apiRequest, Map<String, Serializable> requestBody) {
        final ExecutedPolicyResponse executionResponse = policyServiceExecutor.executePolicyWithAuditing(
                () -> createPolicyRequestFromCachedWorkflow(apiRequest,
                        requestBody,
                        apiRequest.getProcessId()),
                PolicyAuditContext.of(apiRequest, PolicyRequestType.RESUME));
        return createResponse(executionResponse);
    }

    @EventStream(eventName = "idb-process-metadata", customEventStreams = {
            @EventStream.CustomEventStream(type = EventStream.EventStreamType.AUDIT, emitters = "sdpv3", eventStreamDataBuilder = Cal1_5EventStreamDataBuilder.BUILDER_TYPE)
    })
    public ProcessMetadata getProcessMetadata(String resourceId) {
        return processMetadataHelper.getProcessMetadata(resourceId);
    }

    private APIResponse createResponse(final ExecutedPolicyResponse executionResponse) {
        final Result result = executionResponse.getResult();
        final PolicyResponse policyResponse = result.getPolicyResponse();
        final AbstractChassisException systemError = result.getException();
        if (policyResponse != null) {
            return APIResponse.APISuccessResponse.builder().results(policyResponse.getResults())
                    .policyStatus(policyResponse.getPolicyStatus())
                    .metadata(policyResponse.getMetadata())
                    .errorInfo(policyResponse.getSuppressedErrorInfo() != null
                            ? policyResponse.getSuppressedErrorInfo() : policyResponse.getErrorInfo()).build();
        } else if (systemError != null) {
            throw systemError;
        } else {
            throw new IllegalStateException("Unexpected Result");
        }
    }

    @NotNull
    private PolicyRequest createPolicyRequestFromCachedWorkflow(
            APIRequest apiRequest, Map<String, Serializable> requestBody, String resourceId) {

        String stepName = apiRequest.getStep();
        if (cacheService == null) {
            throw new ChassisSystemException(String.format(MISSING_CACHE_PROPERTY, ENABLE_CACHE_PROPERTY));
        }

        PolicyState policyState = cacheService.retrieveFromCache(resourceId);
        if (policyState == null) {
            throw new NotFoundException(String.format(RESOURCE_ID_NOT_FOUND, resourceId));
        }

        // store values before "next steps" checks so the failure audit log will contain this information
        Set<String> availableNextSteps = policyState.getAvailableNextSteps();
        if (availableNextSteps.isEmpty()) {
            throw new RequestValidationException(String.format(NO_AVAILABLE_NEXT_STEPS, resourceId));
        }

        if (stepName == null) {
            if (availableNextSteps.size()
                    == 1) { // execute only available next step if step query param not provided
                stepName = availableNextSteps.iterator().next();
            } else {
                throw new RequestValidationException(MISSING_STEP_PARAMETER);
            }
        } else if (!availableNextSteps.contains(stepName)) {
            throw new NotFoundException(String.format(STEP_NOT_AVAILABLE, stepName, availableNextSteps));
        }

        // omit stepName if "start"
        if (stepName.equals(START)) {
            stepName = null;
        }

        return new PolicyRequest(apiRequest, PolicyRequestType.RESUME, requestBody,
                policyState.getPolicyName(), policyState.getPolicyVersion(), stepName, systemParams, policyState);
    }

    /**
     * This class is responsible for camel route lookup, invocation, and necessary metadata storage during policy execution.
     */
    @Component
    public static class PolicyExecutionHelper {

        private final boolean isSuppressDeveloperText;
        private final SchemaManager schemaManager;
        private final CamelContext camelContext;
        private final ProcessMetadataHelper processMetadataHelper;
        private final CamelCacheUtil camelCacheUtil;
        private final AuthorizationEnforcer<PolicyAuthSubject, PolicyAuthResource, PolicyAuthAction> authorizationEnforcer;
        private final boolean isAbacFeatureEnabled;
        private final PolicyVersionService policyVersionService;

        private final boolean mockFeatureEnabled;

        public PolicyExecutionHelper( //NOSONAR
                                      @Value("${identitybuilder.policycore.service.errors.suppress-developer-text:false}") boolean isSuppressDeveloperText,
                                      SchemaManager schemaManager,
                                      CamelContext camelContext,
                                      ProcessMetadataHelper processMetadataHelper,
                                      @Autowired(required = false) CamelCacheUtil camelCacheUtil,
                                      AuthorizationEnforcer<PolicyAuthSubject, PolicyAuthResource, PolicyAuthAction> authorizationEnforcer,
                                      @Value("${identitybuilder.policycore.feature.abac-enforcement.enabled}") boolean isABACFeatureEnabled,
                                      PolicyVersionService policyVersionService,
                                      @Value("${identitybuilder.policycore.feature.mock-mode.enabled: false}") boolean mockFeatureEnabled,
                                      @Value("${ENVIRONMENT_NAME:UNK}") String environmentName) {
            this.isSuppressDeveloperText = isSuppressDeveloperText;
            this.schemaManager = schemaManager;
            this.camelContext = camelContext;
            this.processMetadataHelper = processMetadataHelper;
            this.camelCacheUtil = camelCacheUtil;
            this.authorizationEnforcer = authorizationEnforcer;
            this.isAbacFeatureEnabled = isABACFeatureEnabled;
            this.policyVersionService = policyVersionService;
            this.mockFeatureEnabled = !environmentName.toLowerCase().contains("prod") && mockFeatureEnabled;
        }

        @Nullable
        private AbacResponse getAbacResponse(String clientId, String policyName, String policyVersion,
                                             PolicyAuthAction policyAuthAction) {
            if (isAbacFeatureEnabled) {
                return authorizationEnforcer.authorize(
                        new PolicyAuthSubject(clientId),
                        new PolicyAuthResource(policyName, policyVersion),
                        policyAuthAction);
            } else {
                return null;
            }
        }

        private void enforceNonNullAbacResponse(@Nullable AbacResponse abacResponse) {
            if (abacResponse != null && !abacResponse.isAuthorized()) {
                ChassisErrorCode chassisErrorCode = new ChassisErrorCode(abacResponse.getErrorCode(),
                        ABAC_CLIENT_UNAUTHORIZED_TEXT,
                        isSuppressDeveloperText ? null : ABAC_CLIENT_UNAUTHORIZED_DEV_TEXT);
                throw new ChassisSystemException(chassisErrorCode, HttpStatus.UNAUTHORIZED.value());
            }
        }

        /**
         * Looks up the camel route corresponding to the given policy request, invokes it, and returns the response, or
         * error information. On success, updates metadata in cache to support multi-step policies.
         *
         * @param policyRequest the policy request
         * @return the policy response
         */
        @Trace
        public ExecutedPolicyResponse executePolicy(PolicyRequest policyRequest) {
            final ExecutedPolicyResponse.Builder response = new ExecutedPolicyResponse.Builder();

            // policy version forwarding
            final PolicyInfo.Patch policyInfo = policyRequest.getPolicyInfo()
                    .orElse(policyVersionService.getPolicyVersion(
                            policyRequest.getPolicyName(),
                            policyRequest.getPolicyVersionRequested()));
            if (policyInfo == null) {
                NotFoundException error = new NotFoundException(String.format("Unknown policy [name=%s, version=%s]",
                        policyRequest.getPolicyName(), policyRequest.getPolicyVersionRequested()));
                return response.setResult(new Result.SystemError(error)).build();
            }
            final String policyVersion = policyInfo.getPolicyVersion();
            response.setExecutedPolicyVersion(policyVersion);
            response.setExecutedPolicyVersionWithPatch(policyInfo.getPolicyPatchVersionString());
            logger.info("Routed version : {}", policyVersion);

            // access control
            final PolicyRequestType policyRequestType = policyRequest.getPolicyRequestType();

            PolicyAuthAction policyAuthAction;
            if (policyRequestType == PolicyRequestType.CREATE) {
                policyAuthAction = PolicyAuthAction.INITIATE;
            } else if (policyRequestType == PolicyRequestType.RESUME) {
                policyAuthAction = PolicyAuthAction.RESUME;
            } else {
                throw new IllegalArgumentException("policyRequestType=" + policyRequestType);
            }
            AbacResponse abacResponse = getAbacResponse(policyRequest.getClientId(), policyInfo.getPolicyShortName(),
                    policyVersion, policyAuthAction);
            response.setAbacResponse(abacResponse);
            try {
                enforceNonNullAbacResponse(abacResponse);
            } catch (AbstractChassisException e) {
                return response.setResult(new Result.SystemError(e)).build();
            }

            // camel argument prep
            final Exchange exchange = new DefaultExchange(camelContext);
            final Message message = exchange.getIn();

            message.setHeader(CACHE_KEY, policyRequest.getProcessId());
            if (policyRequestType == PolicyRequestType.CREATE) {
                message.setHeader(PolicyConstants.HEADER_STEPNAME, START);
            } else if (policyRequestType == PolicyRequestType.RESUME && policyRequest.getPolicyState() != null) {
                message.setHeader(PolicyConstants.HEADER_STEPNAME, policyRequest.getStep());
                message.setHeader(CACHE_VALUE, policyRequest.getPolicyState().getPolicyState());
                message.setHeader(PolicyConstants.HEADER_POLICYSTATE, policyRequest.getPolicyState());
            }

            String policyRouteName = getPolicyRouteName(policyInfo, policyRequest.getStep());
            String policyFullyQualifiedName = "policy:" + policyRouteName;

            if (camelContext.hasEndpoint(policyFullyQualifiedName) == null) {
                return response.setResult(
                        new Result.SystemError(new NotFoundException("Unknown policy: " + policyRouteName))).build();
            }

            Map<String, Serializable> policyParams = policyRequest.getRequestBody();
            JsonNode policyBody = objectMapper.valueToTree(policyParams);
            Set<ValidationMessage> schemaErrors = schemaManager.validate(policyRouteName, policyBody);

            if (!schemaErrors.isEmpty()) {
                PolicyResponse invalidSchemaResponse = PolicyResponse.builder().policyStatus(PolicyStatus.INVALID)
                        .errorInfo(new ErrorInfo("0", "Schema Validation Failure", schemaErrors.toString()))
                        .build();
                return response.setResult(new Result.PolicySuccess(invalidSchemaResponse)).build();
            }

            String businessEvent = policyRequest.getBusinessEvent();
            if (null != businessEvent && !businessEventNamePattern.matcher(businessEvent).matches()) {
                ChassisErrorCode errorCode;
                if (businessEvent.length() == 0) {
                    errorCode = new ChassisErrorCode(INVALID_BUSINESS_EVENT_ID, INVALID_BUSINESS_EVENT_TEXT,
                            MISSING_BUSINESS_EVENT_DEV_TEXT);
                } else {
                    errorCode = new ChassisErrorCode(INVALID_BUSINESS_EVENT_ID, INVALID_BUSINESS_EVENT_TEXT,
                            INVALID_BUSINESS_EVENT_DEV_TEXT);
                }
                return response.setResult(new Result.SystemError(new ChassisBusinessException(errorCode, 400))).build();
            }

            message.setBody(policyParams);
            message.setHeader(PolicyConstants.HEADER_DXHEADERS, policyRequest.getDxHeaders());

            configureMessageForPolicyEngine(message, policyRequest.getPolicyName(), policyVersion,
                    policyInfo.getPolicyPatchVersionString(), policyRouteName, businessEvent,
                    policyRequest.getMockModeFlag());

            ofNullable(policyRequest.getCustomerIdToken()).ifPresent(
                    token -> message.setHeader(PolicyConstants.CUSTOMER_ID_TOKEN, token));
            setPolicyInfoExchangeProperties(policyRequest.getPolicyName(), policyVersion, exchange);

            // Invoke the camel route and fill in the response.
            ErrorInfo errorInfo = invokeCamel(policyFullyQualifiedName, exchange);
            response.setExecutionData(OperationalAuditExecutionData.fromMessage(message));
            response.setMockModeConfig(exchange.getProperty(MOCK_MODE_CONFIG, null, Boolean.class));
            if (errorInfo == null) {
                if (message.getHeader(PolicyConstants.HEADER_AVAILABLENEXTSTEPS) != null && !message.getHeader(
                        PolicyConstants.HEADER_AVAILABLENEXTSTEPS, Set.class).isEmpty()) {
                    if (policyRequest.isChildPolicy()) {
                        throw new PolicyToPolicyException(String.format(
                                "P2P child policy: \"%s\" is a multistep policy. Child policies must be single step policies",
                                policyInfo.getPolicyFullName() + "/" + policyInfo.getPolicyVersion()));
                    }
                    if (camelCacheUtil == null) {
                        ChassisSystemException error = new ChassisSystemException(
                                String.format(MISSING_CACHE_PROPERTY, ENABLE_CACHE_PROPERTY));
                        return response.setResult(new Result.SystemError(error)).build();
                    }
                    camelCacheUtil.implicitCacheWrite(message);
                    // add metadata field to response
                    PolicyResponse policyResponseWithMetadata = PolicyResponse.builder().results(message.getBody())
                            .policyStatus(PolicyStatus.SUCCESS)
                            .metadata(processMetadataHelper.getProcessMetadata(message.getHeader(CACHE_KEY, String.class))).build();
                    return response.setResult(new Result.PolicySuccess(policyResponseWithMetadata)).build();
                } else if (message.getHeader(PolicyConstants.HEADER_POLICYSTATE) != null) {
                    if (camelCacheUtil == null) {
                        ChassisSystemException error = new ChassisSystemException(
                                String.format(MISSING_CACHE_PROPERTY, ENABLE_CACHE_PROPERTY));
                        return response.setResult(new Result.SystemError(error)).build();
                    }
                    // delete from cache no available next steps (end of multi-step workflow)
                    camelCacheUtil.cacheDelete(message.getHeader(CACHE_KEY, String.class));
                }
                PolicyResponse policySuccessResponse = PolicyResponse.builder().results(message.getBody())
                        .policyStatus(PolicyStatus.SUCCESS).build();
                return response.setResult(new Result.PolicySuccess(policySuccessResponse)).build();
            } else {
                PolicyResponse.PolicyResponseBuilder errorPolicyResponseBuilder = PolicyResponse.builder()
                        .policyStatus(PolicyStatus.FAILURE).errorInfo(errorInfo);
                if (policyRequestType == PolicyRequestType.RESUME) {
                    try {
                        ProcessMetadata processMetadata = processMetadataHelper.getProcessMetadata(
                                message.getHeader(CACHE_KEY, String.class));
                        processMetadata.setEffectiveNextStep(null);
                        errorPolicyResponseBuilder.metadata(processMetadata);
                    } catch (NotFoundException e) {
                        logger.info("Error creating processMetadata in error policy response.", e);
                    }
                }
                if (isSuppressDeveloperText ||
                        message.getHeader(PolicyConstants.HEADER_POLICYAUTHOR_SUPPRESS_DEVTEXT) != null
                                && Boolean.parseBoolean(
                                message.getHeader(PolicyConstants.HEADER_POLICYAUTHOR_SUPPRESS_DEVTEXT).toString())) {
                    ErrorInfo redactedErrorInfo = errorInfo.toBuilder().developerText(null).build();
                    return response.setResult(new Result.PolicySuccess(
                            errorPolicyResponseBuilder.suppressedErrorInfo(redactedErrorInfo).build())).build();
                } else {
                    return response.setResult(new Result.PolicySuccess(errorPolicyResponseBuilder.build())).build();
                }
            }
        }

        /**
         * Sets current policyName and version to be used in 5000654 events to correlate with 5000653 events
         *
         * @param policyName name of the invoked policy
         * @param version    version of the invoked policy
         * @param exchange   current Camel Exchange
         */
        private void setPolicyInfoExchangeProperties(String policyName, String version, Exchange exchange) {
            exchange.setProperty(POLICY_NAME, policyName);
            exchange.setProperty(POLICY_VERSION, version);
        }

        /**
         * Update the exchange with the standard headers we supply to all policy routes.
         *
         * @param message            The message to modify with headers for policy engine
         * @param policyName         The policy that is being invoked
         * @param policyVersion      The version of the policy being invoked
         * @param policyVersionPatch The patcher version of the policy being invoked
         * @param policyRouteName    the policy route name
         * @param businessEvent      The business event for the policy that is being invoked
         * @param mockModeFlag       The flag to disable or enable mock mode
         */
        private void configureMessageForPolicyEngine(Message message, String policyName, String policyVersion,
                                                     String policyVersionPatch, String policyRouteName, String businessEvent,
                                                     Boolean mockModeFlag) {
            // This is a hack of sorts. Ideally, policy core wouldn't know or care about
            message.setHeader(PolicyConstants.HEADER_BUSINESS_EVENT, businessEvent);
            message.setHeader(PolicyConstants.HEADER_POLICYNAME, policyName);
            message.setHeader(PolicyConstants.HEADER_POLICYVERSION, policyVersion);
            message.setHeader(PolicyConstants.HEADER_POLICYVERSION_PATCH, policyVersionPatch);
            message.setHeader(PolicyConstants.HEADER_POLICYROUTENAME, policyRouteName);
            message.setHeader(PolicyConstants.HEADER_AUDIT_CONTEXT, RequestContextHolder.getRequestContextOrDefault());
            message.setHeader(PolicyConstants.POLICY_RULE_RESULTS, new ArrayList<Map<String, Object>>());
            message.setHeader(PolicyConstants.OPERATIONAL_AUDIT_EXECUTION_DATA, new OperationalAuditExecutionData());

            if(mockFeatureEnabled) {
                message.setHeader(PolicyConstants.HEADER_MOCKMODE, mockModeFlag);
            }

            // sensible cache defaults; can be overridden by policy
            message.setHeader(CACHE_EXPIRATION, DEFAULT_CACHE_EXPIRATION);
        }

        /**
         * Invoke a Camel route endpoint with exception handling.
         *
         * @param route    the endpoint of the route to invoke
         * @param exchange the active exchange
         * @return the error info corresponding to any policy exception,
         * or <code>null</code> if no exception was reported
         */
        @Trace
        private ErrorInfo invokeCamel(String route, Exchange exchange) {
            try (ProducerTemplate template = camelContext.createProducerTemplate()) {
                template.send(route, exchange);
            } catch (IOException e) {
                throw new IllegalStateException("Unable to create ProducerTemplate", e);
            }

            // If the exchange reports an exception, return an appropriate API error.
            ErrorInfo errorInfo = null;
            if (exchange.getException() != null) {
                Exception e = exchange.getException();
                if (e instanceof DownstreamException) {
                    errorInfo = new ErrorInfo("202020", "Downstream API error", e.toString());
                } else if (e instanceof UnsupportedBusinessEventException) {
                    errorInfo = new ErrorInfo(INVALID_BUSINESS_EVENT_ID, "Invalid Business Event", e.getMessage());
                } else if (e instanceof CustomPolicyException) {
                    errorInfo = ((CustomPolicyException)e).getErrorInfo();
                } else if (e instanceof PolicyToPolicyException || e instanceof PolicyFailureException) {
                    errorInfo = new ErrorInfo(POLICY_TO_POLICY_ERROR_CODE, POLICY_TO_POLICY_TEXT, e.getMessage());
                } else if (e instanceof PolicyCacheException) {
                    errorInfo = new ErrorInfo(((PolicyCacheException) e).getId(), ((PolicyCacheException) e).getText(), ((PolicyCacheException) e).getDeveloperText());
                } else {
                    errorInfo = new ErrorInfo(ChassisErrorCode.SYSTEM_ERROR, "Error occurred during policy execution", e.getClass().getName() + ": " + e.getMessage());
                }

                String msg = "Route " + route + ": " + errorInfo.getDeveloperText();
                if (logger.isDebugEnabled()) {
                    logger.debug(msg, e);
                } else {
                    logger.info(msg);
                }
            }

            return errorInfo;
        }
    }

    /**
     * This class is responsible for obtaining current metadata corresponding to a multi-step policy execution
     */
    @Component
    public static class ProcessMetadataHelper {

        private final PolicyStateCacheService cacheService;

        public ProcessMetadataHelper(@Autowired(required = false) PolicyStateCacheService cacheService) {
            this.cacheService = cacheService;
        }

        /**
         * Obtains metadata corresponding to a policy resource id. This metadata is necessary to support multi-step
         * policies.
         *
         * @param resourceId the UUID used for policy metadata lookup
         * @return the policy metadata corresponding to the id if present, raises exception if metadata not found.
         */
        @Trace
        public ProcessMetadata getProcessMetadata(String resourceId) {
            if (cacheService == null) {
                throw new ChassisSystemException(String.format(MISSING_CACHE_PROPERTY, ENABLE_CACHE_PROPERTY));
            }
            resourceId = PathParamUtil.cleanParameter(resourceId);
            // Retrieve the policy state and step expiration from the cache
            PolicyState policyState = cacheService.retrieveFromCache(resourceId);
            LocalDateTime stepExpiration = cacheService.getStepExpiration(resourceId);

            if (policyState == null || stepExpiration == null) {
                // if resourceId was not found in the cache; it is considered missing/expired.
                throw new NotFoundException(String.format(RESOURCE_ID_NOT_FOUND, resourceId));
            }

            ProcessMetadata processMetadata = new ProcessMetadata();
            processMetadata.setResourceId(resourceId);
            processMetadata.setStepExpiration(cacheService.getStepExpiration(resourceId));
            processMetadata.setAvailableNextSteps(policyState.getAvailableNextSteps());
            processMetadata.setStepsCompleted(policyState.getStepsCompleted());
            processMetadata.setPolicyName(policyState.getPolicyName());
            processMetadata.setPolicyVersion(policyState.getPolicyVersion());
            processMetadata.setEffectiveNextStep(policyState.getEffectiveNextStep());

            return processMetadata;
        }

    }

    @Trace
    private static String getPolicyRouteName(PolicyInfo.Patch version, String stepName) {

        StringBuilder policyRouteName = new StringBuilder(version.getPolicyShortName()).append("_").append(version.getPolicyPatchVersionString());

        if (stepName != null) {
            policyRouteName.append("-").append(stepName);
        }
        return policyRouteName.toString();
    }

    /**
     * The purpose of this interface is to specify that 'executePolicy' result function
     * can return an {@link AbstractChassisException} or a 'successful' {@link PolicyResponse},
     * but not both.
     */
    public interface Result {

        default AbstractChassisException getException() {
            return null;
        }

        default PolicyResponse getPolicyResponse() {
            return null;
        }

        @lombok.Value
        class SystemError implements Result {
            @NonNull
            AbstractChassisException exception;
        }

        @lombok.Value
        class PolicySuccess implements Result {
            @NonNull
            PolicyResponse policyResponse;
        }

    }

    @lombok.Value
    @lombok.Builder(builderClassName = "Builder", setterPrefix = "set")
    public static class ExecutedPolicyResponse {
        String executedPolicyVersion;
        String executedPolicyVersionWithPatch;
        AbacResponse abacResponse;
        Boolean mockModeConfig;

        OperationalAuditExecutionData executionData;

        @NonNull
        Result result;
    }

}
