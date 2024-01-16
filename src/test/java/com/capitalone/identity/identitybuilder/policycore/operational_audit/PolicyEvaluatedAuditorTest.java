package com.capitalone.identity.identitybuilder.policycore.operational_audit;

import com.capitalone.chassis.engine.model.error.ErrorResponse;
import com.capitalone.chassis.engine.model.exception.*;
import com.capitalone.identity.identitybuilder.audit.models.AuditEvent;
import com.capitalone.identity.identitybuilder.audit.sdpv4.AuditPublisher;
import com.capitalone.identity.identitybuilder.audit.sdpv4.ProducerProperties;
import com.capitalone.identity.identitybuilder.policycore.abac.model.AbacResponse;
import com.capitalone.identity.identitybuilder.policycore.model.*;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.cyber.AWSHostContextCollector;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.mapper.ChassisStatusCodeResolver;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.mapper.OperationalEventEnricher;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.mapper.PolicyEvaluatedEventMapper;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.model.*;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.model.policy.*;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.util.MockHostContext;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.util.OperationalAuditPublisher;
import com.capitalone.identity.identitybuilder.policycore.service.PolicyService;
import com.capitalone.identity.identitybuilder.policycore.service.constants.ApplicationConstants;
import com.capitalone.identity.identitybuilder.policycore.service.logging.PolicyAuditContext;
import com.capitalone.identity.identitybuilder.policycore.service.util.JsonUtil;
import com.google.common.collect.Lists;
import org.apache.commons.compress.utils.Sets;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static com.capitalone.identity.identitybuilder.policycore.operational_audit.util.OperationalAuditUtil.DEFAULT_POLICY_ERROR_TEXT;
import static com.capitalone.identity.identitybuilder.policycore.service.constants.ApplicationConstants.*;

public class PolicyEvaluatedAuditorTest {

    private static final List<String> DEFAULT_PIPS_EVALUATED = Lists.newArrayList("Pip1", "Pip2", "Pip3");
    private static final List<String> DEFAULT_DMNS_EVALUATED = Lists.newArrayList("Dmn1", "Dmn2", "Dmn3");
    private static final Integer DEFAULT_CHASSIS_HTTP_STATUS = 200;

    // Error stacktrace suppression flag
    private boolean stackTraceSuppressed;

    // response data variables
    private PolicyService.Result policyResultExpected;
    private PolicyService.ExecutedPolicyResponse executePolicyResponseExpected;

    private OperationalEventOutcome policyOutcomeExpected;
    private PolicyStatus policyStatusExpected;
    private Long startTimestampExpected;
    private String processIdExpected;
    private String policyNameExpected;
    private String policyVersionRequestExpected;
    private String policyVersionExecutedExpected;
    private String stepExpected;
    private String eventEntityNameExpected;
    private PolicyRequestType policyRequestTypeExpected;
    private ErrorInfo errorInfoExpected;
    private OperationalError operationalErrorExpected;
    private OperationalAuditExecutionData executionDataExpected;
    private AbacResponse abacResponseExpected;
    private String parentPolicyNameExpected;
    private Integer chassisHttpStatusExpected;
    private Boolean mockModeConfigExpected;

    // Multistep metadata
    private Set<String> availableNextStepsExpected;
    private String effectiveNextStepExpected;
    private List<String> stepsCompletedExpected;
    private LocalDateTime stepExpirationExpected;

    private PolicyRequest policyRequestExpected;
    private PolicyAuditContext policyAuditContextExpected;

    // Dx headers / execution context
    private String apiKeyExpected;
    private String contentTypeExpected;
    private String channelTypeExpected;
    private String countryCodeExpected;
    private String acceptLanguageExpected;
    private String domainExpected;
    private String subDomainExpected;
    private String acceptExpected;
    private String userAgentExpected;
    private String clientIdExpected;
    private String businessEventExpected;
    private String xJourneyPolicyExpected;
    private Map<String, String> dxHeadersExpected;

    private Map<String, Serializable> requestString = new HashMap<>();

