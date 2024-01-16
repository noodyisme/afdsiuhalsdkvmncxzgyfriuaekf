package com.capitalone.identity.identitybuilder.policycore.service;

import com.capitalone.chassis.engine.model.context.NullRequestContext;
import com.capitalone.chassis.engine.model.error.ErrorResponse;
import com.capitalone.chassis.engine.model.exception.*;
import com.capitalone.identity.identitybuilder.model.ConfigStoreItemInfo;
import com.capitalone.identity.identitybuilder.model.EntityActivationStatus;
import com.capitalone.identity.identitybuilder.model.EntityInfo;
import com.capitalone.identity.identitybuilder.model.parsing.PolicyDefinition;
import com.capitalone.identity.identitybuilder.policycore.abac.model.AbacResponse;
import com.capitalone.identity.identitybuilder.policycore.abac.model.PolicyAuthAction;
import com.capitalone.identity.identitybuilder.policycore.abac.model.PolicyAuthResource;
import com.capitalone.identity.identitybuilder.policycore.abac.model.PolicyAuthSubject;
import com.capitalone.identity.identitybuilder.policycore.abac.service.AuthorizationEnforcer;
import com.capitalone.identity.identitybuilder.policycore.camel.PolicyConstants;
import com.capitalone.identity.identitybuilder.policycore.camel.components.devexchange.DevExchangeException;
import com.capitalone.identity.identitybuilder.policycore.camel.util.CamelCacheUtil;
import com.capitalone.identity.identitybuilder.policycore.configmanagement.UnsupportedBusinessEventException;
import com.capitalone.identity.identitybuilder.policycore.model.*;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.PolicyEvaluatedAuditor;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.model.policy.OperationalAuditExecutionData;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.model.policy.PolicyRequestType;
import com.capitalone.identity.identitybuilder.policycore.policytopolicy.camel.PolicyToPolicyException;
import com.capitalone.identity.identitybuilder.policycore.rest.v1.ExecutionContext;
import com.capitalone.identity.identitybuilder.policycore.schema.SchemaManager;
import com.capitalone.identity.identitybuilder.policycore.service.constants.ApplicationConstants;
import com.capitalone.identity.identitybuilder.policycore.service.dao.impl.DevExchangeDAOImpl;
import com.capitalone.identity.identitybuilder.policycore.service.exception.PolicyCacheException;
import com.capitalone.identity.identitybuilder.policycore.service.jws.JwsService;
import com.capitalone.identity.identitybuilder.policycore.service.jws.JwsServiceValidationResponse;
import com.capitalone.identity.identitybuilder.policycore.service.logging.AuditLogger;
import com.capitalone.identity.identitybuilder.policycore.service.logging.PolicyAuditContext;
import com.capitalone.identity.identitybuilder.policycore.service.logging.PolicyServiceExecutor;
import com.capitalone.identity.identitybuilder.policycore.service.util.MockPolicyVersion;
import com.capitalone.identity.identitybuilder.policycore.service.versions.PolicyVersionService;
import com.capitalone.identity.identitybuilder.policycore.utils.CamelSpringBootContextAwareTest;
import com.capitalone.identity.identitybuilder.policycore.utils.XmlDslCamelTestContextConfiguration;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.*;
import lombok.SneakyThrows;
import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.core.convert.ConversionService;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.*;

import static com.capitalone.identity.identitybuilder.policycore.service.constants.ApplicationConstants.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@CamelSpringBootTest
@CamelSpringBootContextAwareTest(
        contextConfigClasses = PolicyServiceTest.ContextConfig.class,
        excludeFromComponentScan = @ComponentScan.Filter(type = FilterType.ASPECTJ,
                pattern = "com.capitalone.identity.identitybuilder.policycore.service"))
@DirtiesContext
@ExtendWith(MockitoExtension.class)
class PolicyServiceTest {

    private static final String APP_NAME = "appName";

    private static final String API_KEY = "apiKey";
    private static final String CLIENT_API_KEY = "clientApiKey";
    private static final String SESSION_CORRELATION_ID = "sessionCorrelationId";
    private static final String CHANNEL_TYPE = "channelType";
    private static final String CCID = "clientCorrelationId";
    private static final String COUNTRY_CODE = "countryCode";
    private static final String CUSTOMER_IP = "customerIp";
    private static final String CONTENT_TYPE = "contentType";
    private static final String LANGUAGE = "language";
    private static final String DOMAIN = "domain";
    private static final String SUBDOMAIN = "subDomain";
    private static final String USER_AGENT = "userAgent";
    private static final String ACCEPT = "accept";
    private static final String CLIENT_ID = "clientId";
    private static final String BUSINESS_EVENT = "businessEvent";
    private static final String X_JOURNEY_POLICY = "xJourneyPolicy";
    private static final String POLICY_NAME = "policy_name";
    private static final String POLICY_VERSION = "1.0";
    private static final String PROCESS_ID = UUID.randomUUID().toString();
    private static final String STEP = "step1";
    private static final Boolean MOCK_MODE = false;
    public static APIRequest defaultAPIRequest = new APIRequest(API_KEY, CLIENT_API_KEY, CCID, CUSTOMER_IP, CONTENT_TYPE,
            CHANNEL_TYPE, COUNTRY_CODE, LANGUAGE, DOMAIN, SUBDOMAIN, USER_AGENT, ACCEPT, CLIENT_ID, BUSINESS_EVENT,
            X_JOURNEY_POLICY, "customerIdToken", SESSION_CORRELATION_ID, POLICY_NAME, POLICY_VERSION, PROCESS_ID, STEP,
            MOCK_MODE);

    private static final ObjectMapper mapper = new ObjectMapper();
    private static final String SCHEMA_SOURCE = "{\n"
            + " \"$schema\": \"http://json-schema.org/draft/2019-09/schema#\",\n"
            + "  \"type\": \"object\",\n"
            + "  \"properties\": {\n"
            + "    \"name\": { \"type\": \"string\" }\n"
            + "  },\n"
            + "  \"additionalProperties\": false\n"
            + "}\n";
    private static final LocalDateTime stepExpiration = LocalDateTime.now();
    private static final String fieldsToFilter = "none";
    @Autowired
    private CamelContext camelContext;
    @Mock
    private SchemaManager schemaManager;
    @Mock
    private PolicyStateCacheService policyStateCacheService;
    @Mock
    private ConversionService conversionService;
    @Mock
    private CamelCacheUtil camelCacheUtil;
    @Autowired
    private PolicyVersionService policyVersionService;

    @Mock(lenient = true)
    private JwsService jwsService;
    @Mock
    private AuditLogger auditLogger;
    @Mock
    private PolicyEvaluatedAuditor policyEvaluatedAuditor;

    private ExecutionContext ctx;
    private APIRequest apiRequest;
    private PolicyRequest policyRequest;
    private Level oldLogLevel = null;
    private Map<String, Serializable> policyBody;
    private PolicyRequest.SystemParams systemParams;

    @Mock
    private AuthorizationEnforcer<PolicyAuthSubject, PolicyAuthResource, PolicyAuthAction> authorizationEnforcer;

    private PolicyService policyService;

    // Sub-classes
    private PolicyServiceExecutor policyServiceExecutor;
    private PolicyService.ProcessMetadataHelper processMetadataHelper;
    private PolicyService.PolicyExecutionHelper policyExecutionHelper;

    @Captor
    ArgumentCaptor<PolicyRequest> policyRequestArgumentCaptor;

    @BeforeEach
    public void setup() throws Exception {
        camelContext.start();
        ctx = new ExecutionContext();
        ctx.setApiKey(API_KEY);

        ctx.setBusinessEvent(BUSINESS_EVENT);
        ctx.setSessionCorrelationId(SESSION_CORRELATION_ID);
        ctx.setChannelType(CHANNEL_TYPE);
        ctx.setClientCorrelationId(CCID);
        ctx.setCountryCode(COUNTRY_CODE);
        ctx.setCustomerIpAddress(CUSTOMER_IP);
        policyBody = new HashMap<>();
        policyBody.put("foo", "bar");

        apiRequest = new APIRequest(API_KEY, null, CCID, CUSTOMER_IP, null, CHANNEL_TYPE,
                COUNTRY_CODE, null, null, null, null, null, "clientId", "businessEvent", "xJourneyPolicy", "customerIdToken", "sessionCorrelationId", "success",
                "1.0", "uuid", null, null);

        when(auditLogger.getFieldsToFilter()).thenReturn(fieldsToFilter);
        systemParams = new PolicyRequest.SystemParams(fieldsToFilter, "test", null);
        policyRequest = new PolicyRequest(apiRequest, PolicyRequestType.CREATE, policyBody, systemParams);

        createDefaultPolicyService();
    }

