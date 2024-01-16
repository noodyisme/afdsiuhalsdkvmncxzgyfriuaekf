package com.capitalone.identity.identitybuilder.policycore.camel.external.dynamic;

import com.capitalone.identity.identitybuilder.model.ConfigStoreItem;
import com.capitalone.identity.identitybuilder.model.Entity;
import com.capitalone.identity.identitybuilder.model.EntityType;
import com.capitalone.identity.identitybuilder.model.PolicyInfo;
import com.capitalone.identity.identitybuilder.policycore.camel.external.InvalidItemDefinitionException;
import com.capitalone.identity.identitybuilder.policycore.camel.external.ItemUtils;
import com.capitalone.identity.identitybuilder.policycore.camel.external.model.CamelDataModelImpl;
import com.capitalone.identity.identitybuilder.policycore.camel.external.model.ExternalItemDefinitionHolder;
import lombok.extern.java.Log;
import org.apache.camel.CamelContext;
import org.apache.camel.model.*;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.xml.sax.SAXParseException;

import javax.inject.Inject;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
@Log
public class ExternalRouteParserImpl implements ExternalRouteParser {

    public static final String POLICY_REPLACE_STRING = "policyReplaceString";
    public static final String POLICY_SHORT_NAME_STRING = "policyShortName";
    private static final Pattern policyUriPattern = Pattern.compile("^(?:policy:|direct:)(?<policyReplaceString>(?<policyShortName>[-_a-zA-Z\\d]+)_\\d+\\.\\d+)(?:$|-schema$|-.+$|-.+-schema$)");
    private static final Pattern transformationPattern = Pattern.compile(
            "(?<strategyRef>strategyRef\\s*=\\s*)|" +
                "(?<removeProperty><removeProperty\\s+propertyName\\s*=\\s*)|" +
                    "(?<stopOnAggregateException>stopOnAggregateException\\s*=\\s*\"(true|false)\")");
    private final CamelContext context;

    /**
     * This integer variable is used to compare against the CompileVersion policy-metadata.json attribute of policies.
     * InvalidItemDefinition will be thrown at policy load time if a policy compile version is greater than the version
     * defined here.
     */
    private static final int POLICY_COMPILATION_VERSION = 3;

    @Inject
    public ExternalRouteParserImpl(CamelContext context) {
        this.context = context;
    }

    @Override
    public ExternalItemDefinitionHolder parseRouteResource(Entity entity) {
        if (entity instanceof Entity.Policy) {
            return parseRouteResource(entity, ((Entity.Policy) entity).getProcessItems(), EntityType.POLICY);
        } else if (entity instanceof Entity.Pip) {
            return parseRouteResource(entity, Collections.singleton(((Entity.Pip) entity).getRouteFile()), EntityType.PIP);
        } else {
            return parseRouteResource(entity, Collections.emptySet(), EntityType.UNDEFINED);
        }
    }

