package com.capitalone.identity.identitybuilder.policycore.camel.external.model;

import com.capitalone.identity.identitybuilder.model.EntityType;
import org.apache.camel.model.RouteDefinition;
import org.apache.camel.model.RoutesDefinition;
import org.springframework.lang.NonNull;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Utility class that ties policy definitions together as they are being validated.
 */
public final class ExternalItemDefinitionHolder {

    private final List<RouteDefinition> routes;

    private final CamelDataModel model;

    private final EntityType type;

    public ExternalItemDefinitionHolder(@NonNull List<RoutesDefinition> routes,
                                        @NonNull CamelDataModel model,
                                        @NonNull EntityType type) {
        this.routes = routes.stream()
                .map(RoutesDefinition::getRoutes)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
        this.model = model;
        this.type = type;
    }
    
    public List<RouteDefinition> getRoutes() {
        return routes;
    }

    public EntityType getType() {
        return type;
    }

    public CamelDataModel getModel() {
        return model;
    }


}