    private void createDefaultPolicyService() {
        processMetadataHelper = new PolicyService.ProcessMetadataHelper(policyStateCacheService);
        policyExecutionHelper = Mockito.spy(new PolicyService.PolicyExecutionHelper(false, schemaManager, camelContext,
                processMetadataHelper, camelCacheUtil, authorizationEnforcer, true, policyVersionService, false, ""));
        policyServiceExecutor = new PolicyServiceExecutor(auditLogger, policyEvaluatedAuditor,
                conversionService, policyExecutionHelper);
        policyService = new PolicyService(policyServiceExecutor, APP_NAME, schemaManager, policyStateCacheService,
                auditLogger, processMetadataHelper, policyVersionService);
    }

    @AfterEach
    public void cleanup() {
        if (oldLogLevel != null) {
            setTargetLogLevel(oldLogLevel);
        }
    }

    @Test
    void testExecutePolicy() {
        PolicyService.ExecutedPolicyResponse executeResponse = policyExecutionHelper.executePolicy(policyRequest);
        PolicyResponse resp = executeResponse.getResult().getPolicyResponse();
        assertEquals(PolicyStatus.SUCCESS, resp.getPolicyStatus());
        assertNull(resp.getErrorInfo());
        assertNull(resp.getMetadata());
        assertEquals(expectedResult(policyRequest, "success_1.0.0"), resp.getResults());
        assertNotNull(executeResponse.getExecutionData());
        assertNull(executeResponse.getMockModeConfig());
    }

    @Test
    void  testExecutePolicyWithMockModeConfigured() {
        apiRequest = new APIRequest(API_KEY, null, CCID, CUSTOMER_IP, null, CHANNEL_TYPE,
                COUNTRY_CODE, null, null, null, null, null, "clientId",
                "businessEvent", "xJourneyPolicy", "customerIdToken",
                "sessionCorrelationId", "testMockModeConfigured",
                "1.0", "uuid", null, null);
        policyRequest = new PolicyRequest(apiRequest, PolicyRequestType.CREATE, policyBody, systemParams);

        PolicyService.ExecutedPolicyResponse executeResponse = policyExecutionHelper.executePolicy(policyRequest);
        PolicyResponse resp = executeResponse.getResult().getPolicyResponse();
        assertEquals(PolicyStatus.SUCCESS, resp.getPolicyStatus());
        assertNull(resp.getErrorInfo());
        assertNull(resp.getMetadata());
        assertEquals(expectedResult(policyRequest, "testMockModeConfigured_1.0.0"), resp.getResults());
        assertNotNull(executeResponse.getExecutionData());
        assertTrue(executeResponse.getMockModeConfig());
    }

    @Test
    void testExecutePolicyWithNullParameters() {
        policyRequest = new PolicyRequest(apiRequest, PolicyRequestType.CREATE, null, systemParams);
        PolicyService.ExecutedPolicyResponse executeResponse = policyExecutionHelper.executePolicy(policyRequest);
        PolicyResponse resp = executeResponse.getResult().getPolicyResponse();
        assertEquals(PolicyStatus.SUCCESS, resp.getPolicyStatus());
        assertNull(resp.getErrorInfo());
        assertNull(resp.getMetadata());
        assertEquals(expectedResult(policyRequest, "success_1.0.0"), resp.getResults());
    }

    @ParameterizedTest
    @ValueSource(strings = {"business.event.value", "business_event.value", "business-event.value"})
    @NullSource
    void testExecutePolicyParseBusinessEvent_valid(String businessEventValue) {
        apiRequest = new APIRequest(API_KEY, null, CCID, CUSTOMER_IP, null, CHANNEL_TYPE,
                COUNTRY_CODE, null, null, null, null, null, "clientId", businessEventValue, "xJourneyPolicy", "customerIdToken", "sessionCorrelationId", "success",
                "1.0", "uuid", null, null);
        policyRequest = new PolicyRequest(apiRequest, PolicyRequestType.CREATE, policyBody, systemParams);
        PolicyService.ExecutedPolicyResponse executeResponse = policyExecutionHelper.executePolicy(policyRequest);
        PolicyResponse resp = executeResponse.getResult().getPolicyResponse();
        assertEquals(PolicyStatus.SUCCESS, resp.getPolicyStatus());
    }

    @ParameterizedTest
    @ValueSource(strings = {"business+event+value", "business/event/value", ""})
    void testExecutePolicyParseBusinessEvent_invalid(String businessEventValue) {
        apiRequest = new APIRequest(API_KEY, null, CCID, CUSTOMER_IP, null, CHANNEL_TYPE,
                COUNTRY_CODE, null, null, null, null, null, "clientId", businessEventValue, "xJourneyPolicy", "customerIdToken", "sessionCorrelationId", "success",
                "1.0", "uuid", null, null);
        policyRequest = new PolicyRequest(apiRequest, PolicyRequestType.CREATE, policyBody, systemParams);
        ChassisErrorCode errorCode;
        if (Objects.equals(businessEventValue, "")) {
            errorCode = new ChassisErrorCode(INVALID_BUSINESS_EVENT_ID, INVALID_BUSINESS_EVENT_TEXT,
                    MISSING_BUSINESS_EVENT_DEV_TEXT);
        } else {
            errorCode = new ChassisErrorCode(INVALID_BUSINESS_EVENT_ID, INVALID_BUSINESS_EVENT_TEXT,
                    INVALID_BUSINESS_EVENT_DEV_TEXT);
        }
        ChassisBusinessException expectedException = new ChassisBusinessException(errorCode, 400);
        PolicyService.ExecutedPolicyResponse executeResponse = policyExecutionHelper.executePolicy(policyRequest);
        assertEquals(new PolicyService.Result.SystemError(expectedException), executeResponse.getResult());
    }

    @Test
    void testExecutePolicySchemaNotFound() {
        // Have the schema validator mock throw an exception.
        RuntimeException runtimeException = new RuntimeException("test exception");
        when(schemaManager.validate(anyString(), any(JsonNode.class))).thenThrow(runtimeException);
        Exception error = assertThrows(RuntimeException.class, () -> policyExecutionHelper.executePolicy(policyRequest));
        assertEquals(runtimeException, error);

    }

    @Test
    void testExecutePolicyNullCamelCacheUtil() {
        apiRequest = new APIRequest(API_KEY, null, CCID, CUSTOMER_IP, null, CHANNEL_TYPE,
                COUNTRY_CODE, null, null, null, null, null, "clientId", "businessEvent", "xJourneyPolicy", "customerIdToken", "sessionCorrelationId", "success",
                "1.0", "uuid", "step2", null);
        policyRequest = new PolicyRequest(apiRequest, PolicyRequestType.CREATE, policyBody, systemParams);
        policyStateCacheService = null;
        camelCacheUtil = null;
        createDefaultPolicyService();
        PolicyService.Result result = policyExecutionHelper.executePolicy(policyRequest).getResult();
        AbstractChassisException exception = result.getException();
        assertTrue(exception instanceof ChassisSystemException);
        assertEquals(String.format(ApplicationConstants.MISSING_CACHE_PROPERTY, ApplicationConstants.ENABLE_CACHE_PROPERTY), exception.getMessage());
    }

    @Test
    void testPolicySchemaError() {
        // Have the schema validator mock return an error.
        ValidationMessage msg = ValidationMessage.of("foo", ValidatorTypeCode.FALSE, "$.some.path");
        Set<ValidationMessage> set = new HashSet<>();
        set.add(msg);
        when(schemaManager.validate(anyString(), any(JsonNode.class))).thenReturn(set);

        PolicyService.Result result = policyExecutionHelper.executePolicy(policyRequest).getResult();
        PolicyResponse resp = result.getPolicyResponse();
        assertTrue(validateError(resp, PolicyStatus.INVALID, "0", "Schema Validation Failure", "[" + msg.toString() + "]"));
    }

    @Test
    void testPolicyNotFound() {
        String policyName = "missing";
        apiRequest = new APIRequest(API_KEY, null, CCID, CUSTOMER_IP, null, CHANNEL_TYPE,
                COUNTRY_CODE, null, null, null, null, null, "clientId", "businessEvent", "xJourneyPolicy", "customerIdToken", "sessionCorrelationId",
                policyName, "1.0", "uuid", null, null);
        policyRequest = new PolicyRequest(apiRequest, PolicyRequestType.CREATE, policyBody, systemParams);
        PolicyService.Result result = policyExecutionHelper.executePolicy(policyRequest).getResult();
        assertTrue(result.getException() instanceof NotFoundException);

        policyVersionService.remove(MockPolicyVersion.create("a/b/missing/1.0"));
        result = policyExecutionHelper.executePolicy(policyRequest).getResult();
        assertTrue(result.getException() instanceof NotFoundException);
    }