    private ExternalItemDefinitionHolder parseRouteResource(Entity entity, Set<ConfigStoreItem> items, EntityType type) {
        List<RoutesDefinition> routesDefinitions = items.stream()
                .map(item -> {
                    try {
                        InputStream is = type == EntityType.POLICY ? ItemUtils.asStream(getCompileableItemContent(item, entity)) : ItemUtils.asStream(item.content);
                        // this is where camel validates xml format
                        return DynamicPolicyHelper.loadRouteDefinitions(context, is);
                    } catch (SAXParseException e) {
                        String msg = String.format("Error loading policy due to bad xml format: %s", item.getName());
                        throw new InvalidItemDefinitionException(msg, e);
                    } catch (Exception e) {
                        String msg = String.format("Error loading policy: %s", item.getName());
                        throw new InvalidItemDefinitionException(msg, e);
                    }
                })
                .collect(Collectors.toList());

        if (type == EntityType.POLICY) {
            final PolicyInfo.Patch newPolicyInfo = (PolicyInfo.Patch) entity.getInfo();

            // Update 'to' definitions
            List<ToDefinition> toDefinitions = routesDefinitions.stream()
                    .flatMap(routesDefinition -> routesDefinition.getRoutes().stream())
                    .map(RouteDefinition::getOutputs)
                    .flatMap(this::getAllToRoutes)
                    .collect(Collectors.toList());

            List<FromDefinition> fromDefinitions = routesDefinitions.stream()
                    .flatMap(routesDefinition -> routesDefinition.getRoutes().stream())
                    .map(RouteDefinition::getInput)
                    .collect(Collectors.toList());

            List<WireTapDefinition>  wireTapDefinitions = routesDefinitions.stream()
                    .flatMap(routesDefinition -> routesDefinition.getRoutes().stream())
                    .map(RouteDefinition::getOutputs)
                    .flatMap(this::getAllWireTapRoutes)
                    .collect(Collectors.toList());

            Set<String> fromUris = fromDefinitions.stream().map(FromDefinition::getUri)
                    .collect(Collectors.toSet());

            updateToDefinition(newPolicyInfo, toDefinitions, fromUris);

            updateWireTapDefinition(newPolicyInfo, wireTapDefinitions, fromUris);

            // Update 'from' definitions
            updateFromDefinitions(newPolicyInfo, fromDefinitions, fromUris);

            // add namespace to route IDs to avoid conflicts
            addNamespacesToRouteIds(newPolicyInfo, routesDefinitions);

            // transform all 'enrich' and 'pollEnrich' endpoints that reference internal 'from' uris
            transformInternalFromUri(newPolicyInfo, routesDefinitions, fromUris);
        }
        return new ExternalItemDefinitionHolder(routesDefinitions,
                CamelDataModelImpl.fromRoutes(entity.getInfo().getLocationPrefix(), routesDefinitions, type), type);
    }

    private String getCompileableItemContent(ConfigStoreItem item, Entity entity) {
        int compileVersion = ((Entity.Policy) entity).getCompileVersion();
        if (compileVersion > POLICY_COMPILATION_VERSION) {
            String msg = String.format("Error loading policy %s due to compileVersion exceeding limit. compileVersion: %d maxCompileVersion: %d", item.getName(), compileVersion, POLICY_COMPILATION_VERSION);
            throw new InvalidItemDefinitionException(msg);
        }
        if(compileVersion == POLICY_COMPILATION_VERSION) {
            return item.content;
        }

        Matcher matcher = transformationPattern.matcher(item.content);
        StringBuffer result = new StringBuffer();
        while(matcher.find()) {
            if(matcher.group("strategyRef")!= null && matcher.group().matches(matcher.group("strategyRef"))){
                matcher.appendReplacement(result, "aggregationStrategy=");
            }
            if(matcher.group("removeProperty") != null && matcher.group().matches(matcher.group("removeProperty"))){
                matcher.appendReplacement(result, "<removeProperty name=");
            }
            if(matcher.group("stopOnAggregateException") != null && matcher.group().matches(matcher.group("stopOnAggregateException"))){
                matcher.appendReplacement(result, "");
            }
        }
        matcher.appendTail(result);
        return result.toString();
    }

    private void transformInternalFromUri(PolicyInfo.Patch policy, List<RoutesDefinition> routesDefinitions, Set<String> fromUris) {
        routesDefinitions.stream()
                .flatMap(routesDefinition -> routesDefinition.getRoutes().stream())
                .map(RouteDefinition::getOutputs)
                .flatMap(this::getAllEnrichRoutes)
                .map(ExpressionNode::getExpression)
                .forEach(expression -> {
                    String expressionWithUri = expression.getExpression();
                    String namespacedExpression = getNamespacedUri(expressionWithUri, policy, fromUris);
                    expression.setExpression(namespacedExpression);
                });
    }

    private static void addNamespacesToRouteIds(PolicyInfo.Patch policy, List<RoutesDefinition> routesDefinitions) {
        String newPolicyString = getNewPolicyString(policy);
        Stream<RouteDefinition> routes = routesDefinitions.stream()
                .flatMap(routesDefinition -> routesDefinition.getRoutes().stream());
        routes.forEach(routeDefinition -> {
            // add namespace to routeIds that have been set
            String routeId = routeDefinition.getRouteId();
            if (StringUtils.isNotBlank(routeId)) {
                routeDefinition.setId(routeId + "[ns=" + newPolicyString + "]");
            }
        });
    }