    private AuditPublisher auditPublisher;
    private OperationalSystemContext operationalSystemContextExpected;
    private ChassisStatusCodeResolver chassisStatusCodeResolver;
    private PolicyEvaluatedEventMapper.Factory policyEvaluatedEventMapperFactory;
    private ArgumentCaptor<AuditEvent> auditEventArgumentCaptor;
    private PolicyEvaluatedAuditor.InternalAuditor internalPolicyAuditor;
    private PolicyEvaluatedAuditor policyAuditor;

    private final AWSHostContextCollector mockAWSHostContextCollector = new AWSHostContextCollector(new MockHostContext());

    @BeforeEach
    public void setup() throws Exception {
        /* Policy Evaluated Operational Auditor Setup */
        stackTraceSuppressed = false;
        instantiatePolicyAuditorEnabled(true);

        /* Default start timestamp */
        startTimestampExpected = 1647617485225L;

        /* Default metadata */
        policyOutcomeExpected = new OperationalEventOutcome(PolicyStatus.SUCCESS.name(), String.valueOf(DEFAULT_CHASSIS_HTTP_STATUS));
        policyStatusExpected = PolicyStatus.SUCCESS;
        processIdExpected = UUID.randomUUID().toString();
        policyNameExpected = "success";
        policyVersionRequestExpected = "1";
        policyVersionExecutedExpected = "1.3";
        clientIdExpected = "testClientId";
        policyRequestTypeExpected = PolicyRequestType.CREATE;
        stepExpected = "step1";
        eventEntityNameExpected = "success_1.3";
        parentPolicyNameExpected = null;
        chassisHttpStatusExpected = DEFAULT_CHASSIS_HTTP_STATUS;

        /* Default multi-step metadata */
        effectiveNextStepExpected = "step2";
        availableNextStepsExpected = Sets.newHashSet("step2");
        stepsCompletedExpected = Lists.newArrayList("start");
        stepExpirationExpected = LocalDateTime.now().plusYears(1);

        /* Default error info */
        errorInfoExpected = null;
        operationalErrorExpected = null;

        /* Default Policy Header Setup */
        executionDataExpected = new OperationalAuditExecutionData();
        executionDataExpected.getPipsEvaluated().addAll(DEFAULT_PIPS_EVALUATED);
        executionDataExpected.getDmnsEvaluated().addAll(DEFAULT_DMNS_EVALUATED);

        /*
         * Default abac response
         */
        abacResponseExpected = new AbacResponse(true, false, null, null);

        /* Default Mock Mode Configuration value */
        mockModeConfigExpected = null;

        /*
         * Dx Headers and execution context
         */
        xJourneyPolicyExpected = "xJourneyPolicy";
        apiKeyExpected = "apiKey";
        contentTypeExpected = "contentType";
        channelTypeExpected = "channelType";
        countryCodeExpected = "countryCode";
        acceptLanguageExpected = "acceptLangage";
        domainExpected = "domain";
        subDomainExpected = "subDomain";
        userAgentExpected = "userAgent";
        acceptExpected = "accept";
        clientIdExpected = "clientId";
        businessEventExpected = "businessEvent";

        requestString.put("pip", true);
        requestString.put("decision", true);

        createDefaultDxHeaders();


        /*
         * Default policy request
         */
        createDefaultPolicyRequest();

        /*
         * Default audit context
         */
        createDefaultAuditContext();

        /*
         * Default policy response
         */
        createDefaultPolicySuccessResult(false);
        createDefaultPolicyResponse();
    }

