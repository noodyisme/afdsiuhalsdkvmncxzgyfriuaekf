package com.capitalone.identity.identitybuilder.policycore.camel.external.logging;

import lombok.Value;

@Value
public class LoggedPollingConfigurationChange {

    String description;
    String error;

}
