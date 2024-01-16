package com.capitalone.identity.identitybuilder.policycore.dmnrules;

import lombok.NonNull;
import lombok.Value;

import java.util.List;

/**
 * A grouping of one or more rules that may be leveraged as part of a policy
 */
@Value
public class RuleDefinitionModelGroup {

    /**
     * The full path to these rules' policy in configuration store
     */
    @NonNull String identifier;

    /**
     * The identifier used to lookup these rules at execution time
     */
    @NonNull String runtimeId;

    /**
     * The list of rules
     */
    @NonNull List<RuleDefinitionModel> ruleDefinitionModelList;
}
