package com.capitalone.identity.identitybuilder.policycore.operational_audit.model.policy;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;

@Builder
@Value
public class OperationalExecutionContext {

    @JsonProperty("api_key")
    String apiKey;

    @JsonProperty("content_type")
    String contentType;

    @JsonProperty("channel_type")
    String channelType;

    @JsonProperty("country_code")
    String countryCode;

    @JsonProperty("accept_language")
    String acceptLanguage;

    @JsonProperty("sub_domain")
    String subDomain;

    @JsonProperty("user_agent")
    String userAgent;

    @JsonProperty("http_accept")
    String accept;

    @JsonProperty("business_event")
    String businessEvent;

    @JsonProperty("x_journey_policy")
    String xJourneyPolicy;

    @JsonProperty("domain")
    String domain;
}
