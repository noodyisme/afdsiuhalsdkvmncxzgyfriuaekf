package com.capitalone.identity.identitybuilder.policycore.dmnrules;

import com.capitalone.identity.identitybuilder.model.ConfigStoreItem;
import com.capitalone.identity.identitybuilder.policycore.decisionengine.DecisionRegexUtil;
import lombok.NonNull;
import lombok.extern.log4j.Log4j2;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Factory to instantiate a group of rule definition models for a policy
 */
@Log4j2
public class RuleDefinitionModelGroupFactory {

    // The runtimeId is expected to match the last two or second to last two directories of the entity identifier (full path to the owning policy)
    // Must be resilient to both patch ([...]/policy_name/policy_version/[logical_version] and non-patch ([...]/policy_name/policy_version) storage mechanisms:
    // e.g. Non-Patch: -> us_consumers/card_fraud/account_lookup/1.0   => account_lookup/1.0
    //      Patch:     -> us_consumers/card_fraud/account_lookup/1.0/0 => account_lookup/1.0
    private final Pattern runtimeIdPattern = Pattern.compile("\\/?+([^\\/]+\\/+[0-9]+\\.[0-9]+\\.[0-9]+)\\/*[0-9]*\\/*$"); //NOSONAR

    // The dmn name is expected to be in the rules/ directory,
    // e.g.us_consumers/card_fraud/account_lookup/1.0/rules/file.dmn => file.dmn
    private final Pattern dmnNamePattern = Pattern.compile("\\/?+rules\\/+([^\\/]+)$"); //NOSONAR

    // Sanitization pattern to remove any double or more slashes from path
    private final Pattern sanitizePathPattern = Pattern.compile("\\/\\/+");

    /**
     * Creates the rule definition model group
     *
     * @param identifier the full path to the policy to which this rule belongs
     * @param ruleItems  a map that contains items which represent the rules key=name, value=ruleContent
     * @return the rule definition model group
     */
    public RuleDefinitionModelGroup create(final @NonNull String identifier,
                                           final @NonNull Map<String, String> ruleItems) {
        final String sanitizedIdentifier = sanitizePathPattern.matcher(identifier).replaceAll("/").trim();
        final String runtimeRuleId = DecisionRegexUtil.findFirstGroupMatch(runtimeIdPattern, sanitizedIdentifier);
        final List<RuleDefinitionModel> newRules = ruleItems.entrySet().stream()
                .map(entry -> this.createRuleDefinitionModel(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
        return new RuleDefinitionModelGroup(identifier, runtimeRuleId, newRules);
    }

    /**
     * Creates a rule definition model
     *
     * @param name    of the item
     * @param content of the rule
     * @return the rule definition model
     */
    private RuleDefinitionModel createRuleDefinitionModel(final String name, final String content) {
        final String dmnName = DecisionRegexUtil.findFirstGroupMatch(dmnNamePattern, name);
        return new RuleDefinitionModel(dmnName, content);
    }

    public static Map<String, String> convertItemsToMap(Collection<ConfigStoreItem> items) {
        return items.stream().collect(Collectors.toMap(ConfigStoreItem::getName, ConfigStoreItem::getContent));
    }
}
