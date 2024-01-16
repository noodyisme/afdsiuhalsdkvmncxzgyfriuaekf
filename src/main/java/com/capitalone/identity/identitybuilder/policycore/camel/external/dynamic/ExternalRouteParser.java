package com.capitalone.identity.identitybuilder.policycore.camel.external.dynamic;

import com.capitalone.identity.identitybuilder.model.Entity;
import com.capitalone.identity.identitybuilder.policycore.camel.external.model.ExternalItemDefinitionHolder;

public interface ExternalRouteParser {
    ExternalItemDefinitionHolder parseRouteResource(Entity entity);
}