    @Test
    void testPolicyDevExchangeException() {
        apiRequest = new APIRequest(API_KEY, null, CCID, CUSTOMER_IP, null, CHANNEL_TYPE,
                COUNTRY_CODE, null, null, null, null, null, "clientId", "businessEvent", "xJourneyPolicy", "customerIdToken", "sessionCorrelationId",
                "throwDevExchangeException", "1.0", "uuid", null, null);
        policyRequest = new PolicyRequest(apiRequest, PolicyRequestType.CREATE, policyBody, systemParams);
        PolicyService.Result result = policyExecutionHelper.executePolicy(policyRequest).getResult();
        PolicyResponse resp = result.getPolicyResponse();
        assertTrue(validateError(resp, PolicyStatus.FAILURE, "202020", "Downstream API error", "Downstream API \"null\" returned 0: body=\"null\""));
    }

    // Purely for coverage purposes.
    @Test
    void testPolicyDevExchangeExceptionWithDebugLogging() {
        oldLogLevel = setTargetLogLevel(Level.DEBUG);
        apiRequest = new APIRequest(API_KEY, null, CCID, CUSTOMER_IP, null, CHANNEL_TYPE,
                COUNTRY_CODE, null, null, null, null, null, "clientId", "businessEvent", "xJourneyPolicy", "customerIdToken", "sessionCorrelationId",
                "throwDevExchangeException", "1.0", "uuid", null, null);
        policyRequest = new PolicyRequest(apiRequest, PolicyRequestType.CREATE, policyBody, systemParams);
        PolicyService.ExecutedPolicyResponse executeResponse = policyExecutionHelper.executePolicy(policyRequest);
        PolicyResponse resp = executeResponse.getResult().getPolicyResponse();
        assertTrue(validateError(resp, PolicyStatus.FAILURE, "202020", "Downstream API error", "Downstream API \"null\" returned 0: body=\"null\""));
    }

    @Test
    void testPolicyChassisBusinessException() {
        apiRequest = new APIRequest(API_KEY, null, CCID, CUSTOMER_IP, null, CHANNEL_TYPE,
                COUNTRY_CODE, null, null, null, null, null, "clientId", "businessEvent", "xJourneyPolicy", "customerIdToken", "sessionCorrelationId",
                "throwChassisBusinessException", "1.0", "uuid", null, null);
        policyRequest = new PolicyRequest(apiRequest, PolicyRequestType.CREATE, policyBody, systemParams);
        PolicyService.ExecutedPolicyResponse executeResponse = policyExecutionHelper.executePolicy(policyRequest);
        PolicyResponse resp = executeResponse.getResult().getPolicyResponse();
        assertTrue(validateError(resp, PolicyStatus.FAILURE, "200002", "Error occurred during policy execution", "com.capitalone.chassis.engine.model.exception.ChassisBusinessException: test exception"));
    }

    @Test
    void testCacheException() {
        apiRequest = new APIRequest(API_KEY, null, CCID, CUSTOMER_IP, null, CHANNEL_TYPE,
                COUNTRY_CODE, null, null, null, null, null, "clientId", "businessEvent", "xJourneyPolicy", "customerIdToken", "sessionCorrelationId",
                "throwReadMissPolicyCacheException", "1.0", "uuid", null, null);
        policyRequest = new PolicyRequest(apiRequest, PolicyRequestType.CREATE, policyBody, systemParams);
        PolicyService.ExecutedPolicyResponse executeResponse = policyExecutionHelper.executePolicy(policyRequest);
        PolicyResponse resp = executeResponse.getResult().getPolicyResponse();
        assertTrue(validateError(resp, PolicyStatus.FAILURE, "904992", "Cache Read Miss", "Resource id testId is either missing or expired."));
    }

    @Test
    void testPolicyInvalidUseCaseException() {
        apiRequest = new APIRequest(API_KEY, null, CCID, CUSTOMER_IP, null, CHANNEL_TYPE,
                COUNTRY_CODE, null, null, null, null, null, "clientId", "businessEvent", "xJourneyPolicy", "sessionCorrelationId", "customerIdToken",
                "throwUnsupportedBusinessEventException", "1.0", "uuid", null, null);
        policyRequest = new PolicyRequest(apiRequest, PolicyRequestType.CREATE, policyBody, systemParams);
        PolicyService.ExecutedPolicyResponse executeResponse = policyExecutionHelper.executePolicy(policyRequest);
        PolicyResponse resp = executeResponse.getResult().getPolicyResponse();
        assertTrue(validateError(resp, PolicyStatus.FAILURE, INVALID_BUSINESS_EVENT_ID, "Invalid Business Event", "test exception"));
    }

    @Test
    void testExecutePolicyChildPolicyMultiStep() {
        apiRequest = new APIRequest(API_KEY, null, CCID, CUSTOMER_IP, null, CHANNEL_TYPE,
                COUNTRY_CODE, null, null, null, null, null, "clientId", "businessEvent", "xJourneyPolicy", "customerIdToken", "sessionCorrelationId",
                "success", "1.0", "uuid", "step2", null);
        PolicyState policyState = new PolicyState();
        PolicyDefinition mock = new PolicyDefinition("0/success", "1.0");
        EntityInfo.Policy policyInfo = new EntityInfo.Policy(mock, new HashSet<>(Collections.singletonList(new ConfigStoreItemInfo("O/b/c/1.0/process/a.xml", "a"))));
        policyRequest = new PolicyRequest(policyBody, policyInfo, "step2", "businessEvent", "xJourneyPolicy", "clientId",
                systemParams, "uuid", PolicyRequestType.RESUME, policyState, new HashMap<>(), true, "customerIdToken", null);
        Throwable throwable = assertThrows(PolicyToPolicyException.class, () -> policyExecutionHelper.executePolicy(policyRequest));
        assertTrue(throwable.getMessage().contains("Child policies must be single step policies"));
    }

    @Test
    void testCreateProcess() {
        when(authorizationEnforcer.authorize(any(), any(), any())).thenReturn(new AbacResponse(false, true, null, null));
        APIResponse.APISuccessResponse resp = (APIResponse.APISuccessResponse) policyService.createProcess(apiRequest, policyBody);
        assertEquals(PolicyStatus.SUCCESS, resp.getPolicyStatus());
        assertNull(resp.getErrorInfo());
        assertNull(resp.getMetadata());
        assertEquals(expectedResult(policyRequest, "success_1.0.0"), resp.getResults());
    }

    @Test
    void testCreateProcessNoEndpoint() {
        when(conversionService.convert(any(), any())).thenReturn(ErrorResponse.getEmptyResponse());
        final NotFoundException expectedException =
                assertThrows(NotFoundException.class, () -> policyService.createProcess(defaultAPIRequest, policyBody));
        final PolicyService.ExecutedPolicyResponse expectedExecutionPolicyResponse = PolicyService.ExecutedPolicyResponse.builder()
                .setResult(new PolicyService.Result.SystemError(expectedException))
                .build();
        verify(auditLogger).logChassisAudits(any(), any(), any(), any(), any(), any(), any());
        verifyChassisResponseErrorOperationalAudit(PolicyAuditContext.of(defaultAPIRequest, PolicyRequestType.CREATE),
                expectedExecutionPolicyResponse);
    }

    @Test
    void testCreateProcess_ABAC_Enforced() {
        when(authorizationEnforcer.authorize(any(), any(), any())).thenReturn(new AbacResponse(true, true, null, null));
        assertDoesNotThrow(() -> policyService.createProcess(apiRequest, policyBody));


        when(authorizationEnforcer.authorize(any(), any(), any())).thenReturn(new AbacResponse(false, false, null, null));
        assertThrows(ChassisSystemException.class, () -> policyService.createProcess(apiRequest, policyBody));
    }

    @Test
    void testMissingResourceId() {
        when(policyStateCacheService.retrieveFromCache(anyString())).thenReturn(null);
        String resourceId = "missing";
        APIRequest apiRequest1 = new APIRequest(API_KEY, null, CCID, CUSTOMER_IP, null, CHANNEL_TYPE,
                COUNTRY_CODE, null, null, null, null, null, "clientId", "businessEvent", "xJourneyPolicy", "customerIdToken", "sessionCorrelationId",
                null, null, resourceId, "step2", null);
        NotFoundException notFoundException = assertThrows(NotFoundException.class, () -> policyService.resumeProcess(apiRequest1, policyBody));
        assertEquals(String.format(ApplicationConstants.RESOURCE_ID_NOT_FOUND, resourceId), notFoundException.getMessage());
    }

