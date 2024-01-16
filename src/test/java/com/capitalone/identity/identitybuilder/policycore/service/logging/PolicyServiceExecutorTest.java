package com.capitalone.identity.identitybuilder.policycore.service.logging;

import com.capitalone.chassis.engine.model.error.ErrorResponse;
import com.capitalone.chassis.engine.model.exception.ChassisBusinessException;
import com.capitalone.identity.identitybuilder.policycore.model.PolicyRequest;
import com.capitalone.identity.identitybuilder.policycore.model.PolicyResponse;
import com.capitalone.identity.identitybuilder.policycore.model.PolicyStatus;
import com.capitalone.identity.identitybuilder.policycore.model.ProcessMetadata;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.PolicyEvaluatedAuditor;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.model.policy.PolicyRequestType;
import com.capitalone.identity.identitybuilder.policycore.service.PolicyService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.ConverterNotFoundException;

import java.util.*;

import static org.mockito.ArgumentMatchers.*;

class PolicyServiceExecutorTest {

    private static final String POLICY_NAME = "test_policy";
    private static final String POLICY_VERSION = "1.0";
    private static final String POLICY_VERSION_PATCH = "1.0.0";
    private static final String STEP = "step";
    private static final String CLIENT = "client";
    private static final String PROCESS_ID = "1234";
    private static final PolicyRequestType POLICY_REQUEST_TYPE = PolicyRequestType.RESUME;
    private static final Map<String, String> DX_HEADERS = new HashMap<String, String>(){{put("testKey", "testValue");}};
    private static final String X_JOURNEY_POLICY = "xJourney_1234_y";

    private static PolicyRequest defaultPolicyRequest() {
        return PolicyRequest.builder()
                .policyName(POLICY_NAME)
                .policyVersionRequested(POLICY_VERSION)
                .policyInfo(Optional.empty())
                .step(STEP)
                .xJourneyPolicy(X_JOURNEY_POLICY)
                .clientId(CLIENT)
                .policyParametersAudit("{}")
                .processId(PROCESS_ID)
                .policyRequestType(POLICY_REQUEST_TYPE)
                .dxHeaders(DX_HEADERS)
                .build();
    }

    private static PolicyAuditContext defaultPolicyAuditContext() {
        return PolicyAuditContext.builder()
                .policyName(POLICY_NAME)
                .policyVersionRequested(POLICY_VERSION)
                .step(STEP)
                .xJourneyPolicy(X_JOURNEY_POLICY)
                .clientId(CLIENT)
                .processId(PROCESS_ID)
                .policyRequestType(POLICY_REQUEST_TYPE)
                .build();
    }

    // Mocked Dependencies
    private AuditLogger auditLogger;
    private PolicyEvaluatedAuditor policyEvaluatedAuditor;
    private ConversionService conversionService;
    private PolicyService.PolicyExecutionHelper policyExecutionHelper;

    // Inputs
    private final PolicyRequest policyRequest = defaultPolicyRequest();
    private final PolicyAuditContext policyAuditContext = defaultPolicyAuditContext();
    private final ProcessMetadata processMetadata = Mockito.mock(ProcessMetadata.class);

    // Object under test
    private PolicyServiceExecutor policyServiceExecutor;

    @BeforeEach
    void setup() {
        auditLogger = Mockito.mock(AuditLogger.class);
        policyEvaluatedAuditor = Mockito.mock(PolicyEvaluatedAuditor.class);
        conversionService = Mockito.mock(ConversionService.class);
        policyExecutionHelper = Mockito.mock(PolicyService.PolicyExecutionHelper.class);

        policyServiceExecutor = new PolicyServiceExecutor(auditLogger, policyEvaluatedAuditor,
                conversionService, policyExecutionHelper);
    }

    @Test
    void auditsSuccess() {
        // Arrange
        final PolicyService.ExecutedPolicyResponse expectedResponse =
                PolicyService.ExecutedPolicyResponse.builder()
                        .setResult(new PolicyService.Result.PolicySuccess(
                                PolicyResponse.builder()
                                        .results(new Object() {final String hello = "world!";})
                                        .policyStatus(PolicyStatus.SUCCESS)
                                        .metadata(processMetadata)
                                        .build()))
                        .setExecutedPolicyVersion(POLICY_VERSION)
                        .setExecutedPolicyVersionWithPatch(POLICY_VERSION_PATCH)
                        .build();
        Mockito.when(policyExecutionHelper.executePolicy(Mockito.any())).thenReturn(expectedResponse);

        // Act
        final PolicyService.ExecutedPolicyResponse actualResponse =
                policyServiceExecutor.executePolicyWithAuditing(() -> policyRequest, policyAuditContext);

        // Assert
        Assertions.assertSame(expectedResponse, actualResponse);
        Mockito.verify(auditLogger).logAudits(policyRequest, POLICY_NAME, POLICY_VERSION, STEP, expectedResponse, null);
        Mockito.verify(policyEvaluatedAuditor).audit(eq(policyRequest), eq(expectedResponse), eq(null), ArgumentMatchers.any(), eq(policyAuditContext));
    }

