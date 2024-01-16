package com.capitalone.identity.identitybuilder.policycore.camel.external.dynamic;

import com.capitalone.identity.identitybuilder.ClientEnvironment;
import com.capitalone.identity.identitybuilder.configmanagement.ConfigManagementModel;
import com.capitalone.identity.identitybuilder.model.Entity;
import com.capitalone.identity.identitybuilder.model.EntityInfo;
import com.capitalone.identity.identitybuilder.policycore.camel.external.model.ExternalItemDefinitionHolder;
import com.capitalone.identity.identitybuilder.policycore.configmanagement.ConfigManagementService;
import com.capitalone.identity.identitybuilder.policycore.configmanagement.camel.ConfigManagementEndpoint;
import com.capitalone.identity.identitybuilder.policycore.decisionengine.DecisionEngineRuntimeLoadService;
import com.capitalone.identity.identitybuilder.policycore.dmnrules.RuleDefinitionModelGroup;
import com.capitalone.identity.identitybuilder.policycore.dmnrules.RuleDefinitionModelGroupFactory;
import com.capitalone.identity.identitybuilder.policycore.policymode.PolicyModeEndpoint;
import com.capitalone.identity.identitybuilder.policycore.feature.property.ConfigManagementPropertySetter;
import com.capitalone.identity.identitybuilder.policycore.schema.SchemaManager;
import com.capitalone.identity.identitybuilder.policycore.service.versions.PolicyVersionService;
import org.apache.camel.CamelContext;
import org.apache.camel.model.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.*;
import java.util.function.*;
import java.util.stream.Stream;

import static com.capitalone.identity.identitybuilder.policycore.camel.external.dynamic.DynamicPolicyHelper.addRouteDefinitions;
import static com.capitalone.identity.identitybuilder.policycore.camel.external.dynamic.DynamicPolicyHelper.removeRouteDefinitions;

@Component
public class DynamicPolicyLoaderImpl implements DynamicPolicyLoader {

    public static String getPatchIdentifier(EntityInfo info) {
        return info.getId() + "." + info.getPatchVersion();
    }

    public static String getPatchIdentifier(String policyName, String policyPatchVersion) {
        return policyName + "/" + policyPatchVersion;
    }

    private static final Logger logger = LogManager.getLogger(DynamicPolicyLoaderImpl.class.getName());
    private final Map<String, ExternalItemDefinitionHolder> routeItems = new HashMap<>();
    private final Map<String, RuleDefinitionModelGroup> ruleItems = new HashMap<>();
    private final DecisionEngineRuntimeLoadService decisionEngineRuntimeLoadService;
    private final PolicyValidator validator;
    private final CamelContext context;
    private final ExternalRouteParser routeParser;
    private final SchemaManager schemaManager;
    private final ConfigManagementService configManagementService;
    private final boolean enforceDeletable;

    private final String environmentName;
    private final RuleDefinitionModelGroupFactory ruleDefinitionModelGroupFactory = new RuleDefinitionModelGroupFactory();

    @Inject
    public DynamicPolicyLoaderImpl(CamelContext context, //NOSONAR
                                   DecisionEngineRuntimeLoadService decisionEngineRuntimeLoadService,
                                   PolicyValidator validator,
                                   ExternalRouteParser routeParser,
                                   SchemaManager schemaManager,
                                   ConfigManagementService configManagementService,
                                   ClientEnvironment environment,
                                   @Value(" ${" + PolicyVersionService.FEATURE_FLAG_VERSION_FORWARDER_KEY + "}")
                                   boolean versionForwarderEnabled,
                                   @Value("${ENVIRONMENT_NAME:UNK}")
                                   String environmentName) {

        this.decisionEngineRuntimeLoadService = Objects.requireNonNull(decisionEngineRuntimeLoadService);
        this.routeParser = Objects.requireNonNull(routeParser);
        this.context = Objects.requireNonNull(context);
        this.validator = Objects.requireNonNull(validator);
        this.schemaManager = Objects.requireNonNull(schemaManager);
        this.configManagementService = Objects.requireNonNull(configManagementService);
        this.enforceDeletable = versionForwarderEnabled && ClientEnvironment.DEV == environment;
        this.environmentName = environmentName;
    }

