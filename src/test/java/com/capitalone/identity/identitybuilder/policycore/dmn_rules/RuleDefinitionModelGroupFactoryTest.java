package com.capitalone.identity.identitybuilder.policycore.dmn_rules;


import com.capitalone.identity.identitybuilder.model.ConfigStoreItem;
import com.capitalone.identity.identitybuilder.model.ConfigStoreItemInfo;
import com.capitalone.identity.identitybuilder.policycore.dmnrules.RuleDefinitionModelGroup;
import com.capitalone.identity.identitybuilder.policycore.dmnrules.RuleDefinitionModelGroupFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RuleDefinitionModelGroupFactoryTest {

    // Object under test
    private final RuleDefinitionModelGroupFactory ruleDefinitionModelGroupFactory = new RuleDefinitionModelGroupFactory();

    @Test
    void testIllegalArgs() {
        assertThrows(NullPointerException.class,
                () -> ruleDefinitionModelGroupFactory.create(null, new HashMap<>()));
        assertThrows(NullPointerException.class,
                () -> ruleDefinitionModelGroupFactory.create("a", null));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "rules/abc",
            "/sdf/sfd/sdfs/rules/abc",
            "sdf/sfd/sdfs/rules/abc",
            "rules/abc",
            "/10/rules/abc"
    })
    void testRuntimeAndRuleNameEquivalence(String ruleSubPath) {
        String identifier = "/lob/policy_name/3.0.1/";

        RuleDefinitionModelGroup rule2 = ruleDefinitionModelGroupFactory.create(
                identifier,
                createSingletonTestRuleItem(identifier + ruleSubPath)
        );
        assertEquals("policy_name/3.0.1", rule2.getRuntimeId());
        assertEquals("abc", rule2.getRuleDefinitionModelList().get(0).getRuleShortName());
        assertEquals(identifier, rule2.getIdentifier());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/lob/policy_name/23.10.1",
            "/lob/policy_name/532.123.20",
            "/lob/policy_name/1.111.30",
            "/lob/policy_name/111.1.345"
    })
    void testMultiDigitMajorMinorVersions(String identifier) {
        // Arrange
        final String expectedVersion = identifier.substring(identifier.lastIndexOf("/") + 1);
        final String expectedPolicyName = "policy_name";

        // Act
        final RuleDefinitionModelGroup ruleGroup = ruleDefinitionModelGroupFactory.create(identifier,
                createSingletonTestRuleItem(identifier + "/rules/rule.dmn"));

        // Assert
        assertEquals(expectedPolicyName + "/" + expectedVersion, ruleGroup.getRuntimeId());
    }

    private static Map<String, String> createSingletonTestRuleItem(final String itemFullPath) {
        return Stream.of(itemFullPath).collect(Collectors.toMap(path -> path, path -> "dummyDmnContent"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "policy_name/1.2.10/",
            "policy_name//1.2.10/",
            "policy_name/1.2.10//",
            "/policy_name/1.2.10/",
            "abcd/policy_name/1.2.10/",
            "abcd/policy_name/1.2.10/",
            "/abcd/policy_name/1.2.10/",
            "//abcd/policy_name/1.2.10/",
            "/qsfds/abcd/policy_name/1.2.10/",
            "/abcd/efgh/policy_name/1.2.10/0",
            "/abcd/efgh/ijkl/policy_name/1.2.10/12345"
    })
    void testTransactionNameWithAppliedIdentifier(String identifier) {
        RuleDefinitionModelGroup rule = ruleDefinitionModelGroupFactory.create(
                identifier,
                createSingletonTestRuleItem(identifier + "rules/abc"));
        assertEquals("policy_name/1.2.10", rule.getRuntimeId());
        assertEquals("abc", rule.getRuleDefinitionModelList().get(0).getRuleShortName());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "policy_name",
            "version",
            "/policy_name/version/",
            "policy_name/",
            "/policy_name/",
            "/policy_name/1/",
            "/policy_name/1.x",
            "/version",
            "/version/",
            "abc/",
            "/abc/ab",
            "rules/moreRules/abc",
            "/a/b/c/d/abcdefg/policy_name/1.0/0/123"
    })
    @EmptySource
    void testInvalidIdentifierFailure(String identifier) {
        assertThrows(RuntimeException.class, () ->
                ruleDefinitionModelGroupFactory.create(identifier,
                        createSingletonTestRuleItem(identifier + "rules/abc")));
    }

    /**
     * This test represents problem of linking DE runtime identifier at load time, which
     * is based on entity identifier, to the runtime identifier at request time, which is based
     * on info from
     * {@link com.capitalone.identity.identitybuilder.policycore.camel.PolicyConstants#HEADER_POLICYROUTENAME}
     */
    @Test
    void testLoadTimeVsRequestTimeDERuntimeIdentifier() {
        String ruleName = "c";
        String entityId = "us_consumers/lob_x/policy_a/1.0";
        String entityVersionNumber = "1";
        String policyName = "policy_a";
        String policyVersion = "1.0";

        String entityIdentifier = entityId + "." + entityVersionNumber;

        RuleDefinitionModelGroup ruleDefinitionModelGroup = ruleDefinitionModelGroupFactory.create(entityIdentifier,
                createSingletonTestRuleItem(entityId + "/" + entityVersionNumber + "/rules/" + ruleName));
        String loadRuntimeId = ruleDefinitionModelGroup.getRuntimeId();
        String requestRuntimeId = policyName + "/" + policyVersion + "." + entityVersionNumber;
        assertEquals(requestRuntimeId, loadRuntimeId);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "policy_name",
            "version",
            "/policy_name/version/",
            "policy_name/",
            "/policy_name/",
            "/policy_name/1/",
            "/policy_name/1.x",
            "/version",
            "/version/",
            "abc/",
            "/abc/ab",
            "rules/moreRules/abc",
            "/a/b/c/d/abcdefg/policy_name/1.0/0/123"
    })
    void testConvertConfigToMap(String path) {
        final ConfigStoreItemInfo configStoreItemInfo = new ConfigStoreItemInfo(path, "dummyTag");
        final ConfigStoreItem item = new ConfigStoreItem(configStoreItemInfo, "dummyDmnContent");
        Map<String, String> map = RuleDefinitionModelGroupFactory.convertItemsToMap(Collections.singleton(item));
        assertEquals(1, map.size());
        assertEquals("dummyDmnContent", map.get(path));
    }

}
