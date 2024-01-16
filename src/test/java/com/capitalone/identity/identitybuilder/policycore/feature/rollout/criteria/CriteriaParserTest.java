package com.capitalone.identity.identitybuilder.policycore.feature.rollout.criteria;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.Assert.assertTrue;

class CriteriaParserTest {

    @ParameterizedTest
    @ValueSource(strings = {
            "list::foo,bar",
            "list::foo,bar  ",
            "  list::foo,bar",
            "regex::^(foo|bar)$",
            "regex::^(foo|bar)$    ",
            "   regex::^(foo|bar)$",
            "preformatted::match-all",
            "preformatted::match-all   ",
            "   preformatted::match-all",
    })
    void checkParse(String input) {
        Criteria parse = CriteriaParser.parse(input);
        assertTrue(parse.isMatch("foo"));
    }

}