    @Override
    public UpdateTransactionResult add(Entity entity) {

        UpdateTransactionResult addResult = addEntity(entity);
        if (enforceDeletable && addResult.isSuccess()) {
            // attempt a delete operation to ensure that this entity
            // can be removed from service in the future in case of
            // version forwarder rollback
            String identifier = getPatchIdentifier(entity.getInfo());
            UpdateTransactionResult deleteResult = delete(identifier);
            if (deleteResult.isSuccess()) {
                return addEntity(entity);
            } else {
                return deleteResult;
            }
        } else {
            return addResult;
        }
    }

    private UpdateTransactionResult addEntity(Entity entity) {

        final String identifier = getPatchIdentifier(entity.getInfo());
        if (routeItems.containsKey(identifier) || ruleItems.containsKey(identifier)) {
            ExternalItemDefinitionHolder prevItem = routeItems.get(identifier);
            RuleDefinitionModelGroup prevRule = ruleItems.get(identifier);
            String msg = String.format("Entity(s) already exist [%s,%s]", prevItem, prevRule);
            throw new IllegalArgumentException(msg);
        }

        ConfigManagementModel newConfigManagement = null;
        ExternalItemDefinitionHolder newRoutes;
        Optional<RuleDefinitionModelGroup> newRulesOptional = Optional.empty();

        // parsing steps do not require rollback
        try {
            if (entity instanceof Entity.Pip) {
                newRoutes = routeParser.parseRouteResource(entity);
            } else if (entity instanceof Entity.Policy) {
                newRoutes = routeParser.parseRouteResource(entity);
                Entity.Policy policy = (Entity.Policy) entity;
                newConfigManagement = policy.getConfigManagementModelForEnv(environmentName).orElse(null);

                // enrich policy-configuration routes by appending identifier
                newRoutes.getRoutes().stream()
                        .map(RouteDefinition::getOutputs)
                        .flatMap(this::getPolicyConfigurationRoutes)
                        .forEach(route -> route.setUri(route.getUri() + "&policyId=" + identifier));

                // enrich PolicyMode routes by appending identifier
                newRoutes.getRoutes().stream()
                        .map(RouteDefinition::getOutputs)
                        .flatMap(this::getPolicyModeRoutes)
                        .forEach(route -> route.setUri(route.getUri() + "?policyId=" + identifier));

                // enrich property placeholder
                newRoutes.getRoutes().stream()
                        .map(RouteDefinition::getOutputs)
                        .forEach(outputList ->
                                applyPolicyNamespaceToConfigManagementPropertiesInRoutes(identifier, outputList));

                if (!policy.getRuleItems().isEmpty()) {
                    newRulesOptional = Optional.of(ruleDefinitionModelGroupFactory.create(
                            identifier, RuleDefinitionModelGroupFactory.convertItemsToMap(policy.getRuleItems())));
                }

            } else {
                throw new UnsupportedOperationException(
                        String.format("Entity type not supported: %s [entity=%s]", entity, identifier));
            }
        } catch (Exception e) {
            // no need to perform rollback ops because nothing has changed
            return UpdateTransactionResult.error(e);
        }

        // steps that require cleanup
        try {
            routeItems.put(identifier, newRoutes);
            newRulesOptional.ifPresent(newRules -> ruleItems.put(identifier, newRules));
            validator.addModel(newRoutes.getModel());
            configManagementService.setPolicyConfiguration(identifier, newConfigManagement);
            addRouteDefinitions(context, newRoutes.getRoutes());
            newRulesOptional.ifPresent(decisionEngineRuntimeLoadService::loadRulesIntoDecisionRuntime);
            return UpdateTransactionResult.success();
        } catch (Exception e) {
            try {
                // attempt to clean up
                routeItems.remove(identifier);
                Optional<RuleDefinitionModelGroup> previousRules = Optional.ofNullable(ruleItems.remove(identifier));
                configManagementService.deletePolicyConfiguration(identifier);
                if (validator.removeModel(newRoutes.getModel())) {
                    // we know validation step passed and we should try to remove routes from context
                    removeRouteDefinitions(context, newRoutes.getRoutes());
                    previousRules.ifPresent(decisionEngineRuntimeLoadService::removeDecisionRuntime);
                }

                // cleanup failure, internal state is compromised
                return UpdateTransactionResult.error(e);
            } catch (Exception rollbackError) {
                logger.error(String.format("Unrecoverable Error [entityId=%s]", identifier), rollbackError);
                // rollback failure, internal state is compromised
                throw new PolicyAddCleanupFailedException(e, rollbackError);
            }
        }

    }

