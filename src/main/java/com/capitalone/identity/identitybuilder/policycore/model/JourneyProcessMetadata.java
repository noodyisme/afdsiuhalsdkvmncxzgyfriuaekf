package com.capitalone.identity.identitybuilder.policycore.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * Metadata associated with process (policy invocation). Returned by GET/PATCH ../{resourceId}
 */
@Data
@AllArgsConstructor
@RequiredArgsConstructor
public class JourneyProcessMetadata implements Serializable {
    private static final long serialVersionUID = 2110825262491737409L;

    @NotNull
    private String policyProcessId;
    @NotNull
    private String policyName;
    @NotNull
    private String policyVersion;
    @NotNull
    private LocalDateTime stepExpirationTimestamp;
    @NotNull
    private Set<String> availableNextSteps;
    @NotNull
    private List<String> stepsCompleted;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String effectiveNextStep;

    public JourneyProcessMetadata(ProcessMetadata policyCoreResponseMetadata) {
        this.policyProcessId = policyCoreResponseMetadata.getResourceId();
        this.policyName = policyCoreResponseMetadata.getPolicyName();
        this.policyVersion = policyCoreResponseMetadata.getPolicyVersion();
        this.stepExpirationTimestamp = policyCoreResponseMetadata.getStepExpiration();
        this.availableNextSteps = policyCoreResponseMetadata.getAvailableNextSteps();
        this.stepsCompleted = policyCoreResponseMetadata.getStepsCompleted();
        this.effectiveNextStep = policyCoreResponseMetadata.getEffectiveNextStep();
    }
}
