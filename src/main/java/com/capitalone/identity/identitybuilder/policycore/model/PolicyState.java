package com.capitalone.identity.identitybuilder.policycore.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.util.*;

/**
 * Policy State object stored in Redis Cache.
 * Redis is a mapping of resourceId -> PolicyState
 */
@Getter @Setter @ToString @EqualsAndHashCode
public class PolicyState {
    private String policyName = "";
    private String policyVersion = "";
    private String effectiveNextStep = "";
    private Map<String, Serializable> policyState = new HashMap<>();
    private Set<String> availableNextSteps = new HashSet<>();
    private List<String> stepsCompleted = new ArrayList<>();
}