    private Stream<ToDefinition> getPolicyConfigurationRoutes(List<ProcessorDefinition<?>> processors) {
        return processors.stream()
                .flatMap(output -> {
                    if (output instanceof ToDefinition) {
                        if (((ToDefinition) output).getUri().startsWith(ConfigManagementEndpoint.SCHEME)) {
                            return Stream.of((ToDefinition) output);
                        } else {
                            return Stream.empty();
                        }
                    } else {
                        return getPolicyConfigurationRoutes(output.getOutputs());
                    }
                });
    }

    private Stream<ToDefinition> getPolicyModeRoutes(List<ProcessorDefinition<?>> processors) {
        return processors.stream()
                .flatMap(output -> {
                    if (!(output instanceof ToDefinition)) {
                        return getPolicyModeRoutes(output.getOutputs());
                    }

                    ToDefinition outputDef = (ToDefinition) output;
                    if (!outputDef.getUri().startsWith(PolicyModeEndpoint.SCHEME)) {
                        return Stream.empty();
                    }
                    return Stream.of(outputDef);
                });
    }

    /**
     * Transform input processors recursively to apply the provided policy namespace to processors
     * that leverage config management feature-level property placeholders (e.g. {{policy-config-feature:featureX}}
     *
     * @param identifier           namespace to apply to the config property placeholder
     * @param processorDefinitions list of processors that may contain property placeholders
     */
    private void applyPolicyNamespaceToConfigManagementPropertiesInRoutes(
            String identifier,
            List<ProcessorDefinition<?>> processorDefinitions
    ) {
        for (final ProcessorDefinition<?> output : processorDefinitions) {
            if (output instanceof ExpressionNode) {
                ExpressionNode out = (ExpressionNode) output;
                transformationFunction(identifier,
                        () -> out.getExpression().getExpression(),
                        ConfigManagementPropertySetter::getNamespacedPropertyTransformation,
                        transformed -> out.getExpression().setExpression(transformed)
                );
            } else if (output instanceof ToDefinition) {
                ToDefinition out = (ToDefinition) output;
                transformationFunction(identifier,
                        out::getUri,
                        ConfigManagementPropertySetter::getNamespacedPropertyTransformation,
                        out::setUri
                );
            } else if (output instanceof ToDynamicDefinition) {
                ToDynamicDefinition out = (ToDynamicDefinition) output;
                transformationFunction(identifier,
                        out::getUri,
                        ConfigManagementPropertySetter::getNamespacedPropertyTransformation,
                        out::setUri
                );
            }
            applyPolicyNamespaceToConfigManagementPropertiesInRoutes(identifier, output.getOutputs());

        }
    }

    /**
     * Convenience function that coordinates a transformation of a source value (getter) to a destination
     * value (setter) according to the supplied transformation function. The transformation stops if a
     * null value is returned from any stage.
     *
     * @param identifier  namespace to apply in the transformation
     * @param getter      supplies the candidate value for transformation
     * @param transformer this function should return non-null transformed value if transformation is required, or null if no transformation is required
     * @param setter      called when transformer generates a non-null transformed value
     */
    private void transformationFunction(String identifier, Supplier<String> getter, BinaryOperator<String> transformer,
                                        Consumer<String> setter) {
        String inputUri = getter.get();
        if (inputUri != null) {
            String result = transformer.apply(inputUri, identifier);
            if (result != null) {
                setter.accept(result);
            }
        }
    }

    @Override
    public UpdateTransactionResult delete(EntityInfo info) {
        String identifier = getPatchIdentifier(info);
        return delete(identifier);
    }

    private UpdateTransactionResult delete(String identifier) {
        final ExternalItemDefinitionHolder previousRoutes = Objects.requireNonNull(routeItems.remove(identifier));
        final Optional<RuleDefinitionModelGroup> previousRules = Optional.ofNullable(ruleItems.remove(identifier));

        try {
            if (validator.removeModel(previousRoutes.getModel())) {
                removeRouteDefinitions(context, previousRoutes.getRoutes());
            }

            configManagementService.deletePolicyConfiguration(identifier);
            previousRules.ifPresent(decisionEngineRuntimeLoadService::removeDecisionRuntime);
            schemaManager.purgeCache();
            return UpdateTransactionResult.success();
        } catch (Exception e) {
            return UpdateTransactionResult.error(e);
        }
    }

    public static class PolicyAddCleanupFailedException extends RuntimeException {
        public PolicyAddCleanupFailedException(Throwable originalError, Throwable rollbackError) {
            super("Suppressed rollback error during dynamic policy load/update operation", originalError);
            this.addSuppressed(rollbackError);
        }
    }
}
