package com.capitalone.identity.identitybuilder.policycore.decisionengine;

import com.capitalone.identity.identitybuilder.decisionengine.adapter.api.DecisionEngineRuntimeLoadResult;
import com.capitalone.identity.identitybuilder.decisionengine.service.api.DecisionEngineService;
import com.capitalone.identity.identitybuilder.decisionengine.service.api.DecisionRuntimeLoadRequest;
import com.capitalone.identity.identitybuilder.decisionengine.service.audit.model.DecisionEngineStatus;
import com.capitalone.identity.identitybuilder.decisionengine.service.audit.model.DecisionPolicyInfo;
import com.capitalone.identity.identitybuilder.policycore.dmnrules.RuleDefinitionModel;
import com.capitalone.identity.identitybuilder.policycore.dmnrules.RuleDefinitionModelGroup;
import com.google.common.collect.Lists;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@SpringBootTest(properties = {"spring.main.allow-bean-definition-overriding=true"})
class DecisionEngineRuntimeLoadServiceTest {

    private static final List<RuleDefinitionModel> defaultRuleDefinitionList = Lists.newArrayList(
            new RuleDefinitionModel("myRuleA.dmn", "dummyContentA"),
            new RuleDefinitionModel("myRuleB.dmn", "dummyContentB"));

    private static final String defaultPolicyName = "myPolicy";
    private static final String defaultPolicyVersion = "5.2";

    private static final RuleDefinitionModelGroup defaultRuleGroup = new RuleDefinitionModelGroup(
            "/lob/x/y/" + defaultPolicyName + "/" + defaultPolicyVersion,
            defaultPolicyName + "/" + defaultPolicyVersion, defaultRuleDefinitionList);

    private static final RuleDefinitionModelGroup invalidRuntimeIdFormatRuleGroup = new RuleDefinitionModelGroup(
            "/lob/x/y/" + defaultPolicyName, defaultPolicyName, defaultRuleDefinitionList);

    private static final DecisionEngineRuntimeLoadResult loadResultSuccess = createLoadResultStub(DecisionEngineStatus.SUCCESS);

    private static final DecisionEngineRuntimeLoadResult loadResultFailure = createLoadResultStub(DecisionEngineStatus.FAILURE);

    private static DecisionEngineRuntimeLoadResult createLoadResultStub(final DecisionEngineStatus status) {
        final DecisionEngineRuntimeLoadResult resultStub = Mockito.mock(DecisionEngineRuntimeLoadResult.class);
        Mockito.when(resultStub.getStatus()).thenReturn(status);
        return resultStub;
    }

    private ArgumentCaptor<DecisionRuntimeLoadRequest> loadResultArgumentCaptor;

    @MockBean
    private DecisionEngineService decisionEngineService;

    // Object under test
    @Autowired
    private DecisionEngineRuntimeLoadService decisionEngineRuntimeLoadService;

    @BeforeEach
    void setup() {
        // Clear all runtimes
        decisionEngineService.getLoadedRuntimesSnapshot().values().forEach(
                runtimeInfo -> decisionEngineService.removeDecisionRuntime(runtimeInfo.getRuntimeId()));
        loadResultArgumentCaptor = ArgumentCaptor.forClass(DecisionRuntimeLoadRequest.class);
    }

    @Test
    void decisionRuntimeLoadRuntimeExceptionFailure() {
        // Arrange
        Mockito.when(decisionEngineService.load(Mockito.any()))
                .thenThrow(new RuntimeException("Test-only error."));

        // Act & Assert
        Assertions.assertThrows(LoadRuleException.class,
                () -> decisionEngineRuntimeLoadService.loadRulesIntoDecisionRuntime(defaultRuleGroup));
    }

    @Test
    void decisionRuntimeLoadResponseFailure() {
        // Arrange
        Mockito.when(decisionEngineService.load(Mockito.any())).thenReturn(loadResultFailure);

        // Act & Assert
        Assertions.assertThrows(LoadRuleException.class,
                () -> decisionEngineRuntimeLoadService.loadRulesIntoDecisionRuntime(defaultRuleGroup));
    }

