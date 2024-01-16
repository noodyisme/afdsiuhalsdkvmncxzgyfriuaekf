package com.capitalone.identity.identitybuilder.policycore.service.logging;

import com.capitalone.chassis.engine.model.error.ErrorResponse;
import com.capitalone.chassis.engine.model.exception.ChassisBusinessException;
import com.capitalone.chassis.engine.model.exception.ChassisSystemException;
import com.capitalone.chassis.engine.model.exception.NotFoundException;
import com.capitalone.identity.identitybuilder.model.EntityActivationStatus;
import com.capitalone.identity.identitybuilder.policycore.abac.model.PolicyAuthAction;
import com.capitalone.identity.identitybuilder.policycore.abac.model.PolicyAuthResource;
import com.capitalone.identity.identitybuilder.policycore.abac.model.PolicyAuthSubject;
import com.capitalone.identity.identitybuilder.policycore.abac.service.AuthorizationEnforcer;
import com.capitalone.identity.identitybuilder.policycore.camel.PolicyConstants;
import com.capitalone.identity.identitybuilder.policycore.camel.components.devexchange.DevExchangeException;
import com.capitalone.identity.identitybuilder.policycore.camel.util.CamelCacheUtil;
import com.capitalone.identity.identitybuilder.policycore.model.*;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.PolicyEvaluatedAuditor;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.model.policy.PolicyRequestType;
import com.capitalone.identity.identitybuilder.policycore.rest.v1.ExecutionContext;
import com.capitalone.identity.identitybuilder.policycore.schema.SchemaManager;
import com.capitalone.identity.identitybuilder.policycore.service.PolicyService;
import com.capitalone.identity.identitybuilder.policycore.service.PolicyStateCacheService;
import com.capitalone.identity.identitybuilder.policycore.service.jws.JwsService;
import com.capitalone.identity.identitybuilder.policycore.service.jws.JwsServiceValidationResponse;
import com.capitalone.identity.identitybuilder.policycore.service.util.MockPolicyVersion;
import com.capitalone.identity.identitybuilder.policycore.service.versions.PolicyVersionService;
import com.capitalone.identity.identitybuilder.policycore.utils.CamelSpringBootContextAwareTest;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.ModelCamelContext;
import org.apache.camel.model.dataformat.JsonDataFormat;
import org.apache.camel.spring.SpringCamelContext;
import org.apache.camel.support.DefaultExchange;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.core.convert.ConversionService;
import org.springframework.test.annotation.DirtiesContext;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@CamelSpringBootContextAwareTest(
        contextConfigClasses = AuditLoggerTest.ContextConfig.class,
        excludeFromComponentScan = @ComponentScan.Filter(type = FilterType.ASPECTJ,
                pattern = "com.capitalone.identity.identitybuilder.policycore.service"))
@DirtiesContext
@ExtendWith(MockitoExtension.class)
public class AuditLoggerTest {

    @Mock
    private SchemaManager schemaManager;
    @Mock
    private PolicyStateCacheService policyStateCacheService;
    @Mock
    private ConversionService conversionService;
    @Mock
    private CamelCacheUtil camelCacheUtil;
    @Mock
    private AuthorizationEnforcer<PolicyAuthSubject, PolicyAuthResource, PolicyAuthAction> authorizationEnforcer;
    @Mock(lenient = true)
    private JwsService jwsService;
    @Autowired
    private CamelContext camelContext;

    @Autowired
    private PolicyVersionService policyVersionService;

    private AuditLogger auditLogger;

    private ExecutionContext ctx;
    private PolicyRequest policyRequest;
    private APIRequest apiRequest;
    private Exchange exchange;
    private static final String APP_NAME = "appName";
    private static final String API_KEY = "apiKey";
    private static final String BUSINESS_EVENT = "businessEvent";
    private static final String CHANNEL_TYPE = "channelType";
    private static final String CCID = "clientCorrelationId";
    private static final String COUNTRY_CODE = "countryCode";
    private static final String CUSTOMER_IP = "customerIp";
    private PolicyRequest.SystemParams systemParams;

    // Sub-classes of PolicyService
    private PolicyServiceExecutor policyServiceExecutor;
    private PolicyService.ProcessMetadataHelper processMetadataHelper;
    private PolicyService.PolicyExecutionHelper policyExecutionHelper;

