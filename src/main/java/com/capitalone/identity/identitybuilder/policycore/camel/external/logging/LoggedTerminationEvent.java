package com.capitalone.identity.identitybuilder.policycore.camel.external.logging;

import lombok.Value;

/**
 * see {@link RuntimeUpdateEventLogger}
 */
@Value
public class LoggedTerminationEvent {

    String error;

}