    @Test
    void decisionRuntimeLoadHappyPathSuccess() {
        // Arrange
        Mockito.when(decisionEngineService.load(Mockito.any())).thenReturn(loadResultSuccess);

        // Act
        decisionEngineRuntimeLoadService.loadRulesIntoDecisionRuntime(defaultRuleGroup);

        // Assert
        Mockito.verify(decisionEngineService).load(loadResultArgumentCaptor.capture());
        final DecisionRuntimeLoadRequest actualRequest = loadResultArgumentCaptor.getValue();
        final DecisionPolicyInfo decisionPolicyInfo = actualRequest.getDecisionPolicyInfoOptional().orElse(null);
        Assertions.assertEquals(defaultRuleGroup.getRuntimeId(), actualRequest.getRuntimeId());
        Assertions.assertEquals(defaultRuleGroup.getRuleDefinitionModelList().size(), actualRequest.getDmnContentMap().size());
        Assertions.assertEquals(defaultRuleGroup.getIdentifier(), actualRequest.getSupplementalAttributes().get("identifier"));
        Assertions.assertNotNull(decisionPolicyInfo);
        Assertions.assertEquals(defaultPolicyName, decisionPolicyInfo.getPolicyName());
        Assertions.assertEquals(defaultPolicyVersion, decisionPolicyInfo.getPolicyVersion());
    }

    @Test
    void decisionRuntimeLoadMissingPolicyInfoSuccess() {
        // Arrange
        Mockito.when(decisionEngineService.load(Mockito.any())).thenReturn(loadResultSuccess);

        // Act
        decisionEngineRuntimeLoadService.loadRulesIntoDecisionRuntime(invalidRuntimeIdFormatRuleGroup);

        // Assert
        Mockito.verify(decisionEngineService).load(loadResultArgumentCaptor.capture());
        final DecisionRuntimeLoadRequest actualRequest = loadResultArgumentCaptor.getValue();
        Assertions.assertEquals(invalidRuntimeIdFormatRuleGroup.getRuntimeId(), actualRequest.getRuntimeId());
        Assertions.assertEquals(invalidRuntimeIdFormatRuleGroup.getRuleDefinitionModelList().size(), actualRequest.getDmnContentMap().size());
        Assertions.assertEquals(invalidRuntimeIdFormatRuleGroup.getIdentifier(), actualRequest.getSupplementalAttributes().get("identifier"));
        Assertions.assertFalse(actualRequest.getDecisionPolicyInfoOptional().isPresent());
    }

    @Test
    void removeDecisionRuntimeSuccess() {
        // Act
        decisionEngineRuntimeLoadService.removeDecisionRuntime(defaultRuleGroup);

        // Assert
        Mockito.verify(decisionEngineService).removeDecisionRuntime(defaultRuleGroup.getRuntimeId());
    }

    @Test
    void decisionRuntimeLoadRuntimeIdConflictFailure() {
        // Arrange
        final RuleDefinitionModelGroup conflictingRuleGroup = new RuleDefinitionModelGroup(
                "/different_lob/y/z/" + defaultPolicyName + "/" + defaultPolicyVersion,
                defaultPolicyName + "/" + defaultPolicyVersion, defaultRuleDefinitionList);
        Mockito.when(decisionEngineService.load(Mockito.any())).thenReturn(loadResultSuccess);

        // Act & Assert
        decisionEngineRuntimeLoadService.loadRulesIntoDecisionRuntime(defaultRuleGroup);
        Assertions.assertThrows(IllegalStateException.class,
                () -> decisionEngineRuntimeLoadService.loadRulesIntoDecisionRuntime(conflictingRuleGroup));
        Mockito.verify(decisionEngineService, Mockito.times(1)).load(Mockito.any());
    }

    @Configuration
    @ComponentScan (basePackages = { "com.capitalone.identity.identitybuilder.policycore.decisionengine" })
    static class TestConfig {

        @Bean
        public DecisionEngineRuntimeLoadService decisionEngineRuntimeLoadService(
                final DecisionEngineService decisionEngineService) {
            return new DecisionEngineRuntimeLoadService(decisionEngineService);
        }
    }

}