    @BeforeEach
    public void setup() throws Exception {
        ctx = new ExecutionContext();
        ctx.setBusinessEvent(BUSINESS_EVENT);
        ctx.setApiKey(API_KEY);
        ctx.setChannelType(CHANNEL_TYPE);
        ctx.setClientCorrelationId(CCID);
        ctx.setCountryCode(COUNTRY_CODE);
        ctx.setCustomerIpAddress(CUSTOMER_IP);
        auditLogger = Mockito.spy(new AuditLogger());
        createDefaultPolicyService();
        HashMap<String, Serializable> policyBody = new HashMap<>();
        policyBody.put("foo", "bar");
        exchange = new DefaultExchange(camelContext);
        apiRequest = new APIRequest(API_KEY, null, CCID, CUSTOMER_IP, null, CHANNEL_TYPE,
                COUNTRY_CODE, null, null, null, null, null, "clientId", "businessEvent", "xJourneyPolicy", "customerIdToken", "sessionCorrelationId", "success",
                "1.0", "uuid", null, null);
        systemParams = new PolicyRequest.SystemParams("none", "test", null);
        policyRequest = new PolicyRequest(apiRequest, PolicyRequestType.CREATE, policyBody, systemParams);

        when(jwsService.validate(anyString(),any(),anyBoolean())).thenReturn(new JwsServiceValidationResponse());
    }

    private void createDefaultPolicyService() {
        processMetadataHelper = new PolicyService.ProcessMetadataHelper(policyStateCacheService);
        policyExecutionHelper = Mockito.spy(new PolicyService.PolicyExecutionHelper(false, schemaManager, camelContext,
                processMetadataHelper, camelCacheUtil, authorizationEnforcer, true, policyVersionService, false, ""));
        policyServiceExecutor = new PolicyServiceExecutor(auditLogger, Mockito.mock(PolicyEvaluatedAuditor.class),
                conversionService, policyExecutionHelper);
    }

    @Test
    public void testExecutePolicyAuditLogs() {
        System.out.println("test");
        PolicyService.ExecutedPolicyResponse executeResponse = policyServiceExecutor.executePolicyWithAuditing(
                () -> policyRequest, PolicyAuditContext.of(apiRequest, PolicyRequestType.CREATE));
        PolicyResponse resp = executeResponse.getResult().getPolicyResponse();
        assertEquals(PolicyStatus.SUCCESS, resp.getPolicyStatus());
        assertNull(resp.getErrorInfo());
        assertNull(resp.getMetadata());
        Mockito.verify(auditLogger).logAudits(policyRequest, policyRequest.getPolicyName(),
                executeResponse.getExecutedPolicyVersion(), null, executeResponse, null);
        ExecutePolicyAuditResponse auditResponse = auditLogger.logAudits(policyRequest, policyRequest.getPolicyName(),
                executeResponse.getExecutedPolicyVersion(), null, executeResponse, null);
        assertEquals("{\"foo\":\"bar\"}", policyRequest.getPolicyParametersAudit());
        assert (auditResponse.getResultsAudit().contains("{\"foo\":\"bar\"}"));
    }


    @Test
    public void testExecutePolicyWithNullParametersAuditLogs() {
        policyRequest = new PolicyRequest(apiRequest, PolicyRequestType.CREATE, null, systemParams);
        PolicyService.ExecutedPolicyResponse executeResponse = policyServiceExecutor.executePolicyWithAuditing(
                () -> policyRequest, PolicyAuditContext.of(apiRequest, PolicyRequestType.CREATE));
        Mockito.verify(auditLogger).logAudits(policyRequest, policyRequest.getPolicyName(),
                executeResponse.getExecutedPolicyVersion(), null, executeResponse, null);
        ExecutePolicyAuditResponse auditResponse = auditLogger.logAudits(policyRequest, policyRequest.getPolicyName(),
                executeResponse.getExecutedPolicyVersion(), null, executeResponse, null);
        assertEquals("", policyRequest.getPolicyParametersAudit());
        assert (auditResponse.getResultsAudit().contains("\"parameters\":null"));
    }

    @Test
    public void testExecutePolicyFilteredRequestAndResponseAuditLogs() {
        auditLogger.setFieldsToFilter("foo");
        systemParams = new PolicyRequest.SystemParams(auditLogger.getFieldsToFilter(), "test", null);
        HashMap<String, Serializable> policyBody = new HashMap<>();
        policyBody.put("foo", "bar");
        policyRequest = new PolicyRequest(apiRequest, PolicyRequestType.CREATE, policyBody, systemParams);
        PolicyService.ExecutedPolicyResponse executeResponse = policyServiceExecutor.executePolicyWithAuditing(
                () -> policyRequest, PolicyAuditContext.of(apiRequest, PolicyRequestType.CREATE));
        Mockito.verify(auditLogger).logAudits(policyRequest, policyRequest.getPolicyName(),
                executeResponse.getExecutedPolicyVersion(), null, executeResponse, null);
        ExecutePolicyAuditResponse auditResponse = auditLogger.logAudits(policyRequest, policyRequest.getPolicyName(),
                executeResponse.getExecutedPolicyVersion(), null, executeResponse, null);
        assertEquals("{\"foo\":\"XXXXXX\"}", policyRequest.getPolicyParametersAudit());
        assert (auditResponse.getResultsAudit().contains("{\"foo\":\"XXXXXX\"}"));
    }

