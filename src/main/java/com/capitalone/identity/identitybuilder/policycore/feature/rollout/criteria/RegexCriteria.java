package com.capitalone.identity.identitybuilder.policycore.feature.rollout.criteria;

import com.google.re2j.Pattern;

import java.util.Objects;

public class RegexCriteria implements Criteria {

    private final Pattern pattern;

    public RegexCriteria(Pattern pattern) {
        this.pattern = Objects.requireNonNull(pattern);
    }

    @Override
    public boolean isMatch(String input) {
        return input != null && pattern.matcher(input).matches();
    }
}
