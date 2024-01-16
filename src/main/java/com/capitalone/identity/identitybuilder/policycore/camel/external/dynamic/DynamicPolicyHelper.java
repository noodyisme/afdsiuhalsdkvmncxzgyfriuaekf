package com.capitalone.identity.identitybuilder.policycore.camel.external.dynamic;

import com.capitalone.chassis.engine.model.exception.ChassisSystemException;
import java.util.Optional;
import org.apache.camel.CamelContext;
import org.apache.camel.ExtendedCamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.ModelCamelContext;
import org.apache.camel.model.RouteDefinition;
import org.apache.camel.model.RoutesDefinition;
import org.apache.logging.log4j.CloseableThreadContext;

import java.io.InputStream;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * @author plz569
 * <p>
 * Helper class to load and remove RouteDefinitions
 */
public class DynamicPolicyHelper {

    private DynamicPolicyHelper() {
    }

    /**
     * Loads XML route definition from input stream into RoutesDefinition object
     *
     * @param context Existing Camel Context, instance of {@link CamelContext}
     * @param is      InputStream of XML route definitions, instance of {@link InputStream}
     * @return routesDefinition RouteDefinitions loaded from input stream, instance of {@link RoutesDefinition}
     * @throws Exception Exception occurred during processing
     */
    public static RoutesDefinition loadRouteDefinitions(CamelContext context, InputStream is) throws Exception {
        Optional<RoutesDefinition> routesDefinition = (Optional<RoutesDefinition>) (context.adapt(ExtendedCamelContext.class)).getXMLRoutesDefinitionLoader().loadRoutesDefinition(context, is);
        return routesDefinition.orElseThrow(() -> new ChassisSystemException("Missing Route Definitions"));
    }

    /**
     * Loads XML route definition from input stream into Camel Context and return RoutesDefinition object
     *
     * @param context Existing Camel Context, instance of {@link CamelContext}
     * @param is      InputStream of XML route definitions, instance of {@link InputStream}
     * @return routesDefinition RouteDefinitions loaded from input stream, instance of {@link RoutesDefinition}
     * @throws Exception Exception occurred during processing
     */
    public static RoutesDefinition loadRouteDefinitionsIntoCamelContext(CamelContext context, InputStream is) throws Exception {
        Optional<RoutesDefinition> routesDefinition = (Optional<RoutesDefinition>) (context.adapt(ExtendedCamelContext.class)
                .getXMLRoutesDefinitionLoader().loadRoutesDefinition(context, is));
        routesDefinition.ifPresent( r -> {
            try {
                addRouteDefinitions(context, r.getRoutes());
            } catch (Exception e) {
                throw new ChassisSystemException("Unable to add route definition", e);
            }
        });
        return routesDefinition.orElseThrow(() -> new ChassisSystemException("Missing Route Definitions"));
    }
    /**
     * Adds RouteDefinitions to CamelContext
     *
     * @param context          Existing Camel Context, instance of {@link CamelContext}
     * @param routeDefinitions Collection of Route Definitions, instance of {@link List<RouteDefinition> }
     * @throws Exception Exception occurred during processing
     */
    public static void addRouteDefinitions(CamelContext context, Collection<RouteDefinition> routeDefinitions) throws Exception {
        getModelCamelContext(context).addRouteDefinitions(routeDefinitions);
    }

    /**
     * Removes RouteDefinitions from CamelContext
     *
     * @param context          Existing Camel Context, instance of {@link CamelContext}
     * @param routeDefinitions Collection of Route Definitions, instance of {@link List<RouteDefinition> }
     * @throws Exception Exception occurred during processing
     */
    public static void removeRouteDefinitions(CamelContext context, Collection<RouteDefinition> routeDefinitions) throws Exception {
        getModelCamelContext(context).removeRouteDefinitions(routeDefinitions);
    }

    /**
     * Adapts CamelContext to extended ModelCamelContext
     *
     * @param camelContext Current Camel Context, instance of {@link CamelContext}
     * @return modelCamelContext       Implementation of Camel Context, instance of {@link ModelCamelContext}
     */
    private static ModelCamelContext getModelCamelContext(CamelContext camelContext) {
        return camelContext.adapt(ModelCamelContext.class);
    }

    /**
     * Returns Collection of Route Definitions from a given Camel Context
     *
     * @param context Current Camel Context, instance of {@link CamelContext}
     * @return routeDefinitions     Collection of Route Definitions, instance of {@link List<RouteDefinition>}
     */
    public static List<RouteDefinition> getRouteDefinitions(CamelContext context) {
        return getModelCamelContext(context).getRouteDefinitions();
    }

    /**
     * Add routes expresses by RouteBuilder to a given Camel Context
     *
     * @param camelContext Current Camel Context, instance of {@link CamelContext}
     * @param routeBuilder Route, instance of {@link RouteBuilder}
     * @throws Exception Exception occured during processing, intance of {@link Exception}
     */
    public static void addRoutes(CamelContext camelContext, RouteBuilder routeBuilder) throws Exception {
        camelContext.addRoutes(routeBuilder);
    }

    public static CloseableThreadContext.Instance populateUpdateContext() {
        return CloseableThreadContext.put("DecisionEngineRequestId", UUID.randomUUID().toString());
    }

}

