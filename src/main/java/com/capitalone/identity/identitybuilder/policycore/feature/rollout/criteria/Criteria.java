package com.capitalone.identity.identitybuilder.policycore.feature.rollout.criteria;

public interface Criteria {
    boolean isMatch(String input);
}
