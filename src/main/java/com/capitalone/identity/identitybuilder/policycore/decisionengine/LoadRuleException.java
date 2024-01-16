package com.capitalone.identity.identitybuilder.policycore.decisionengine;

import com.capitalone.identity.identitybuilder.policycore.dmnrules.RuleDefinitionModel;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Ensure exceptions during rule loading contain the name of the rule file(s).
 */
public class LoadRuleException extends RuntimeException {

    private static String createMessage(final String runtimeName,
            final List<RuleDefinitionModel> ruleDefinitionModelList) {
        return String.format("RuntimeName: %s, Rules: %s", runtimeName,
                ruleDefinitionModelList.stream().map(RuleDefinitionModel::getRuleShortName)
                        .collect(Collectors.joining(",")));
    }

    public LoadRuleException(final String runtimeName, final List<RuleDefinitionModel> ruleDefinitionModelList,
            final Throwable original) {
        super(createMessage(runtimeName, ruleDefinitionModelList), original);
    }

    public LoadRuleException(final String runtimeName, final List<RuleDefinitionModel> ruleDefinitionModelList) {
        super(createMessage(runtimeName, ruleDefinitionModelList));
    }
}