    private void instantiatePolicyAuditorEnabled(final boolean enabled) {
        auditPublisher = Mockito.mock(AuditPublisher.class);
        ProducerProperties producerProperties = Mockito.mock(ProducerProperties.class);
        Mockito.lenient().when(producerProperties.getSchemaName()).thenReturn("testSchema");
        operationalSystemContextExpected = Mockito.mock(OperationalSystemContext.class);
        chassisStatusCodeResolver = Mockito.mock(ChassisStatusCodeResolver.class);
        policyEvaluatedEventMapperFactory = new PolicyEvaluatedEventMapper.Factory(chassisStatusCodeResolver, null);
        Mockito.when(chassisStatusCodeResolver.resolveHttpStatusCode(Mockito.any(), Mockito.any()))
                .thenReturn(DEFAULT_CHASSIS_HTTP_STATUS);

        auditEventArgumentCaptor = ArgumentCaptor.forClass(AuditEvent.class);
        // Object(s) under test
        internalPolicyAuditor = new PolicyEvaluatedAuditor.InternalAuditor(
                new OperationalEventEnricher(operationalSystemContextExpected, stackTraceSuppressed),
                new OperationalAuditPublisher(auditPublisher, producerProperties.getSchemaName()),
                mockAWSHostContextCollector, policyEvaluatedEventMapperFactory);

        policyAuditor = new PolicyEvaluatedAuditor(enabled ? internalPolicyAuditor : null);
    }

    @Test
    public void auditCreateProcessSuccess() {
        // Act
        policyAuditor.audit(policyRequestExpected, executePolicyResponseExpected, null,
                startTimestampExpected, policyAuditContextExpected);

        // Assert
        expectedPolicyEvaluatedEventOccurred(false);
    }

    @Test
    public void auditCreateProcessDisabledSuccess() {
        // Arrange
        instantiatePolicyAuditorEnabled(false);

        // Act
        policyAuditor.audit(policyRequestExpected, executePolicyResponseExpected, null, startTimestampExpected, policyAuditContextExpected);

        // Assert
        Mockito.verify(auditPublisher, Mockito.times(0)).publish(auditEventArgumentCaptor.capture());
    }

    @Test
    public void auditChassisExceptionSuccess() {
        // Arrange
        policyNameExpected = "missing";
        policyVersionRequestExpected = "2.1";
        policyVersionExecutedExpected = null;
        eventEntityNameExpected = "missing_2.1";
        stepExpected = "start";
        policyStatusExpected = PolicyStatus.FAILURE;
        policyOutcomeExpected = new OperationalEventOutcome("FAILURE", String.valueOf(chassisHttpStatusExpected));
        createDefaultPolicyRequest();
        createDefaultAuditContext();
        final ErrorResponse chassisErrorResponse = ErrorResponse.getEmptyResponse();
        final AbstractChassisException chassisException = new NotFoundException("Unknown policy: missing_2.1");
        operationalErrorExpected = expectedOperationalError(Optional.of(chassisErrorResponse), chassisException);
        policyResultExpected = new PolicyService.Result.SystemError(chassisException);
        createDefaultPolicyResponse();

        // Act
        policyAuditor.auditChassisError(policyRequestExpected, chassisException, chassisErrorResponse,
                executePolicyResponseExpected, startTimestampExpected, policyAuditContextExpected);

        // Assert
        expectedPolicyEvaluatedEventOccurred(false);
    }

    @Test
    void auditChassisExceptionWithErrorDetailsSuccess() {
        policyNameExpected = "missing";
        policyVersionRequestExpected = "2.1";
        policyVersionExecutedExpected = null;
        eventEntityNameExpected = "missing_2.1";
        stepExpected = "start";
        policyStatusExpected = PolicyStatus.FAILURE;
        policyOutcomeExpected = new OperationalEventOutcome("FAILURE", String.valueOf(chassisHttpStatusExpected));
        createDefaultPolicyRequest();
        createDefaultAuditContext();

        final ErrorResponse chassisErrorResponse = ErrorResponse.getEmptyResponse();
        ChassisErrorCode errorCode = new ChassisErrorCode("200001", "200099", "200001");
        errorCode.addErrorDetail(CACHE_READ_MISS_ID, CACHE_READ_MISS_ID);
        errorCode.getErrorDetails().get(0).setDeveloperTextId(ApplicationConstants.DYNAMIC_TEXT_MSG);
        errorCode.getErrorDetails().get(0).getDeveloperTextMessageParms().add("missing developer text");
        AbstractChassisException chassisException = new ChassisSystemException(errorCode);

        operationalErrorExpected = expectedOperationalError(Optional.of(chassisErrorResponse), chassisException);
        policyResultExpected = new PolicyService.Result.SystemError(chassisException);
        createDefaultPolicyResponse();

        // Act
        policyAuditor.auditChassisError(policyRequestExpected, chassisException, chassisErrorResponse,
                executePolicyResponseExpected, startTimestampExpected, policyAuditContextExpected);

        // Assert
        expectedPolicyEvaluatedEventOccurred(false);
    }

