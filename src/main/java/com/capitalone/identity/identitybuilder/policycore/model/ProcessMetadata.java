package com.capitalone.identity.identitybuilder.policycore.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Metadata associated with process (policy invocation). Returned by GET/PATCH ../{resourceId}
 */
@Data
@AllArgsConstructor
@RequiredArgsConstructor
public class ProcessMetadata implements Serializable {
    private static final long serialVersionUID = 2110825262491737409L;

    @NotNull
    private String resourceId;
    @NotNull
    private String policyName;
    @NotNull
    private String policyVersion;
    @NotNull
    private LocalDateTime stepExpiration;
    @NotNull
    private Set<String> availableNextSteps;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String effectiveNextStep;
    @NotNull
    private List<String> stepsCompleted;
}