    @Test
    void testResumeProcess() {
        PolicyState policyState = new PolicyState();
        policyState.setPolicyName("success");
        policyState.setPolicyVersion("1.0");
        Set<String> availableNextSteps = new HashSet<>();
        availableNextSteps.add("step2");
        policyState.setAvailableNextSteps(availableNextSteps);
        List<String> stepsCompleted = new ArrayList<>();
        stepsCompleted.add("start");
        policyState.setStepsCompleted(stepsCompleted);

        String resourceId = "sampleResourceId";

        AbacResponse abacResponseExpected = new AbacResponse(false, true, null, null);

        when(policyStateCacheService.retrieveFromCache(anyString())).thenReturn(policyState);
        when(policyStateCacheService.getStepExpiration(anyString())).thenReturn(stepExpiration);
        when(authorizationEnforcer.authorize(any(), any(), any())).thenReturn(abacResponseExpected);

        APIRequest apiRequest1 = new APIRequest(API_KEY, null, CCID, CUSTOMER_IP, null, CHANNEL_TYPE,
                COUNTRY_CODE, null, null, null, null, null, "clientId", "businessEvent", "xJourneyPolicy", "customerIdToken", "sessionCorrelationId", null,
                null, resourceId, "step2", null);

        APIResponse.APISuccessResponse resp = (APIResponse.APISuccessResponse) policyService.resumeProcess(apiRequest1, policyBody);
        assertEquals(PolicyStatus.SUCCESS, resp.getPolicyStatus());
        assertNull(resp.getErrorInfo());

        ProcessMetadata processMetadata = new ProcessMetadata(
                resourceId, "success", "1.0", stepExpiration, availableNextSteps, "", stepsCompleted
        );

        assertEquals(processMetadata, resp.getMetadata());
        Map<String, Object> resultExpected = expectedResult(policyRequest, "success_1.0.0-step2");
        assertEquals(resultExpected, resp.getResults());
        verify(auditLogger).logAudits(any(), any(), any(), any(), any(), any());
        verifyStandardOperationalAudit(PolicyAuditContext.of(apiRequest1, PolicyRequestType.RESUME),
                processMetadata, abacResponseExpected, resultExpected);
    }

    @Test
    void testResumeProcessNullStep() {
        PolicyState policyState = new PolicyState();
        policyState.setPolicyName("success");
        policyState.setPolicyVersion("1.0");
        Set<String> availableNextSteps = new HashSet<>();
        availableNextSteps.add("step2");
        policyState.setAvailableNextSteps(availableNextSteps);
        policyState.setEffectiveNextStep("step2");
        List<String> stepsCompleted = new ArrayList<>();
        stepsCompleted.add("start");
        policyState.setStepsCompleted(stepsCompleted);

        String resourceId = "sampleResourceId";

        AbacResponse abacResponseExpected = new AbacResponse(false, true, null, null);
        Map<String, Object> resultMapExpected = expectedResult(policyRequest, "success_1.0.0-step2");

        when(policyStateCacheService.retrieveFromCache(anyString())).thenReturn(policyState);
        when(policyStateCacheService.getStepExpiration(anyString())).thenReturn(stepExpiration);
        when(authorizationEnforcer.authorize(any(), any(), any())).thenReturn(abacResponseExpected);

        APIRequest apiRequest1 = new APIRequest(API_KEY, null, CCID, CUSTOMER_IP, null, CHANNEL_TYPE,
                COUNTRY_CODE, null, null, null, null, null, "clientId", "businessEvent", "xJourneyPolicy", "customerIdToken", "sessionCorrelationId",
                null, null, resourceId, null, null);
        APIResponse.APISuccessResponse resp = (APIResponse.APISuccessResponse) policyService.resumeProcess(apiRequest1, policyBody);
        assertEquals(PolicyStatus.SUCCESS, resp.getPolicyStatus());
        assertNull(resp.getErrorInfo());

        ProcessMetadata processMetadataExpected = new ProcessMetadata(
                resourceId, "success", "1.0", stepExpiration, availableNextSteps, "step2", stepsCompleted
        );

        assertEquals(processMetadataExpected, resp.getMetadata());
        assertEquals(resultMapExpected, resp.getResults());
        verify(auditLogger).logAudits(any(), any(), any(), any(), any(), any());
        verifyStandardOperationalAudit(PolicyAuditContext.of(apiRequest1, PolicyRequestType.RESUME), processMetadataExpected, abacResponseExpected, resultMapExpected);
    }

    @Test
    void testResumeProcessStartStep() {
        PolicyState policyState = new PolicyState();
        policyState.setPolicyName("success");
        policyState.setPolicyVersion("1.0");
        Set<String> availableNextSteps = new HashSet<>();
        availableNextSteps.add("start");
        policyState.setAvailableNextSteps(availableNextSteps);
        List<String> stepsCompleted = new ArrayList<>();
        stepsCompleted.add("start");
        policyState.setStepsCompleted(stepsCompleted);

        String resourceId = "sampleResourceId";

        Map<String, Object> resultMapExpected = expectedResult(policyRequest, "success_1.0.0");
        AbacResponse abacResponseExpected = new AbacResponse(false, true, null, null);

        when(policyStateCacheService.retrieveFromCache(anyString())).thenReturn(policyState);
        when(authorizationEnforcer.authorize(any(), any(), any())).thenReturn(abacResponseExpected);

        apiRequest = new APIRequest(API_KEY, null, CCID, CUSTOMER_IP, null, CHANNEL_TYPE,
                COUNTRY_CODE, null, null, null, null, null, "clientId", "businessEvent", "xJourneyPolicy", "customerIdToken", "sessionCorrelationId",
                null, null, resourceId, "start", null);

        policyRequest = new PolicyRequest(apiRequest, PolicyRequestType.CREATE, policyBody, systemParams);
        APIResponse.APISuccessResponse resp = (APIResponse.APISuccessResponse) policyService.resumeProcess(apiRequest, policyBody);
        assertEquals(PolicyStatus.SUCCESS, resp.getPolicyStatus());
        assertNull(resp.getErrorInfo());
        assertNull(resp.getMetadata());
        assertEquals(resultMapExpected, resp.getResults());
        verify(auditLogger).logAudits(any(), any(), any(), any(), any(), any());
        verifyStandardOperationalAudit(PolicyAuditContext.of(apiRequest, PolicyRequestType.RESUME), null, abacResponseExpected, resultMapExpected);
    }

    /**
     * Verifies the expected request, response, exception, and any other metadata is correctly forwarded to
     * operational audit to be sent to onelake, snowflake & splunk for operational non-error scenarios
     *
     * @param policyAuditContextExpected expected audit context
     * @param processMetadataExpected    expected process metadata
     * @param abacResponseExpected       expected abac response
     * @param resultExpected             expected result map
     */
    private void verifyStandardOperationalAudit(final PolicyAuditContext policyAuditContextExpected,
                                                final ProcessMetadata processMetadataExpected, final AbacResponse abacResponseExpected,
                                                final Map<String, Object> resultExpected) {
        verify(policyExecutionHelper).executePolicy(policyRequestArgumentCaptor.capture());
        final PolicyRequest policyRequestActual = policyRequestArgumentCaptor.getValue();

        final PolicyService.Result.PolicySuccess policySuccess = new PolicyService.Result.PolicySuccess(PolicyResponse.builder()
                .policyStatus(PolicyStatus.SUCCESS)
                .results(resultExpected)
                .errorInfo(null)
                .metadata(processMetadataExpected)
                .suppressedErrorInfo(null)
                .build());

        verify(policyEvaluatedAuditor).audit(
                eq(policyRequestActual),
                eq(PolicyService.ExecutedPolicyResponse.builder()
                        .setExecutedPolicyVersion("1.0")
                        .setExecutedPolicyVersionWithPatch("1.0.0")
                        .setAbacResponse(abacResponseExpected)
                        .setExecutionData(new OperationalAuditExecutionData())
                        .setResult(policySuccess)
                        .build()),
                eq(null),
                argThat(startTimestamp -> startTimestamp <= System.currentTimeMillis()),
                eq(policyAuditContextExpected));
    }