    @Test
    public void auditChassisErrorDisabledSuccess() {
        // Arrange
        instantiatePolicyAuditorEnabled(false);
        final ErrorResponse chassisErrorResponse = ErrorResponse.getEmptyResponse();
        final AbstractChassisException chassisException = new NotFoundException("Unknown policy: missing_1.0");

        // Act
        policyAuditor.auditChassisError(policyRequestExpected, chassisException, chassisErrorResponse,
                executePolicyResponseExpected, startTimestampExpected, policyAuditContextExpected);

        // Assert
        Mockito.verify(auditPublisher, Mockito.times(0)).publish(auditEventArgumentCaptor.capture());
    }

    @Test
    public void auditResumeSuccess() {
        // Arrange
        instantiatePolicyAuditorEnabled(true);
        policyRequestTypeExpected = PolicyRequestType.RESUME;
        createDefaultPolicyRequest();
        createDefaultAuditContext();
        createDefaultPolicySuccessResult(true);
        createDefaultPolicyResponse();

        // Act
        policyAuditor.audit(policyRequestExpected, executePolicyResponseExpected, null, startTimestampExpected, policyAuditContextExpected);

        // Assert
        expectedPolicyEvaluatedEventOccurred(true);
    }

    @Test
    public void auditNonErrorInfoResponseFailure() {
        // Arrange
        final Exception exception = new NullPointerException("Oops.");
        chassisHttpStatusExpected = 500;
        policyStatusExpected = PolicyStatus.FAILURE;
        policyOutcomeExpected = new OperationalEventOutcome("FAILURE", String.valueOf(chassisHttpStatusExpected));
        errorInfoExpected = new ErrorInfo("dummyId", "dummyText", "dummyDeveloperText");
        createDefaultPolicySuccessResult(false);
        createDefaultPolicyResponse();
        operationalErrorExpected = expectedOperationalError(errorInfoExpected);
        Mockito.when(chassisStatusCodeResolver.resolveHttpStatusCode(Mockito.any(), Mockito.any()))
                .thenReturn(chassisHttpStatusExpected);

        // Act
        policyAuditor.audit(policyRequestExpected, executePolicyResponseExpected, exception,
                startTimestampExpected, policyAuditContextExpected);

        // Assert
        expectedPolicyEvaluatedEventOccurred(false);
    }

    @Test
    public void auditFailureStackTraceSuppressed() {
        // Arrange
        stackTraceSuppressed = true;
        instantiatePolicyAuditorEnabled(true);
        final Exception exception = new NullPointerException("Oops.");
        policyStatusExpected = PolicyStatus.FAILURE;
        chassisHttpStatusExpected = 500;
        policyOutcomeExpected = new OperationalEventOutcome("FAILURE", String.valueOf(chassisHttpStatusExpected));
        errorInfoExpected = new ErrorInfo("dummyId", "dummyText", "dummyDeveloperText");
        createDefaultPolicySuccessResult(false);
        createDefaultPolicyResponse();
        operationalErrorExpected = expectedOperationalError(errorInfoExpected);
        Mockito.when(chassisStatusCodeResolver.resolveHttpStatusCode(Mockito.any(), Mockito.any()))
                .thenReturn(chassisHttpStatusExpected);

        // Act
        policyAuditor.audit(policyRequestExpected, executePolicyResponseExpected, exception,
                startTimestampExpected, policyAuditContextExpected);

        // Assert
        expectedPolicyEvaluatedEventOccurred(false);

    }

