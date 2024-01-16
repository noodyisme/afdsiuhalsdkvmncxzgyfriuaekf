package com.capitalone.identity.identitybuilder.policycore.camel.external.model;

import com.capitalone.identity.identitybuilder.model.EntityType;
import com.capitalone.identity.identitybuilder.policycore.camel.external.dynamic.DynamicPolicyHelper;
import org.apache.camel.CamelContext;
import org.apache.camel.model.*;
import org.springframework.lang.Nullable;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CamelDataModelImpl implements CamelDataModel {

    private static List<String> getRouteInputUris(RouteDefinition route) {
        return Collections.singletonList(route.getInput().getUri());
    }

    private static List<ProcessorDefinition<?>> getRouteOutputSubtree(List<ProcessorDefinition<?>> processors) {
        return processors.stream().flatMap(it -> {
            if (it instanceof ToDefinition) {
                return Stream.of((ToDefinition) it);
            } else {
                return getRouteOutputSubtree(it.getOutputs()).stream();
            }
        }).collect(Collectors.toList());
    }

    /**
     * Id of a camel route, e.g. "routeA" in the following camel route declaration: &lt;route id="route_A"&gt;
     */
    private final List<String> routeIds;

    /**
     * Uri that represents an inbound link into a camel route, e.g. "direct:route_A" in the following camel declaration:
     * <b>&lt;from uri="direct:route_A" /&gt;</b>
     */
    private final List<String> inboundUris;

    /**
     * Uri that represents an outbound link from a camel route, e.g. "direct:route_A" in the following camel declaration:
     * <b>&lt;to uri="direct:route_A" /&gt;</b>
     */
    private final List<String> outboundUris;

    private final String name;

    private final EntityType type;

    private final String childPolicyUri;

    private static final Pattern MULTISTEP_POLICY_PATTERN = Pattern.compile("\\d\\.\\d-\\w");

    public static CamelDataModelImpl fromRoutes(String name, List<RoutesDefinition> routesDefinitions, EntityType type) {
        List<RouteDefinition> collected = routesDefinitions.stream()
                .flatMap(definition -> definition.getRoutes().stream())
                .collect(Collectors.toList());
        return new CamelDataModelImpl(name, collected, type);
    }

    private CamelDataModelImpl(String name, List<RouteDefinition> routeDefinitions, @Nullable EntityType type) {
        this.name = name;
        this.type = type;

        routeIds = routeDefinitions.stream()
                .map(OptionalIdentifiedDefinition::getId)
                .collect(Collectors.toList());

        List<String> uri = routeDefinitions.stream()
                .filter(routeDef -> routeDef.getId().contains("childPolicy=true")
                        && routeDef.getInput().getUri().startsWith("policy")
                        && !MULTISTEP_POLICY_PATTERN.matcher(routeDef.getInput().getUri()).find())
                .map(routeDef -> routeDef.getInput().getUri())
                .collect(Collectors.toList());
        if (uri.size() == 1){
            childPolicyUri = uri.get(0);
        } else {
            childPolicyUri = null;
        }


        inboundUris = routeDefinitions.stream()
                .map(CamelDataModelImpl::getRouteInputUris)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());

        outboundUris = routeDefinitions.stream()
                .flatMap(it -> getRouteOutputSubtree(it.getOutputs()).stream())
                .filter(it -> it instanceof ToDefinition)
                .map(it -> ((ToDefinition) it).getUri())
                .collect(Collectors.toList());
    }

    /**
     * Generate a full model from all route definitions that have been added to the {@link CamelContext}
     */
    public CamelDataModelImpl(CamelContext context) {
        this(context.getName(), DynamicPolicyHelper.getRouteDefinitions(context), null);
    }

    @Override
    public List<String> getRouteIds() {
        return routeIds;
    }

    @Override
    public List<String> getInboundUris() {
        return inboundUris;
    }

    @Override
    public List<String> getOutboundUris() {
        return outboundUris;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public EntityType getType() {
        return Objects.requireNonNull(type);
    }

    @Override
    public Optional<String> getChildPolicyUri() {
        return Optional.ofNullable(childPolicyUri);
    }
}
