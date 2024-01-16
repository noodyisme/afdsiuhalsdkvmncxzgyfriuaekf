package com.capitalone.identity.identitybuilder.policycore.decisionengine;

import com.capitalone.identity.identitybuilder.decisionengine.service.api.DecisionEvaluateResponse;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.springframework.lang.NonNull;

import java.util.Map;
import java.util.Objects;

public class DecisionEngineResult implements DecisionResult {

    @NonNull
    private final Object result;

    @NonNull
    private final String name;

    private final DecisionEvaluateResponse decisionEvaluateResponse;

    /**
     * @deprecated Use DecisionEngineResult(outputName, response, decisionEvaluateResponse), or otherwise mock this
     * object or its interface if needed for isolated testing
     * @param outputName the outputName of the response key of interest
     * @param response the response from decision engine
     */
    @Deprecated
    public DecisionEngineResult(@NonNull String outputName, @NonNull Map<String, Object> response) {
        this(outputName, response, null);
    }

    public DecisionEngineResult(@NonNull String outputName, @NonNull Map<String, Object> response,
            DecisionEvaluateResponse decisionEvaluateResponse) {
        this.name = Objects.requireNonNull(outputName);
        this.result = Objects.requireNonNull(
                Objects.requireNonNull(response, "null response from DMN engine").get(name),
                String.format("Missing key '%s' in dmn response", outputName));
        this.decisionEvaluateResponse = decisionEvaluateResponse;
    }

    @NonNull
    @Override
    public String getName() {
        return name;
    }

    @NonNull
    @Override
    public Object getResult() {
        return result;
    }

    @JsonIgnore
    public DecisionEvaluateResponse getDecisionEvaluateResponse() {
        return decisionEvaluateResponse;
    }

    @NonNull
    @JsonIgnore
    @Override
    public Boolean getResultAsBoolean() {
        return (Boolean) result;
    }

}
