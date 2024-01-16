package com.capitalone.identity.identitybuilder.policycore.feature.rollout.criteria;

import org.apache.logging.log4j.util.Strings;

import java.util.Set;
import java.util.stream.Collectors;

public class ListCriteria implements Criteria {

    private final Set<String> match;

    ListCriteria(Set<String> items) {
        match = items.stream().filter(Strings::isNotBlank).collect(Collectors.toSet());
    }

    @Override
    public boolean isMatch(String input) {
        return match.contains(input);
    }
}
