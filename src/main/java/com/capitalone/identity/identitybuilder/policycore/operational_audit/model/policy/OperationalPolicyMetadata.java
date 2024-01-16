package com.capitalone.identity.identitybuilder.policycore.operational_audit.model.policy;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Builder
@Value
public class OperationalPolicyMetadata {

    @JsonProperty("policy_version_requested")
    String policyVersionRequested;

    @JsonProperty("step_expiration")
    String stepExpiration;

    @JsonProperty("available_next_steps")
    List<String> availableNextSteps;

    @JsonProperty("steps_completed")
    List<String> stepsCompleted;

    @JsonProperty("current_step")
    String step;

    @JsonProperty("request_type")
    PolicyRequestType requestType;

    @JsonProperty("process_id")
    String processId;

    @JsonProperty("policy_information_points_requested")
    @Builder.Default List<String> pipsEvaluated = new ArrayList<>();

    @JsonProperty("decision_modeling_notations_evaluated")
    @Builder.Default List<String> dmnsEvaluated = new ArrayList<>();

    @JsonProperty("policy_name")
    String policyName;

    @JsonProperty("version_executed")
    String versionExecuted;

    @JsonProperty("effective_next_step")
    String effectiveNextStep;

    @JsonProperty("policy_to_policy_parent_name")
    String p2PParentPolicyName;

    @JsonProperty("policy_to_policy_children_policy_names")
    String p2PChildrenPolicyNames;

    @JsonProperty("client_id")
    String clientId;
}