    /**
     * Verifies the expected request, response, exception, and any other metadata is correctly forwarded to
     * operational audit to be sent to onelake, snowflake & splunk for operational error scenarios where the
     * request had been successfully created but the response object is expected to contain a caught chassis
     * exception.
     *
     * @param policyAuditContextExpected     the expected context
     * @param executedPolicyResponseExpected the expected response to be audited
     */
    private void verifyChassisResponseErrorOperationalAudit(final PolicyAuditContext policyAuditContextExpected,
                                                            final PolicyService.ExecutedPolicyResponse executedPolicyResponseExpected) {
        verify(policyExecutionHelper).executePolicy(policyRequestArgumentCaptor.capture());
        final PolicyRequest policyRequestActual = policyRequestArgumentCaptor.getValue();
        final AbstractChassisException chassisException = executedPolicyResponseExpected.getResult().getException();
        Assertions.assertNotNull(chassisException);
        verify(policyEvaluatedAuditor).auditChassisError(eq(policyRequestActual),
                eq(chassisException),
                argThat(errorResponse -> errorResponse == null || errorResponse.getId().equals(chassisException.getApiError().getId())),
                eq(executedPolicyResponseExpected),
                argThat(startTimestamp -> startTimestamp <= System.currentTimeMillis()),
                eq(policyAuditContextExpected));
    }

    private static PolicyRequest expectedPlaceholderPolicyRequest(final APIRequest apiRequest,
                                                                  final PolicyRequestType policyRequestType) {
        return PolicyRequest.builder()
                .policyName(String.valueOf(apiRequest.getPolicyName()))
                .policyVersionRequested(String.valueOf(apiRequest.getPolicyVersion()))
                .policyInfo(Optional.empty())
                .step(apiRequest.getStep())
                .businessEvent(apiRequest.getBusinessEvent())
                .xJourneyPolicy(apiRequest.getXJourneyPolicy())
                .clientId(apiRequest.getClientId())
                .policyParametersAudit("N/A")
                .processId(apiRequest.getProcessId())
                .policyRequestType(policyRequestType)
                .dxHeaders(new HashMap<>())
                .childPolicy(false)
                .build();
    }


    @Test
    void testResumeProcessMissingRequiredStep() {
        PolicyState policyState = new PolicyState();
        policyState.setPolicyName("success");
        policyState.setPolicyVersion("1.0");
        policyState.getAvailableNextSteps().add("step2");
        policyState.getAvailableNextSteps().add("step3");
        String resourceId = "sampleResourceId";
        when(policyStateCacheService.retrieveFromCache(resourceId)).thenReturn(policyState);
        APIRequest apiRequest1 = new APIRequest(API_KEY, null, CCID, CUSTOMER_IP, null, CHANNEL_TYPE,
                COUNTRY_CODE, null, null, null, null, null, "clientId", "businessEvent", "xJourneyPolicy", "customerIdToken", "sessionCorrelationId", null,
                null, resourceId, null, null);
        ChassisBusinessException chassisBusinessException = assertThrows(ChassisBusinessException.class, () -> policyService.resumeProcess(apiRequest1, policyBody));
        assertEquals(ApplicationConstants.MISSING_STEP_PARAMETER, chassisBusinessException.getMessage());
        verify(policyEvaluatedAuditor).audit(
                eq(expectedPlaceholderPolicyRequest(apiRequest1, PolicyRequestType.RESUME)),
                eq(null),
                eq(chassisBusinessException),
                argThat(startTimestamp -> startTimestamp <= System.currentTimeMillis()),
                eq(PolicyAuditContext.of(apiRequest1, PolicyRequestType.RESUME)));
    }

    @Test
    void testResumeProcessNoAvailableNextSteps() {
        PolicyState policyState = new PolicyState();
        String resourceId = "sampleResourceId";
        when(policyStateCacheService.retrieveFromCache(resourceId)).thenReturn(policyState);
        APIRequest apiRequest1 = new APIRequest(API_KEY, null, CCID, CUSTOMER_IP, null, CHANNEL_TYPE,
                COUNTRY_CODE, null, null, null, null, null, "clientId", "businessEvent", "xJourneyPolicy", "sessionCorrelationId", "customerIdToken", null,
                null, resourceId, null, null);
        RequestValidationException requestValidationException = assertThrows(RequestValidationException.class, () -> policyService.resumeProcess(apiRequest1, policyBody));
        assertEquals(String.format(ApplicationConstants.NO_AVAILABLE_NEXT_STEPS, resourceId), requestValidationException.getMessage());
        verify(policyEvaluatedAuditor).audit(
                eq(expectedPlaceholderPolicyRequest(apiRequest1, PolicyRequestType.RESUME)),
                eq(null),
                eq(requestValidationException),
                argThat(startTimestamp -> startTimestamp <= System.currentTimeMillis()),
                eq(PolicyAuditContext.of(apiRequest1, PolicyRequestType.RESUME)));
    }

    @Test
    void testResumeProcessStepNotAvailable() {
        PolicyState policyState = new PolicyState();
        policyState.getAvailableNextSteps().add("step2");
        policyState.getAvailableNextSteps().add("step3");
        String resourceId = "sampleResourceId";
        String stepName = "step4";
        when(policyStateCacheService.retrieveFromCache(resourceId)).thenReturn(policyState);
        APIRequest apiRequest1 = new APIRequest(API_KEY, null, CCID, CUSTOMER_IP, null, CHANNEL_TYPE,
                COUNTRY_CODE, null, null, null, null, null, "clientId", "businessEvent", "xJourneyPolicy", "sessionCorrelationId", "customerIdToken", null,
                null, resourceId, stepName, null);
        NotFoundException notFoundException = assertThrows(NotFoundException.class, () -> policyService.resumeProcess(apiRequest1, policyBody));
        assertEquals(String.format(ApplicationConstants.STEP_NOT_AVAILABLE, stepName, policyState.getAvailableNextSteps()), notFoundException.getMessage());
        verify(policyEvaluatedAuditor).audit(
                eq(expectedPlaceholderPolicyRequest(apiRequest1, PolicyRequestType.RESUME)),
                eq(null),
                eq(notFoundException),
                argThat(startTimestamp -> startTimestamp <= System.currentTimeMillis()),
                eq(PolicyAuditContext.of(apiRequest1, PolicyRequestType.RESUME)));
    }

    @Test
    void testResumeProcessMissingCacheService() {
        policyStateCacheService = null;
        createDefaultPolicyService();
        String resourceId = "resourceId";
        APIRequest apiRequest1 = new APIRequest(API_KEY, null, CCID, CUSTOMER_IP, null, CHANNEL_TYPE,
                COUNTRY_CODE, null, null, null, null, null, "clientId", "businessEvent", "xJourneyPolicy", "customerIdToken", "sessionCorrelationId",
                null, null, resourceId, null, null);
        ChassisSystemException chassisSystemException = assertThrows(ChassisSystemException.class, () -> policyService.resumeProcess(apiRequest1, policyBody));
        assertEquals(String.format(ApplicationConstants.MISSING_CACHE_PROPERTY, ApplicationConstants.ENABLE_CACHE_PROPERTY), chassisSystemException.getMessage());
        verify(policyEvaluatedAuditor).audit(
                eq(expectedPlaceholderPolicyRequest(apiRequest1, PolicyRequestType.RESUME)),
                eq(null),
                eq(chassisSystemException),
                argThat(startTimestamp -> startTimestamp <= System.currentTimeMillis()),
                eq(PolicyAuditContext.of(apiRequest1, PolicyRequestType.RESUME)));
    }

    @Test
    void testResumeProcessNullCamelCacheUtil() {
        camelCacheUtil = null;
        createDefaultPolicyService();

        PolicyState policyState = new PolicyState();
        policyState.setPolicyName("success");
        policyState.setPolicyVersion("1.0");
        Set<String> availableNextSteps = new HashSet<>();
        availableNextSteps.add("start");
        policyState.setAvailableNextSteps(availableNextSteps);
        List<String> stepsCompleted = new ArrayList<>();
        stepsCompleted.add("start");
        policyState.setStepsCompleted(stepsCompleted);
        AbacResponse abacResponseExpected = new AbacResponse(false, true, null, null);

        when(policyStateCacheService.retrieveFromCache(anyString())).thenReturn(policyState);
        when(authorizationEnforcer.authorize(any(), any(), any())).thenReturn(abacResponseExpected);
        ChassisSystemException chassisSystemException = assertThrows(ChassisSystemException.class, () -> policyService.resumeProcess(apiRequest, policyBody));
        assertEquals(String.format(ApplicationConstants.MISSING_CACHE_PROPERTY, ApplicationConstants.ENABLE_CACHE_PROPERTY), chassisSystemException.getMessage());
        PolicyService.ExecutedPolicyResponse executedPolicyResponseExpected =
                new PolicyService.ExecutedPolicyResponse("1.0", "1.0.0", abacResponseExpected, null, new OperationalAuditExecutionData(),
                        new PolicyService.Result.SystemError(chassisSystemException));
        verifyChassisResponseErrorOperationalAudit(PolicyAuditContext.of(apiRequest, PolicyRequestType.RESUME), executedPolicyResponseExpected);
    }