    /**
     * @param srcUri        can be any camel uri (e.g. uri set in 'from' or 'to' construct)
     * @param policy        policy where the uri is executing
     * @param consumingUris list of 'from' uris defined in the policy that represent internal addresses
     * @return a version of the uri that is 'namespaced' to the policy patch version and should be used
     * instead of the supplied uri. URI's that should be publicly-addressable are transformed to include
     * policy patch version information. URI's that are internally-addressable are transformed to append policy
     * patch information to the address. All other uri's are left alone (e.g. other camel 'to' uris, or camel
     * routes defined at the system-level)
     */
    static String getNamespacedUri(String srcUri, PolicyInfo.Patch policy, Set<String> consumingUris) {
        final String newPolicyString = getNewPolicyString(policy);
        Matcher matcher = policyUriPattern.matcher(srcUri);
        if (matcher.matches() && policy.getPolicyShortName().equals(matcher.group(POLICY_SHORT_NAME_STRING))) {
            String stringToReplace = matcher.group(POLICY_REPLACE_STRING);
            return srcUri.replace(stringToReplace, newPolicyString);
        } else if (consumingUris.contains(srcUri)) {
            return srcUri + "|" + newPolicyString;
        } else {
            return srcUri;
        }
    }

    @NotNull
    static String getNewPolicyString(PolicyInfo.Patch policy) {
        return policy.getPolicyShortName() + "_" + policy.getPolicyPatchVersionString();
    }

    private static void updateToDefinition(PolicyInfo.Patch policy, List<ToDefinition> toDefinitionStream, Set<String> fromUris) {
        toDefinitionStream.forEach(route -> {
            String namespacedUri = getNamespacedUri(route.getUri(), policy, fromUris);
            log.info(String.format("ReplaceUri-to [from=%s to=%s]", route.getUri(), namespacedUri));
            route.setUri(namespacedUri);
        });
    }

    private static void updateWireTapDefinition(PolicyInfo.Patch policy, List<WireTapDefinition> wireTapDefinitionStream, Set<String> fromUris) {
        wireTapDefinitionStream.forEach(route -> {
            String namespacedUri = getNamespacedUri(route.getUri(), policy, fromUris);
            log.info(String.format("ReplaceUri-wiretap [from=%s to=%s]", route.getUri(), namespacedUri));
            route.setUri(namespacedUri);
        });
    }

    private static void updateFromDefinitions(PolicyInfo.Patch policy, List<FromDefinition> fromDefinitionStream, Set<String> fromUris) {
        fromDefinitionStream.forEach(route -> {
            String namespacedUri = getNamespacedUri(route.getUri(), policy, fromUris);
            log.info(String.format("ReplaceUri-from [from=%s to=%s]", route.getUri(), namespacedUri));
            route.setUri(namespacedUri);
        });
    }

    private Stream<ToDefinition> getAllToRoutes(List<ProcessorDefinition<?>> processors) {
        return processors.stream().flatMap(output -> {
            if (output instanceof ToDefinition) {
                return Stream.of((ToDefinition) output);
            } else {
                return getAllToRoutes(output.getOutputs());
            }
        });
    }

    private Stream<WireTapDefinition> getAllWireTapRoutes(List<ProcessorDefinition<?>> processors) {
        return processors.stream().flatMap(output -> {
            if (output instanceof WireTapDefinition) {
                return Stream.of((WireTapDefinition) output);
            } else {
                return getAllWireTapRoutes(output.getOutputs());
            }
        });
    }

    private Stream<ExpressionNode> getAllEnrichRoutes(List<ProcessorDefinition<?>> processors) {
        return processors.stream().flatMap(output -> {
            if (output instanceof EnrichDefinition || output instanceof PollEnrichDefinition) {
                return Stream.of((ExpressionNode) output);
            } else {
                return getAllEnrichRoutes(output.getOutputs());
            }
        });
    }
}
