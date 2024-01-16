package com.capitalone.identity.identitybuilder.policycore.feature.rollout.criteria;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Arrays;
import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ListCriteriaTest {

    @ParameterizedTest
    @CsvSource({
            "abc,abc,xyz",
            "xyz,abc,xyz",
            "abc,xyz,abc",
            "xyz,xyz,abc",
            "abc,abc,abc",
            "xyz,xyz,xyz",
            "abc,abc,",
            "xyz,xyz,",
            "abc,,abc",
            "xyz,,xyz",
    })
    void testListCriteriaMatch(String input, String listItem1, String listItem2) {
        HashSet<String> matchList = new HashSet<>(Arrays.asList(listItem1, listItem2));
        ListCriteria criteria = new ListCriteria(matchList);
        assertTrue(criteria.isMatch(input));
    }

    @ParameterizedTest
    @CsvSource({
            "ABC,abc,xyz",
            "abc,ABC,xyz",
            "ABC,xyz,abc",
            "abc,xyz,ABC",
            "abc,,",
            ",abc,xyz",
            "abcde,abc,xyz",
    })
    void testListCriteriaNoMatch(String input, String listItem1, String listItem2) {
        HashSet<String> matchList = new HashSet<>(Arrays.asList(listItem1, listItem2));
        ListCriteria criteria = new ListCriteria(matchList);
        assertFalse(criteria.isMatch(input));
    }

    @Test
    void testListCriteriaNoMatchNullWhitespaceDifferences() {
        HashSet<String> matchList = new HashSet<>(Arrays.asList(
                " abc", "abc ", "\tabc", "\nabc", null, "", "\t", "\n"));
        ListCriteria criteria = new ListCriteria(matchList);
        assertFalse(criteria.isMatch("abc"));
        assertFalse(criteria.isMatch(""));
        assertFalse(criteria.isMatch("\t"));
        assertFalse(criteria.isMatch("\n"));
        assertFalse(criteria.isMatch(null));
    }

}
