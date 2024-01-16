package com.capitalone.identity.identitybuilder.policycore.decisionengine;

import com.capitalone.identity.identitybuilder.decisionengine.adapter.api.DecisionEngineRuntimeLoadResult;
import com.capitalone.identity.identitybuilder.decisionengine.service.api.DecisionEngineService;
import com.capitalone.identity.identitybuilder.decisionengine.service.api.DecisionRuntimeLoadRequest;
import com.capitalone.identity.identitybuilder.decisionengine.service.audit.model.DecisionEngineStatus;
import com.capitalone.identity.identitybuilder.decisionengine.service.audit.model.DecisionPolicyInfo;
import com.capitalone.identity.identitybuilder.policycore.dmnrules.RuleDefinitionModel;
import com.capitalone.identity.identitybuilder.policycore.dmnrules.RuleDefinitionModelGroup;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@AllArgsConstructor
@Log4j2
@Component
public class DecisionEngineRuntimeLoadService {

    private final DecisionEngineService decisionEngineService;
    private final Map<String, String> runtimeIdToEntityIdMap = new HashMap<>();

    /**
     * Load the specified rules as a decision runtime into decision engine core library
     * @param ruleDefinitionModelGroup the rules
     */
    public void loadRulesIntoDecisionRuntime(final RuleDefinitionModelGroup ruleDefinitionModelGroup) {
        final String runtimeId = ruleDefinitionModelGroup.getRuntimeId();
        final String identifier = ruleDefinitionModelGroup.getIdentifier();
        throwIfRuntimeIdConflictDetected(runtimeId, identifier);
        final Map<String, String> dmnContentMap = new HashMap<>();
        final List<RuleDefinitionModel> rules = ruleDefinitionModelGroup.getRuleDefinitionModelList();
        rules.forEach(newRule -> dmnContentMap.put(newRule.getRuleShortName(), newRule.getContent()));
        final DecisionEngineRuntimeLoadResult loadResult;
        try {
            loadResult = decisionEngineService.load(
                    DecisionRuntimeLoadRequest.builder()
                            .runtimeId(runtimeId)
                            .dmnContentMap(dmnContentMap)
                            .decisionPolicyInfoOptional(createDecisionPolicyInfo(ruleDefinitionModelGroup))
                            .supplementalAttributes(createSupplementalAttributes(ruleDefinitionModelGroup))
                            .build());
        } catch (final RuntimeException e) {
            throw new LoadRuleException(runtimeId, rules, e);
        }
        if (loadResult.getStatus() != DecisionEngineStatus.SUCCESS) {
            throw new LoadRuleException(runtimeId, rules);
        }
    }

    public void removeDecisionRuntime(final RuleDefinitionModelGroup ruleDefinitionModelGroup) {
        decisionEngineService.removeDecisionRuntime(ruleDefinitionModelGroup.getRuntimeId());
    }

    /**
     * Validation to confirm that the runtimeId is globally unique even between different entity identifiers.
     * Raise exception to caller if a conflicting runtime id is detected from another entity identifier.
     * @param runtimeId the runtimeId
     * @param identifier the identifier
     */
    private void throwIfRuntimeIdConflictDetected(final String runtimeId, final String identifier) {
        final String existingIdentifier = runtimeIdToEntityIdMap.get(runtimeId);
        if (existingIdentifier != null && !existingIdentifier.equals(identifier)) {
            throw new IllegalStateException(String.format("Conflicting decision engine runtime id, %s, detected "
                    + "between identifier %s and %s. The policy name and version must be unique among all policies. "
                    + "Please assign unique names to resolve.", runtimeId, existingIdentifier, identifier));
        }
        runtimeIdToEntityIdMap.put(runtimeId, identifier);
    }

    private Map<String, String> createSupplementalAttributes(final RuleDefinitionModelGroup ruleDefinitionModelGroup) {
        final String identifier = ruleDefinitionModelGroup.getIdentifier();
        final Map<String, String> retVal = new HashMap<>();

        // Add entity identifier
        retVal.put("identifier", identifier);

        return retVal;
    }

    private Optional<DecisionPolicyInfo> createDecisionPolicyInfo(final RuleDefinitionModelGroup ruleDefinitionModelGroup) {
        final String identifier = ruleDefinitionModelGroup.getIdentifier();
        final String runtimeId = ruleDefinitionModelGroup.getRuntimeId();
        final String[] runtimeIdSplit = StringUtils.split(runtimeId, "/");
        if (runtimeIdSplit.length != 2) {
            log.error("Unexpected decision runtimeId format detected. Entity Identifier: {}, Runtime id: {}",
                    identifier, runtimeId);
            return Optional.empty();
        }
        return Optional.of(DecisionPolicyInfo.builder()
                .policyName(runtimeIdSplit[0])
                .policyVersion(runtimeIdSplit[1])
                .build());
    }
}
