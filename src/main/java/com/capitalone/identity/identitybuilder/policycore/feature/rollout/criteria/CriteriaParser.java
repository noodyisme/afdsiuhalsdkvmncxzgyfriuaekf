package com.capitalone.identity.identitybuilder.policycore.feature.rollout.criteria;

import com.capitalone.identity.identitybuilder.util.StringUtils;
import com.google.re2j.Pattern;

import java.util.Arrays;
import java.util.HashSet;

public class CriteriaParser {

    private static final String REGEX_PREFIX = "regex::";
    private static final String PREFORMATTED_PREFIX = "preformatted::";
    private static final String LIST_PREFIX = "list::";

    private CriteriaParser() {
        // no-op
    }

    public static Criteria parse(String rawCriteria) {
        rawCriteria = StringUtils.requireNotNullOrBlank(rawCriteria).trim();
        if (rawCriteria.startsWith(LIST_PREFIX)) {
            HashSet<String> items = new HashSet<>(Arrays.asList(rawCriteria.substring(LIST_PREFIX.length()).split(",")));
            return new ListCriteria(items);
        } else if (rawCriteria.startsWith(REGEX_PREFIX)) {
            return new RegexCriteria(Pattern.compile(rawCriteria.substring(REGEX_PREFIX.length())));
        } else if (rawCriteria.startsWith(PREFORMATTED_PREFIX)) {
            String preformatted = rawCriteria.substring(PREFORMATTED_PREFIX.length());
            return new RegexCriteria(Pattern.compile(PreformattedRegex.valueOfLabel(preformatted).regex));
        } else {
            throw new IllegalArgumentException("unrecognized input type '" + rawCriteria + "'");
        }
    }
}

