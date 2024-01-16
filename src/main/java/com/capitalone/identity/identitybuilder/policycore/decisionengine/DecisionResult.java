package com.capitalone.identity.identitybuilder.policycore.decisionengine;

import org.springframework.lang.NonNull;

public interface DecisionResult {

    @NonNull
    String getName();

    @NonNull
    Object getResult();

    /**
     * @throws ClassCastException if result of {@link #getResult()} is not a boolean.
     * @deprecated use {@link #getResult()}
     */
    @NonNull
    @Deprecated
    Boolean getResultAsBoolean();

}