    @Test
    void auditsNullInputsFailure() {
        // Act & Assert
        Assertions.assertThrows(NullPointerException.class, () -> policyServiceExecutor.executePolicyWithAuditing(null, policyAuditContext));
        Assertions.assertThrows(NullPointerException.class, () -> policyServiceExecutor.executePolicyWithAuditing(() -> policyRequest, null));
    }

    @Test
    void auditsRequestGenerationFailure() {
        // Arrange
        final PolicyRequest expectedAuditedPolicyRequest = policyRequest.toBuilder()
                .policyParametersAudit("N/A")
                .dxHeaders(Collections.emptyMap())
                .build();

        // Act
        final IllegalArgumentException actualException = Assertions.assertThrows(IllegalArgumentException.class,
                () -> policyServiceExecutor.executePolicyWithAuditing(
                        () -> { throw new IllegalArgumentException("test"); }, policyAuditContext));

        // Assert
        Mockito.verify(policyEvaluatedAuditor).audit(eq(expectedAuditedPolicyRequest), eq(null),
                eq(actualException), any(), eq(policyAuditContext));
    }

    @Test
    void auditsChassisExceptionFailure() {
        // Arrange
        Mockito.when(policyExecutionHelper.executePolicy(any())).thenThrow(new ChassisBusinessException("test"));
        final ErrorResponse chassisErrorResponse = Mockito.mock(ErrorResponse.class);
        Mockito.when(conversionService.convert(any(), any())).thenReturn(chassisErrorResponse);

        // Act
        final ChassisBusinessException actualException = Assertions.assertThrows(ChassisBusinessException.class,
                () -> policyServiceExecutor.executePolicyWithAuditing(() -> policyRequest, policyAuditContext));

        // Assert
        Mockito.verify(auditLogger).logChassisAudits(eq(policyRequest), eq(POLICY_NAME), eq(POLICY_VERSION),
                eq(STEP), eq(chassisErrorResponse), any(), any());
        Mockito.verify(policyEvaluatedAuditor).auditChassisError(eq(policyRequest), any(),
                eq(chassisErrorResponse), any(), any(), eq(policyAuditContext));
    }

    @Test
    void auditsNonChassisExceptionFailure() {
        // Arrange
        Mockito.when(policyExecutionHelper.executePolicy(any())).thenThrow(new IllegalStateException("test"));

        // Act
        final IllegalStateException actualException = Assertions.assertThrows(IllegalStateException.class,
                () -> policyServiceExecutor.executePolicyWithAuditing(() -> policyRequest, policyAuditContext));

        // Assert
        Mockito.verify(policyEvaluatedAuditor).audit(eq(policyRequest), any(), eq(actualException),
                any(), eq(policyAuditContext));
    }

    @Test
    void auditsErrorResponseFailure() {
        // Arrange
        final ChassisBusinessException exceptionExpected = new ChassisBusinessException("test");
        final PolicyService.ExecutedPolicyResponse responseExpected =
                PolicyService.ExecutedPolicyResponse.builder()
                        .setExecutedPolicyVersion(POLICY_VERSION)
                        .setExecutedPolicyVersionWithPatch(POLICY_VERSION_PATCH)
                        .setResult(new PolicyService.Result.SystemError(exceptionExpected))
                        .build();
        Mockito.when(policyExecutionHelper.executePolicy(any())).thenReturn(responseExpected);
        final ErrorResponse chassisErrorResponse = Mockito.mock(ErrorResponse.class);
        Mockito.when(conversionService.convert(any(), any())).thenReturn(chassisErrorResponse);

        // Act
        final PolicyService.ExecutedPolicyResponse actualResponse = policyServiceExecutor.executePolicyWithAuditing(
                () -> policyRequest, policyAuditContext);

        // Assert
        Assertions.assertSame(responseExpected, actualResponse);
        Mockito.verify(auditLogger).logChassisAudits(eq(policyRequest), eq(POLICY_NAME), eq(POLICY_VERSION),
                eq(STEP), eq(chassisErrorResponse), any(), eq(exceptionExpected));
        Mockito.verify(policyEvaluatedAuditor).auditChassisError(eq(policyRequest), eq(exceptionExpected),
                eq(chassisErrorResponse), eq(responseExpected), any(), eq(policyAuditContext));
    }

    @Test
    void auditsConversionServiceException() {
        // Arrange
        Mockito.when(policyExecutionHelper.executePolicy(any())).thenThrow(new ConverterNotFoundException(null, null));

        // Act
        Assertions.assertThrows(ConverterNotFoundException.class,
                () -> policyServiceExecutor.executePolicyWithAuditing(() -> policyRequest, policyAuditContext));

        // Assert
        Mockito.verify(policyEvaluatedAuditor).audit(eq(policyRequest), any(), any(ConverterNotFoundException.class),
                any(), eq(policyAuditContext));
    }

}