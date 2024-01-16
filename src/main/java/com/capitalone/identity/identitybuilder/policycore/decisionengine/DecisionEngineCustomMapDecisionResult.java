package com.capitalone.identity.identitybuilder.policycore.decisionengine;

import org.springframework.lang.NonNull;

import java.util.HashMap;
import java.util.Objects;

/**
 * Represents a decision engine result output object of type: {@link HashMap<String, Boolean>} that contains
 * a single entry with key=ruleName and value=rule pass/fail boolean.
 *
 * @deprecated use {@link DecisionEngineResult}
 */
@Deprecated
public final class DecisionEngineCustomMapDecisionResult implements DecisionResult {

    @NonNull
    private final String ruleName;

    private final boolean didPass;

    public DecisionEngineCustomMapDecisionResult(DecisionResult result) {
        HashMap<String, Boolean> resultHashMap = (HashMap<String, Boolean>) result.getResult();
        String resultRuleName = resultHashMap.keySet().stream().findFirst().orElse(null);
        this.ruleName = Objects.requireNonNull(resultRuleName,
                String.format("Missing rule entry in Map<String,Boolean> from source outputName '%s'", result.getName()));
        this.didPass = resultHashMap.getOrDefault(ruleName, false);
    }

    @NonNull
    @Override
    public String getName() {
        return ruleName;
    }

    @NonNull
    @Override
    public Object getResult() {
        return getResultAsBoolean();
    }

    @NonNull
    @Override
    public Boolean getResultAsBoolean() {
        return didPass;
    }

}