    @Test
    public void auditCreateProcessAbacFailSoftEnforcement() {
        // Arrange
        abacResponseExpected = new AbacResponse(false, false, "Abac Text", "Abac Error Code");
        policyStatusExpected = PolicyStatus.FAILURE;
        policyOutcomeExpected = new OperationalEventOutcome("SUCCESS", "200"); // strict enforcement disabled, thus success
        final ErrorResponse chassisErrorResponse = ErrorResponse.getEmptyResponse();
        final AbstractChassisException chassisException = new NotFoundException("Access not found");
        createDefaultPolicyResponse();

        operationalErrorExpected = expectedOperationalError(Optional.of(chassisErrorResponse), chassisException);

        // Act
        policyAuditor.auditChassisError(policyRequestExpected, chassisException, chassisErrorResponse,
                executePolicyResponseExpected, startTimestampExpected, policyAuditContextExpected);

        // Assert
        expectedPolicyEvaluatedEventOccurred(false);
    }

    @Test
    public void auditNon2xxChassisHttpStatus() {
        // Arrange
        chassisHttpStatusExpected = 404;
        Mockito.when(chassisStatusCodeResolver.resolveHttpStatusCode(Mockito.any(), Mockito.any()))
                .thenReturn(chassisHttpStatusExpected);
        policyStatusExpected = PolicyStatus.FAILURE;
        policyOutcomeExpected = new OperationalEventOutcome("FAILURE", String.valueOf(chassisHttpStatusExpected));
        final ErrorResponse chassisErrorResponse = ErrorResponse.getEmptyResponse();
        final AbstractChassisException chassisException = new NotFoundException("Bad");
        policyResultExpected = new PolicyService.Result.SystemError(chassisException);
        createDefaultPolicyResponse();

        operationalErrorExpected = expectedOperationalError(Optional.of(chassisErrorResponse), chassisException);

        // Act
        policyAuditor.auditChassisError(policyRequestExpected, chassisException, chassisErrorResponse,
                executePolicyResponseExpected, startTimestampExpected, policyAuditContextExpected);

        // Assert
        expectedPolicyEvaluatedEventOccurred(false);
    }

    @Test
    void auditBlankChassisSystemExceptionNoErrorResponse() {
        chassisHttpStatusExpected = 505;
        Mockito.when(chassisStatusCodeResolver.resolveHttpStatusCode(Mockito.any(), Mockito.any()))
                .thenReturn(chassisHttpStatusExpected);
        policyStatusExpected = PolicyStatus.FAILURE;
        policyOutcomeExpected = new OperationalEventOutcome("FAILURE", String.valueOf(chassisHttpStatusExpected));

        final ErrorResponse chassisErrorResponse = null;
        final AbstractChassisException chassisException = new ChassisSystemException();
        policyResultExpected = new PolicyService.Result.SystemError(chassisException);
        createDefaultPolicyResponse();

        operationalErrorExpected = expectedOperationalError(Optional.empty(), chassisException);

        // Act
        policyAuditor.auditChassisError(policyRequestExpected, chassisException, chassisErrorResponse,
                executePolicyResponseExpected, startTimestampExpected, policyAuditContextExpected);

        expectedPolicyEvaluatedEventOccurred(false);
    }

    /**
     * Runs and asserts NPE on non-null enforced permutations where each required value is null
     */

