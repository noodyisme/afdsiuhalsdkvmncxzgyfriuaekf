package com.capitalone.identity.identitybuilder.policycore.dmnrules;

import lombok.*;


/**
 * A discrete rule definition in a rudimentary form that isn't guaranteed to be in a particular format.
 *
 * @author RVR730
 */
@Value
public class RuleDefinitionModel {

    /**
     * The name of the rule file stripped from the end of the full path (e.g, rule.dmn)
     */
    @NonNull String ruleShortName;

    /**
     * Rule content as a string. Not verified to be in any particular format.
     */
    @NonNull String content;

}
