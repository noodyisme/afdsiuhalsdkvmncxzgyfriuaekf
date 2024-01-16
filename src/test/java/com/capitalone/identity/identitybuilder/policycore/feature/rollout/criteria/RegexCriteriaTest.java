package com.capitalone.identity.identitybuilder.policycore.feature.rollout.criteria;

import com.google.re2j.Pattern;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class RegexCriteriaTest {
    static final Pattern FOOBARBAZ = Pattern.compile("(^foo.+$|^.+bar.+$|^.+baz$)");

    @Test
    void testNullAndEmpty() {
        RegexCriteria criteria = new RegexCriteria(Pattern.compile(".*"));
        assertFalse(criteria.isMatch(null));
        assertTrue(criteria.isMatch(UUID.randomUUID().toString()));
        assertTrue(criteria.isMatch(""));
    }

    @Test
    void testConstructorThrowsNPE() {
        assertThrows(NullPointerException.class, ()->new RegexCriteria(null));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "foojj",
            "jjbarjj",
            "jjbaz",
            "foobarbaz",
            "bazbarfoo",
    })
    void testListCriteriaMatch(String input) {
        RegexCriteria criteria = new RegexCriteria(FOOBARBAZ);
        assertTrue(criteria.isMatch(input));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "bazfoobar",
            "barbazfoo",
            "foo",
            "bar",
            "baz",
            "abc",
            "xyz",
    })
    @NullAndEmptySource
    void testListCriteriaNoMatch(String input) {
        RegexCriteria criteria = new RegexCriteria(FOOBARBAZ);
        assertFalse(criteria.isMatch(input));
    }

}