    @Test
    public void auditP2PSuccess() {
        parentPolicyNameExpected = "parentPolicy/1.0";
        createDefaultAuditContext();

        // Act
        policyAuditor.audit(policyRequestExpected, executePolicyResponseExpected, null,
                startTimestampExpected, policyAuditContextExpected);

        // Assert
        expectedPolicyEvaluatedEventOccurred(false);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void auditMockModeConfigured(boolean mockModeConfig){
        // Arrange
        mockModeConfigExpected = mockModeConfig;
        createDefaultPolicyResponse();

        // Act
        policyAuditor.audit(policyRequestExpected, executePolicyResponseExpected, null,
                startTimestampExpected, policyAuditContextExpected);

        // Assert
        expectedPolicyEvaluatedEventOccurred(false);
    }

    private void expectedPolicyEvaluatedEventOccurred(final boolean multistepMetadataExpected) {
        Mockito.verify(auditPublisher).publish(auditEventArgumentCaptor.capture());
        final OperationalEvent operationalAuditActual = (OperationalEvent) auditEventArgumentCaptor.getValue().getMessage();
        final OperationalPolicyMetadata policyMetadataActual = operationalAuditActual.getPolicyMetadata();
        Assertions.assertEquals("POLICY_EVALUATED", operationalAuditActual.getEventName());
        Assertions.assertEquals("POLICY", operationalAuditActual.getEventEntity());
        Assertions.assertEquals("EVALUATED", operationalAuditActual.getEventAction());
        Assertions.assertNotNull(operationalAuditActual.getEventOperationId());
        Assertions.assertEquals(eventEntityNameExpected, operationalAuditActual.getEventEntityName());
        Assertions.assertEquals(policyOutcomeExpected, operationalAuditActual.getEventOutcome());
        Assertions.assertEquals(startTimestampExpected, operationalAuditActual.getEventStartTimestamp());
        Assertions.assertTrue(operationalAuditActual.getEventEndTimestamp() >= startTimestampExpected);
        Assertions.assertTrue(Long.parseLong(operationalAuditActual.getEventDurationMs()) >= 0);
        Assertions.assertEquals(operationalSystemContextExpected, operationalAuditActual.getSystemContext());

        // Error metadata
        if (!policyOutcomeExpected.getResult().equals("SUCCESS")) {
            Assertions.assertNotNull(operationalAuditActual.getErrorMetadata());
            final OperationalError operationalErrorActual = operationalAuditActual.getErrorMetadata();
            Assertions.assertEquals(operationalErrorExpected.getText(), operationalErrorActual.getText());
            Assertions.assertEquals(operationalErrorExpected.getDeveloperText(), operationalErrorActual.getDeveloperText());
            Assertions.assertEquals(operationalErrorExpected.getErrorId(), operationalErrorActual.getErrorId());
            if (!stackTraceSuppressed) {
                Assertions.assertNotNull(operationalErrorActual.getStackTrace());
            } else {
                Assertions.assertNull(operationalErrorActual.getStackTrace());
            }
        }

        Assertions.assertNotNull(operationalAuditActual.getRequestMessageId());
        Assertions.assertNotNull(operationalAuditActual.getRequestCorrelationId());
        Assertions.assertNotNull(operationalAuditActual.getPolicyMetadata());

        // Execution Context
        Assertions.assertNotNull(operationalAuditActual.getExecutionContext());
        final OperationalExecutionContext executionContextActual = operationalAuditActual.getExecutionContext();
        Assertions.assertEquals(businessEventExpected, executionContextActual.getBusinessEvent());
        Assertions.assertEquals(xJourneyPolicyExpected, executionContextActual.getXJourneyPolicy());
        Assertions.assertEquals(domainExpected, executionContextActual.getDomain());
        Assertions.assertEquals(apiKeyExpected, executionContextActual.getApiKey());
        Assertions.assertEquals(contentTypeExpected, executionContextActual.getContentType());
        Assertions.assertEquals(channelTypeExpected, executionContextActual.getChannelType());
        Assertions.assertEquals(countryCodeExpected, executionContextActual.getCountryCode());
        Assertions.assertEquals(acceptLanguageExpected, executionContextActual.getAcceptLanguage());

        // Multi-step metadata
        Assertions.assertEquals(processIdExpected, policyMetadataActual.getProcessId());
        if (multistepMetadataExpected) {
            Assertions.assertEquals(Lists.newArrayList(availableNextStepsExpected),
                    policyMetadataActual.getAvailableNextSteps());
            Assertions.assertEquals(effectiveNextStepExpected, policyMetadataActual.getEffectiveNextStep());
            Assertions.assertEquals(stepsCompletedExpected, policyMetadataActual.getStepsCompleted());
            Assertions.assertNotNull(policyMetadataActual.getStepExpiration());
        } else {
            Assertions.assertNull(policyMetadataActual.getAvailableNextSteps());
            Assertions.assertNull(policyMetadataActual.getEffectiveNextStep());
            Assertions.assertNull(policyMetadataActual.getStepsCompleted());
            Assertions.assertNull(policyMetadataActual.getStepExpiration());
        }

        // General metadata
        Assertions.assertEquals(policyNameExpected, policyMetadataActual.getPolicyName());
        Assertions.assertEquals(policyVersionRequestExpected, policyMetadataActual.getPolicyVersionRequested());
        Assertions.assertEquals(policyVersionExecutedExpected, policyMetadataActual.getVersionExecuted());
        Assertions.assertEquals(stepExpected, policyMetadataActual.getStep());

        // Operational Policy Header
        Assertions.assertEquals(executionDataExpected.getPipsEvaluated(),
                operationalAuditActual.getPolicyMetadata().getPipsEvaluated());
        Assertions.assertEquals(executionDataExpected.getDmnsEvaluated(),
                operationalAuditActual.getPolicyMetadata().getDmnsEvaluated());

        // ABAC
        final Map<String, String> actualSupplementalMetadata = operationalAuditActual.getSupplementalMetadata()
                .stream().collect(Collectors.toMap(OperationalSupplementalAttribute::getKey, OperationalSupplementalAttribute::getValue));
        Assertions.assertEquals(String.valueOf(abacResponseExpected.isAuthorized()),
                actualSupplementalMetadata.get("abac_is_authorized"));
        Assertions.assertEquals(String.valueOf(abacResponseExpected.isStrictEnforcementFlag()),
                actualSupplementalMetadata.get("abac_is_strict_enforcement"));
        Assertions.assertEquals(abacResponseExpected.getErrorCode(),
                actualSupplementalMetadata.get("abac_error_code"));
        Assertions.assertEquals(abacResponseExpected.getText(),
                actualSupplementalMetadata.get("abac_text"));

        // Mock Mode
        if(mockModeConfigExpected != null) {
            Assertions.assertEquals(String.valueOf(mockModeConfigExpected), actualSupplementalMetadata.get("mock_mode_configuration_value"));
        }
        else {
            Assertions.assertNull(actualSupplementalMetadata.get("mock_mode_configuration_value"));
        }

        // Request and Response
        Assertions.assertEquals(JsonUtil.getFilteredResponse(requestString, ""),
                operationalAuditActual.getProtocolRequestDetail());

        switch (operationalAuditActual.getEventOutcome().getResult()) {
            case "SUCCESS": {
                if (policyResultExpected instanceof PolicyService.Result.PolicySuccess)
                    Assertions.assertEquals(JsonUtil.getFilteredResponse(policyResultExpected.getPolicyResponse().getResults(), ""),
                            operationalAuditActual.getProtocolResponseDetail());
                break;
            }
            case "FAILURE": {
                if (policyResultExpected instanceof PolicyService.Result.SystemError)
                    Assertions.assertEquals(JsonUtil.getFilteredResponse(policyResultExpected.getException().getApiError(), ""),
                            operationalAuditActual.getProtocolResponseDetail());
                break;
            }
            case "INVALID": {
                Assertions.assertNull(operationalAuditActual.getProtocolResponseDetail());
                break;
            }
        }

        // Client Id
        Assertions.assertEquals(clientIdExpected, policyMetadataActual.getClientId());

        // HTTP Status
        Assertions.assertEquals(chassisHttpStatusExpected, Integer.valueOf(actualSupplementalMetadata.get("http_status")));

        // P2P
        if (parentPolicyNameExpected != null) {
            Assertions.assertEquals(parentPolicyNameExpected, policyMetadataActual.getP2PParentPolicyName());
            Assertions.assertEquals(policyNameExpected, policyMetadataActual.getP2PChildrenPolicyNames());
        } else {
            Assertions.assertNull(policyMetadataActual.getP2PParentPolicyName());
            Assertions.assertNull(policyMetadataActual.getP2PChildrenPolicyNames());
        }
    }

    private void createDefaultPolicyRequest() {
        policyRequestExpected = PolicyRequest.builder()
                .policyName(policyNameExpected)
                .policyVersionRequested(policyVersionRequestExpected)
                .policyInfo(Optional.empty())
                .step(stepExpected)
                .businessEvent(businessEventExpected)
                .xJourneyPolicy(xJourneyPolicyExpected)
                .clientId(clientIdExpected)
                .policyParametersAudit(JsonUtil.getFilteredResponse(requestString, ""))
                .processId(processIdExpected)
                .policyRequestType(policyRequestTypeExpected)
                .dxHeaders(dxHeadersExpected)
                .childPolicy(false).
                requestBody(requestString)
                .build();
    }

    private void createDefaultAuditContext() {
        policyAuditContextExpected = PolicyAuditContext.builder()
                .policyName(policyNameExpected)
                .policyVersionRequested(policyVersionRequestExpected)
                .step(stepExpected)
                .businessEvent(businessEventExpected)
                .xJourneyPolicy(xJourneyPolicyExpected)
                .clientId(clientIdExpected)
                .processId(processIdExpected)
                .policyRequestType(policyRequestTypeExpected)
                .parentPolicyName(parentPolicyNameExpected)
                .contentType(contentTypeExpected)
                .acceptLanguage(acceptLanguageExpected)
                .domain(domainExpected)
                .subDomain(subDomainExpected)
                .userAgent(userAgentExpected)
                .accept(acceptExpected)
                .build();
    }

    private void createDefaultDxHeaders() {
        final Map<String, String> map = new HashMap<>();
        map.put(API_KEY, apiKeyExpected);
        map.put(CHANNEL_TYPE, channelTypeExpected);
        map.put(CLIENT_API_KEY, "clientApiKey");
        map.put(CLIENT_CORRELATION_ID, "clientCorrelation");
        map.put(CUSTOMER_IP_ADDR, "customerIp");
        map.put(COUNTRY_CODE, countryCodeExpected);
        map.put(X_JOURNEY_POLICY, xJourneyPolicyExpected);
        map.put("Business-Event", businessEventExpected);
        dxHeadersExpected = Collections.unmodifiableMap(map);
    }

    private void createDefaultPolicySuccessResult(final boolean withProcessMetadata) {
        policyResultExpected = new PolicyService.Result.PolicySuccess(
                PolicyResponse.builder()
                        .policyStatus(policyStatusExpected)
                        .errorInfo(errorInfoExpected)
                        .metadata(withProcessMetadata ?
                                new ProcessMetadata(
                                        processIdExpected,
                                        policyNameExpected,
                                        policyVersionExecutedExpected,
                                        stepExpirationExpected,
                                        availableNextStepsExpected,
                                        effectiveNextStepExpected,
                                        stepsCompletedExpected)
                                : null)
                        .build());
    }

    private void createDefaultPolicyResponse() {
        executePolicyResponseExpected = PolicyService.ExecutedPolicyResponse.builder()
                .setExecutedPolicyVersion(policyVersionExecutedExpected)
                .setAbacResponse(abacResponseExpected)
                .setExecutionData(executionDataExpected)
                .setResult(policyResultExpected)
                .setMockModeConfig(mockModeConfigExpected)
                .build();
    }

    private OperationalError expectedOperationalError(final Optional<ErrorResponse> chassisErrorResponse,
                                                      final AbstractChassisException chassisException) {
        return OperationalError.builder()
                .text(chassisErrorResponse.map(ErrorResponse::getText).orElse(DEFAULT_POLICY_ERROR_TEXT))
                .developerText(chassisErrorResponse.map(ErrorResponse::getDeveloperText)
                        .orElse(chassisException.getMessage() != null ? chassisException.getMessage() :
                                chassisException.getApiError().getErrorDetails().toString()))
                .errorId(chassisErrorResponse.map(ErrorResponse::getId).orElse(null))
                .build();
    }

    private OperationalError expectedOperationalError(final ErrorInfo errorInfo) {
        return OperationalError.builder()
                .errorId(errorInfo.getId())
                .text(errorInfo.getText())
                .developerText(errorInfo.getDeveloperText())
                .build();
    }

}