    @Test
    void getProcessMetadata() {
        PolicyState policyState = new PolicyState();
        String policyName = "success";
        String policyVersion = "1.0";
        policyState.setPolicyName(policyName);
        policyState.setPolicyVersion(policyVersion);
        policyState.getAvailableNextSteps().add("step2");
        policyState.getStepsCompleted().add("start");
        policyState.getStepsCompleted().add("step2");
        policyState.setEffectiveNextStep("step2");

        String resourceId = "sampleResourceId";
        when(policyStateCacheService.retrieveFromCache(resourceId)).thenReturn(policyState);
        when(policyStateCacheService.getStepExpiration(resourceId)).thenReturn(LocalDateTime.MAX);

        ProcessMetadata processMetadata = policyService.getProcessMetadata(resourceId);
        assertEquals(resourceId, processMetadata.getResourceId());
        assertEquals(policyName, processMetadata.getPolicyName());
        assertEquals(policyVersion, processMetadata.getPolicyVersion());
        assertEquals(LocalDateTime.MAX, processMetadata.getStepExpiration());
        assertEquals(policyState.getAvailableNextSteps(), processMetadata.getAvailableNextSteps());
        assertEquals(policyState.getStepsCompleted(), processMetadata.getStepsCompleted());
        assertEquals(policyState.getEffectiveNextStep(), processMetadata.getEffectiveNextStep());
    }

    @Test
    void getProcessMetadataNullPolicyState() {
        String resourceId = "sampleResourceId";
        when(policyStateCacheService.retrieveFromCache(resourceId)).thenReturn(null);
        when(policyStateCacheService.getStepExpiration(resourceId)).thenReturn(LocalDateTime.MAX);
        NotFoundException notFoundException = assertThrows(NotFoundException.class, () -> policyService.getProcessMetadata(resourceId));
        assertEquals(String.format(ApplicationConstants.RESOURCE_ID_NOT_FOUND, resourceId), notFoundException.getMessage());
    }

    @Test
    void getProcessMetadataNullStepExpiration() {
        PolicyState policyState = new PolicyState();
        String policyName = "success";
        String policyVersion = "1.0";
        policyState.setPolicyName(policyName);
        policyState.setPolicyVersion(policyVersion);
        policyState.getAvailableNextSteps().add("step2");
        policyState.getStepsCompleted().add("start");
        policyState.getStepsCompleted().add("step2");

        String resourceId = "sampleResourceId";
        when(policyStateCacheService.retrieveFromCache(resourceId)).thenReturn(policyState);
        when(policyStateCacheService.getStepExpiration(resourceId)).thenReturn(null);
        NotFoundException notFoundException = assertThrows(NotFoundException.class, () -> policyService.getProcessMetadata(resourceId));
        assertEquals(String.format(ApplicationConstants.RESOURCE_ID_NOT_FOUND, resourceId), notFoundException.getMessage());
    }

    @Test
    void getProcessMetadataMissingCacheService() {
        policyStateCacheService = null;
        camelCacheUtil = null;
        createDefaultPolicyService();
        ChassisSystemException chassisSystemException = assertThrows(ChassisSystemException.class, () -> policyService.getProcessMetadata("resourceId"));
        assertEquals(String.format(ApplicationConstants.MISSING_CACHE_PROPERTY, ApplicationConstants.ENABLE_CACHE_PROPERTY), chassisSystemException.getMessage());
    }

    @Test
    void getProcessMetadataNullEffectiveStep() {
        PolicyState policyState = new PolicyState();
        String policyName = "success";
        String policyVersion = "1.0";
        policyState.setPolicyName(policyName);
        policyState.setPolicyVersion(policyVersion);
        policyState.getAvailableNextSteps().add("step2");
        policyState.getStepsCompleted().add("start");
        policyState.getStepsCompleted().add("step2");
        policyState.setEffectiveNextStep(null);

        String resourceId = "sampleResourceId";
        when(policyStateCacheService.retrieveFromCache(resourceId)).thenReturn(policyState);
        when(policyStateCacheService.getStepExpiration(resourceId)).thenReturn(LocalDateTime.MAX);

        ProcessMetadata processMetadata = policyService.getProcessMetadata(resourceId);
        assertEquals(resourceId, processMetadata.getResourceId());
        assertEquals(policyName, processMetadata.getPolicyName());
        assertEquals(policyVersion, processMetadata.getPolicyVersion());
        assertEquals(LocalDateTime.MAX, processMetadata.getStepExpiration());
        assertEquals(policyState.getAvailableNextSteps(), processMetadata.getAvailableNextSteps());
        assertEquals(policyState.getStepsCompleted(), processMetadata.getStepsCompleted());
        assertEquals(policyState.getEffectiveNextStep(), processMetadata.getEffectiveNextStep());
    }

    @Test
    void testGetPolicyMetadata() {
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V201909);
        JsonSchema schema = factory.getSchema(SCHEMA_SOURCE);
        SchemaManager.Entry entry = new SchemaManager.Entry("success_1.0", "source", schema);
        when(schemaManager.getEntry(anyString())).thenReturn(entry);

