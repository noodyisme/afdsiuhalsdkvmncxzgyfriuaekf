package com.capitalone.identity.identitybuilder.policycore.operational_audit.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

/**
 * Information about the system that is specified at startup
 */
@Value
@Builder
public class OperationalSystemContext {

    @JsonProperty("application_name")
    @NonNull String applicationName;

    @JsonProperty("application_code")
    String applicationCode;

    @JsonProperty("cluster_id")
    String clusterId;

    @JsonProperty("container_id")
    String containerId;

    @JsonProperty("host_name")
    String hostName;

    @JsonProperty("host_ip_address")
    String hostIpAddress;

    @JsonProperty("region")
    String region;

    @JsonProperty("environment_name")
    String environmentName;

    @JsonProperty("client_id")
    String clientId;

    @JsonProperty("business_application")
    String businessApplication;

    @JsonProperty("system_id")
    String systemId;

}
