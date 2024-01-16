package com.capitalone.identity.identitybuilder.policycore.operational_audit.model.configstore;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
public class OperationalConfigStoreMetadata {

    @JsonProperty("change_type")
    String changeType;

    @JsonProperty("entity_type")
    String entityType;

    @JsonProperty("entity_id")
    String entityId;

    @JsonProperty("entity_version")
    String entityVersion;

    @JsonProperty("entity_items")
    String entityItems;

    @JsonProperty("items_count")
    String itemsCount;

    @JsonProperty("ignored_non_policy_items")
    String ignoredNonPolicyItems;

    @JsonProperty("scan_scheduled")
    String scanScheduled;

    @JsonProperty("scan_type")
    String scanType;

    @JsonProperty("new_polling_configuration")
    String newPollingConfig;

    @JsonProperty("version_number")
    String configStoreMetadataVersionNumber;

    @JsonProperty("deployment_status")
    String configStoreMetadataDeploymentStatus;

}