        PolicyMetadata resp = policyService.getPolicyMetadata("success", "1.0", null);
        assertEquals("appName", resp.getDomain());
        assertEquals("success", resp.getPolicyName());
        assertEquals("1.0", resp.getPolicyVersion());
        assertEquals(mapper.convertValue(schema.getSchemaNode(), new TypeReference<Map<String, Object>>() {
        }), resp.getPolicyJsonSchema());
    }

    @Test
    void testGetPolicyMetadataWithStep() {
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V201909);
        JsonSchema schema = factory.getSchema(SCHEMA_SOURCE);
        SchemaManager.Entry entry = new SchemaManager.Entry("success_1.0-step2", "source", schema);
        when(schemaManager.getEntry(anyString())).thenReturn(entry);

        PolicyMetadata resp = policyService.getPolicyMetadata("success", "1.0", "step2");
        assertEquals("appName", resp.getDomain());
        assertEquals("success", resp.getPolicyName());
        assertEquals("1.0", resp.getPolicyVersion());
        assertEquals(mapper.convertValue(schema.getSchemaNode(), new TypeReference<Map<String, Object>>() {
        }), resp.getPolicyJsonSchema());
    }

    @Test
    void testGetPolicyMetadataNotFound() {
        when(schemaManager.getEntry(anyString())).thenThrow(new NotFoundException("test exception"));
        assertThrows(NotFoundException.class, () -> policyService.getPolicyMetadata("success", "1.0", null));
    }

    @Test
    void testGetPolicyMetadataInvalid() {
        when(schemaManager.getEntry(anyString())).thenThrow(new JsonSchemaException("test exception"));
        assertThrows(JsonSchemaException.class, () -> policyService.getPolicyMetadata("success", "1.0", null));
    }

    /**
     * Asserts that the response is an error and contains the expected values.
     *
     * @param resp    the response to check
     * @param status  the expected status
     * @param id      the expected error id
     * @param text    the expected error text
     * @param devText the expected error developer text
     * @return always returns <code>true</code> because SonarQube does not like
     * test methods that do not contain an assertion
     */
    private boolean validateError(PolicyResponse resp, PolicyStatus status, String id, String text, String devText) {
        assertEquals(status, resp.getPolicyStatus());
        assertNull(resp.getResults());

        ErrorInfo info = resp.getErrorInfo();
        assertEquals(id, info.getId());
        assertEquals(text, info.getText());
        assertEquals(devText, info.getDeveloperText());
        return true;
    }

    /**
     * Returns the expected policy result data for a successful policy
     * test. This allows us to validate that the exchange message was
     * built properly by the tested class on entry to the policy.
     *
     * @param request the policy request
     * @return the expected policy result on success (based on the success_1.0
     * policy definition in this test file)
     */
    private Map<String, Object> expectedResult(PolicyRequest request, String policyRouteName) {
        Map<String, Object> dxHeaders = new TreeMap<>();
        dxHeaders.put(ApplicationConstants.API_KEY, ctx.getApiKey());
        dxHeaders.put(ApplicationConstants.CHANNEL_TYPE, ctx.getChannelType());
        dxHeaders.put(ApplicationConstants.CLIENT_API_KEY,
                ctx.getClientApiKey() != null ? ctx.getClientApiKey() : ctx.getApiKey());
        dxHeaders.put(ApplicationConstants.CLIENT_CORRELATION_ID, ctx.getClientCorrelationId());
        dxHeaders.put(ApplicationConstants.CUSTOMER_IP_ADDR, ctx.getCustomerIpAddress());
        if (ctx.getCountryCode() != null) {
            dxHeaders.put(ApplicationConstants.COUNTRY_CODE, ctx.getCountryCode());
        }
        dxHeaders.put(ApplicationConstants.X_JOURNEY_POLICY, request.getXJourneyPolicy());
        dxHeaders.put((ApplicationConstants.SESSION_CORRELATION_ID), ctx.getSessionCorrelationId());
        dxHeaders.put("Business-Event", request.getBusinessEvent());

        Map<String, Object> headers = new TreeMap<>();
        headers.put(PolicyConstants.HEADER_POLICYROUTENAME, policyRouteName);
        headers.put("auditContext", new NullRequestContext());
        headers.put("ruleResults", null);
        headers.put("dxHeaders", dxHeaders);

        Map<String, Object> map = new TreeMap<>();
        map.put("parameters", request.getRequestBody());
        map.put("headers", headers);

        return map;
    }

    /**
     * Sets the target log level and returns the old log level so it
     * can be reset at the end of the tests.
     *
     * @param level the new log level to set
     * @return the old log level
     */
    private Level setTargetLogLevel(Level level) {
        LoggerContext context = ((LoggerContext) LogManager.getContext(false));
        LoggerConfig config = context.getConfiguration().getLoggerConfig(DevExchangeDAOImpl.class.getName());
        Level oldLevel = config.getLevel();
        config.setLevel(level);
        context.updateLoggers();
        return oldLevel;
    }

    @Test
    void testSuppressDeveloperText() {
        apiRequest = new APIRequest(API_KEY, null, CCID, CUSTOMER_IP, null, CHANNEL_TYPE,
                COUNTRY_CODE, null, null, null, null, null, "clientId", "businessEvent", "xJourneyPolicy", "customerIdToken", "sessionCorrelationId",
                "testSuppressDeveloperText", "1.0", "uuid", null, null);
        policyRequest = new PolicyRequest(apiRequest, PolicyRequestType.CREATE, policyBody, systemParams);

        PolicyService.Result result = policyExecutionHelper.executePolicy(policyRequest).getResult();
        PolicyResponse resp = result.getPolicyResponse();

        assertNull(resp.getSuppressedErrorInfo().getDeveloperText());
        assertEquals("Downstream API error", resp.getSuppressedErrorInfo().getText());
        assertEquals("202020", resp.getSuppressedErrorInfo().getId());
    }

    @Test
    void testSuppressDeveloperTextAtHostLevel() {
        ReflectionTestUtils.setField(policyExecutionHelper, "isSuppressDeveloperText", true);

        apiRequest = new APIRequest(API_KEY, null, CCID, CUSTOMER_IP, null, CHANNEL_TYPE,
                COUNTRY_CODE, null, null, null, null, null, "clientId", "businessEvent", "xJourneyPolicy", "customerIdToken", "sessionCorrelationId",
                "testSuppressDeveloperTextHostLevel", "1.0", "uuid", null, null);
        policyRequest = new PolicyRequest(apiRequest, PolicyRequestType.CREATE, policyBody, systemParams);

        PolicyService.Result result = policyExecutionHelper.executePolicy(policyRequest).getResult();
        PolicyResponse resp = result.getPolicyResponse();

        assertNull(resp.getSuppressedErrorInfo().getDeveloperText());
        assertEquals("Downstream API error", resp.getSuppressedErrorInfo().getText());
        assertEquals("202020", resp.getSuppressedErrorInfo().getId());
    }

    @Test
    void testSuppressDeveloperTextCreateProcess() {

        apiRequest = new APIRequest(API_KEY, null, CCID, CUSTOMER_IP, null, CHANNEL_TYPE,
                COUNTRY_CODE, null, null, null, null, null, "clientId", "businessEvent", "xJourneyPolicy", "customerIdToken", "sessionCorrelationId", "testSuppressDeveloperText",
                "1.0", "uuid", null, null);

        APIResponse.APISuccessResponse resp = (APIResponse.APISuccessResponse) policyService.createProcess(apiRequest, policyBody);

        assertNull(resp.getErrorInfo().getDeveloperText());
        assertEquals("Downstream API error", resp.getErrorInfo().getText());
        assertEquals("202020", resp.getErrorInfo().getId());
    }

    @Test
    void testSuppressDeveloperTextAtHostLevelCreateProcess() {

        ReflectionTestUtils.setField(policyExecutionHelper, "isSuppressDeveloperText", true);

        apiRequest = new APIRequest(API_KEY, null, CCID, CUSTOMER_IP, null, CHANNEL_TYPE,
                COUNTRY_CODE, null, null, null, null, null, "clientId", "businessEvent", "xJourneyPolicy", "customerIdToken", "sessionCorrelationId",
                "testSuppressDeveloperTextHostLevel", "1.0", "uuid", null, null);

        APIResponse.APISuccessResponse resp = (APIResponse.APISuccessResponse) policyService.createProcess(apiRequest, policyBody);

        assertNull(resp.getErrorInfo().getDeveloperText());
        assertEquals("Downstream API error", resp.getErrorInfo().getText());
        assertEquals("202020", resp.getErrorInfo().getId());
    }

    @Test
    void testSuppressDeveloperTextResumeProcess() {
        PolicyState policyState = new PolicyState();
        policyState.setPolicyName("testSuppressDeveloperText");
        policyState.setPolicyVersion("1.0");
        Set<String> availableNextSteps = new HashSet<>();
        availableNextSteps.add("step2");
        policyState.setAvailableNextSteps(availableNextSteps);
        List<String> stepsCompleted = new ArrayList<>();
        stepsCompleted.add("start");
        policyState.setStepsCompleted(stepsCompleted);

        when(policyStateCacheService.retrieveFromCache(anyString())).thenReturn(policyState);

        apiRequest = new APIRequest(API_KEY, null, CCID, CUSTOMER_IP, null, CHANNEL_TYPE,
                COUNTRY_CODE, null, null, null, null, null, "clientId", "businessEvent", "xJourneyPolicy", "customerIdToken", "sessionCorrelationId", null,
                null, "uuid", "step2", null);

        APIResponse.APISuccessResponse resp = (APIResponse.APISuccessResponse) policyService.resumeProcess(apiRequest, policyBody);

        assertNull(resp.getErrorInfo().getDeveloperText());
        assertEquals("Downstream API error", resp.getErrorInfo().getText());
        assertEquals("202020", resp.getErrorInfo().getId());
    }

    @Test
    void testSuppressDeveloperTextAtHostLevelResumeProcess() {
        PolicyState policyState = new PolicyState();
        policyState.setPolicyName("testSuppressDeveloperTextHostLevel");
        policyState.setPolicyVersion("1.0");
        Set<String> availableNextSteps = new HashSet<>();
        availableNextSteps.add("step2");
        policyState.setAvailableNextSteps(availableNextSteps);
        List<String> stepsCompleted = new ArrayList<>();
        stepsCompleted.add("start");
        policyState.setStepsCompleted(stepsCompleted);

        when(policyStateCacheService.retrieveFromCache(anyString())).thenReturn(policyState);

        ReflectionTestUtils.setField(policyExecutionHelper, "isSuppressDeveloperText", true);

        apiRequest = new APIRequest(API_KEY, null, CCID, CUSTOMER_IP, null, CHANNEL_TYPE,
                COUNTRY_CODE, null, null, null, null, null, "clientId", "businessEvent", "xJourneyPolicy", "sessionCorrelationId", "customerIdToken", null,
                null, "uuid", null, null);

        APIResponse.APISuccessResponse resp = (APIResponse.APISuccessResponse) policyService.resumeProcess(apiRequest, policyBody);

        assertNull(resp.getErrorInfo().getDeveloperText());
        assertEquals("Downstream API error", resp.getErrorInfo().getText());
        assertEquals("202020", resp.getErrorInfo().getId());
    }

    @Test
    void testJourneyPolicyHeaderIsNotNull() {
        apiRequest = new APIRequest(API_KEY, null, CCID, CUSTOMER_IP, null, CHANNEL_TYPE,
                COUNTRY_CODE, null, null, null, null, null, "clientId", "businessEvent", null, "sessionCorrelationId", "customerIdToken", "success",
                "1.0", "uuid", null, null);
        systemParams = new PolicyRequest.SystemParams(fieldsToFilter, "masterbuilder-2.0", null);
        policyRequest = new PolicyRequest(apiRequest, PolicyRequestType.CREATE, policyBody, systemParams);

        assertEquals("success:1.0:uuid", policyRequest.getXJourneyPolicy());
    }

    @SneakyThrows
    @Test
    public void testCustomerIdTokenIsAvailableInExecutionContext() {
        when(jwsService.validate(anyString(), any(), anyBoolean())).thenReturn(new JwsServiceValidationResponse());
        apiRequest = new APIRequest(API_KEY, null, CCID, CUSTOMER_IP, null, CHANNEL_TYPE,
                COUNTRY_CODE, null, null, null, null, null, "clientId", "businessEvent", "xJourneyPolicy", "customerIdToken", "sessionCorrelationId", "customerIdToken",
                "1.0", "uuid", null, null);

        systemParams = new PolicyRequest.SystemParams(fieldsToFilter, "test", null);
        policyRequest = new PolicyRequest(apiRequest, PolicyRequestType.CREATE, policyBody, systemParams);

        PolicyService.ExecutedPolicyResponse executedPolicyResponse = policyExecutionHelper.executePolicy(policyRequest);
        assertEquals("SUCCESS", executedPolicyResponse.getResult().getPolicyResponse().getPolicyStatus().toString());
        assertEquals("it is all good man", executedPolicyResponse.getResult().getPolicyResponse().getResults());
    }

    @SneakyThrows
    @Test
    public void testCustomerIdTokenIsAvailableInExecutionContextWithNoJwtTokensSet() {
        when(jwsService.validate(anyString(), any(), anyBoolean())).thenReturn(new JwsServiceValidationResponse());
        apiRequest = new APIRequest(API_KEY, null, CCID, CUSTOMER_IP, null, CHANNEL_TYPE,
                COUNTRY_CODE, null, null, null, null, null, "clientId", "businessEvent", "xJourneyPolicy", "customerIdToken", "sessionCorrelationId", "customerIdToken",
                "1.0", "uuid", null, null);

        systemParams = new PolicyRequest.SystemParams(fieldsToFilter, "test", null);
        policyRequest = new PolicyRequest(apiRequest, PolicyRequestType.CREATE, policyBody, systemParams);

        PolicyService.ExecutedPolicyResponse executedPolicyResponse = policyExecutionHelper.executePolicy(policyRequest);
        assertEquals("SUCCESS", executedPolicyResponse.getResult().getPolicyResponse().getPolicyStatus().toString());
        assertEquals("it is all good man", executedPolicyResponse.getResult().getPolicyResponse().getResults());
    }

    @Configuration
    public static class ContextConfig extends XmlDslCamelTestContextConfiguration {


        @Bean
        PolicyVersionService getVersionForwardingService() {
            PolicyVersionService service = new PolicyVersionService(true);
            service.set(MockPolicyVersion.create("a/b/success/1.0"), EntityActivationStatus.AVAILABLE);
            service.set(MockPolicyVersion.create("a/b/missing/1.0"), EntityActivationStatus.AVAILABLE);
            service.set(MockPolicyVersion.create("a/b/throwDevExchangeException/1.0"), EntityActivationStatus.AVAILABLE);
            service.set(MockPolicyVersion.create("a/b/testSuppressDeveloperText/1.0"), EntityActivationStatus.AVAILABLE);
            service.set(MockPolicyVersion.create("a/b/testSuppressDeveloperTextHostLevel/1.0"), EntityActivationStatus.AVAILABLE);
            service.set(MockPolicyVersion.create("a/b/throwChassisBusinessException/1.0"), EntityActivationStatus.AVAILABLE);
            service.set(MockPolicyVersion.create("a/b/throwReadMissPolicyCacheException/1.0"), EntityActivationStatus.AVAILABLE);
            service.set(MockPolicyVersion.create("a/b/throwIllegalStateException/1.0"), EntityActivationStatus.AVAILABLE);
            service.set(MockPolicyVersion.create("a/b/throwUnsupportedBusinessEventException/1.0"), EntityActivationStatus.AVAILABLE);
            service.set(MockPolicyVersion.create("a/b/customerIdToken/1.0"), EntityActivationStatus.AVAILABLE);
            service.set(MockPolicyVersion.create("a/b/testMockModeConfigured/1.0"), EntityActivationStatus.AVAILABLE);
            return service;
        }

        /**
         * Define the test routes that provide the various behaviors under test.
         */
        @Override
        public RouteBuilder route() {
            return new RouteBuilder() {
                @Override
                public void configure() {
                    from("policy:success_1.0.0-step2")
                            .setHeader(PolicyConstants.HEADER_AVAILABLENEXTSTEPS)
                            .groovy("[\"step2\"]")
                            .process(exchange -> {
                                Optional.ofNullable(exchange.getMessage().getHeader("cacheValue"))
                                        .ifPresent(cachedMap -> assertTrue(cachedMap instanceof Map));
                                Optional.ofNullable(exchange.getMessage().getHeader("policyState"))
                                        .ifPresent(policyState -> assertTrue(policyState instanceof PolicyState));
                            })
                            .to("policy:success_1.0.0");
                    from("policy:success_1.0.0")
                            .setBody().groovy("["
                                    + "parameters: body,"
                                    + "headers: ["
                                    + "dxHeaders: headers.dxHeaders,"
                                    + PolicyConstants.HEADER_POLICYROUTENAME + ": headers." + PolicyConstants.HEADER_POLICYROUTENAME + ","
                                    + "auditContext: headers.auditContext,"
                                    + "ruleResults: headers.ruleResults"
                                    + "]"
                                    + "]");
                    from("policy:throwDevExchangeException_1.0.0")
                            .throwException(new DevExchangeException(
                                    new DevExchangeRequest(),
                                    new DevExchangeResponse()));
                    from("policy:testSuppressDeveloperText_1.0.0")
                            .setHeader(PolicyConstants.HEADER_POLICYAUTHOR_SUPPRESS_DEVTEXT).simple("true")
                            .throwException(new DevExchangeException(
                                    new DevExchangeRequest(),
                                    new DevExchangeResponse()));
                    from("policy:testSuppressDeveloperText_1.0.0-step2")
                            .setHeader(PolicyConstants.HEADER_POLICYAUTHOR_SUPPRESS_DEVTEXT).simple("true")
                            .throwException(new DevExchangeException(
                                    new DevExchangeRequest(),
                                    new DevExchangeResponse()));
                    from("policy:testSuppressDeveloperTextHostLevel_1.0.0")
                            .setHeader(PolicyConstants.HEADER_POLICYAUTHOR_SUPPRESS_DEVTEXT).simple("false")
                            .throwException(new DevExchangeException(
                                    new DevExchangeRequest(),
                                    new DevExchangeResponse()));
                    from("policy:testSuppressDeveloperTextHostLevel_1.0.0-step2")
                            .setHeader(PolicyConstants.HEADER_POLICYAUTHOR_SUPPRESS_DEVTEXT).simple("false")
                            .throwException(new DevExchangeException(
                                    new DevExchangeRequest(),
                                    new DevExchangeResponse()));
                    from("policy:throwChassisBusinessException_1.0.0")
                            .throwException(new ChassisBusinessException("test exception"));
                    from("policy:throwReadMissPolicyCacheException_1.0.0")
                            .throwException(PolicyCacheException.newReadMissException("testId"));
                    from("policy:throwIllegalStateException_1.0.0")
                            .throwException(new IllegalStateException("test exception"));
                    from("policy:throwUnsupportedBusinessEventException_1.0.0")
                            .throwException(new UnsupportedBusinessEventException("test exception"));
                    from("policy:customerIdToken_1.0.0")
                            .choice()
                            .when(ex -> ex.getMessage().getHeader(PolicyConstants.CUSTOMER_ID_TOKEN).equals("customerIdToken"))
                            .to("direct:customerIdTokenOk")
                            .otherwise().throwException(new IllegalStateException("test exception"));
                    from("direct:customerIdTokenOk")
                            .setBody(constant("it is all good man"));
                    from("policy:testMockModeConfigured_1.0.0")
                            .process(exchange -> {
                                exchange.setProperty(MOCK_MODE_CONFIG, true);
                            })
                            .to("policy:success_1.0.0");
                }
            };
        }
    }
}