    @Test
    public void testExecutePolicyExceptionAuditLogs() {
        ErrorResponse errorResponse = new ErrorResponse("000000", "Test Error", "Error for testing", new ArrayList<>(), new ArrayList<>());
        PolicyService.ExecutedPolicyResponse executedPolicyResponse = PolicyService.ExecutedPolicyResponse.builder()
                .setResult(new PolicyService.Result.SystemError(new ChassisBusinessException())).build();
        ChassisErrorAuditResponse auditResponse = auditLogger.logChassisAudits(null, null,
                "dummy", "1", errorResponse, executedPolicyResponse, new ChassisSystemException("Error for testing"));
        assertEquals(errorResponse.getId(), auditResponse.getId(), "ID does not match");
        assertEquals(errorResponse.getText(), auditResponse.getText(), "Text does not match");
        assertEquals(errorResponse.getDeveloperText(), auditResponse.getDeveloperText(), "DeveloperText does not match");
        assertEquals(errorResponse.getActions(), auditResponse.getActions(), "Actions does not match");
        assertEquals(errorResponse.getErrorDetails(), auditResponse.getErrorDetails(), "ErrorDetails does not match");
    }

    @Test
    public void testStandardAuditNullResultResponse() {
        final PolicyService.ExecutedPolicyResponse executedResponseExpected =
                Mockito.mock(PolicyService.ExecutedPolicyResponse.class);
        Mockito.when(executedResponseExpected.getResult()).thenReturn(
                new PolicyService.Result.SystemError(new NotFoundException("Oops!")));

        final ExecutePolicyAuditResponse auditResponseActual =
                auditLogger.logAudits(policyRequest, policyRequest.getPolicyName(), policyRequest.getPolicyVersionRequested(),
                        "start", executedResponseExpected, null);

        Assertions.assertNull(auditResponseActual);
    }

    @Test
    public void testStandardAuditCircularReferenceResultResponse() {
        class CircularReference { public CircularReference reference; }
        final CircularReference circularReference = new CircularReference();
        circularReference.reference = circularReference;
        final PolicyService.ExecutedPolicyResponse executedResponseExpected =
                Mockito.mock(PolicyService.ExecutedPolicyResponse.class);
        Mockito.when(executedResponseExpected.getResult()).thenReturn(
               new PolicyService.Result.PolicySuccess(PolicyResponse.builder()
                       .policyStatus(PolicyStatus.FAILURE)
                       .results(circularReference).build()));

        Assertions.assertThrows(ChassisSystemException.class, () ->
                auditLogger.logAudits(policyRequest, policyRequest.getPolicyName(), policyRequest.getPolicyVersionRequested(),
                        "start", executedResponseExpected, null));
    }


    @Configuration
    public static class ContextConfig {

        @Bean
        protected CamelContext camelContext(ApplicationContext applicationContext) throws Exception {
            CamelContext camelContext = new SpringCamelContext(applicationContext);
            camelContext.adapt(ModelCamelContext.class)
                    .setDataFormats(Collections.singletonMap("jackson", new JsonDataFormat()));
            camelContext.addRoutes(route());
            camelContext.build();
            return camelContext;
        }

        @Bean
        protected ProducerTemplate producerTemplate(CamelContext camelContext) {
            return camelContext.createProducerTemplate();
        }

        @Bean
        PolicyVersionService getVersionForwardingService() {
            PolicyVersionService service = new PolicyVersionService(true);
            service.set(MockPolicyVersion.create("a/b/success/1.0"), EntityActivationStatus.AVAILABLE);
            service.set(MockPolicyVersion.create("a/b/throwDevExchangeException/1.0"), EntityActivationStatus.AVAILABLE);
            service.set(MockPolicyVersion.create("a/b/throwChassisBusinessException/1.0"), EntityActivationStatus.AVAILABLE);
            service.set(MockPolicyVersion.create("a/b/throwIllegalStateException/1.0"), EntityActivationStatus.AVAILABLE);
            return service;
        }

        /**
         * Define the test routes that provide the various behaviors under test.
         */
        public RouteBuilder route() {
            return new RouteBuilder() {
                @Override
                public void configure() {
                    from("policy:success_1.0.0-step2")
                            .setHeader(PolicyConstants.HEADER_AVAILABLENEXTSTEPS)
                            .groovy("[\"step2\"]")
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
                    from("policy:throwDevExchangeException_1.0")
                            .throwException(new DevExchangeException(
                                    new DevExchangeRequest(),
                                    new DevExchangeResponse()));
                    from("policy:throwChassisBusinessException_1.0")
                            .throwException(new ChassisBusinessException("test exception"));
                    from("policy:throwIllegalStateException_1.0")
                            .throwException(new IllegalStateException("test exception"));
                }
            };
        }
    }

}