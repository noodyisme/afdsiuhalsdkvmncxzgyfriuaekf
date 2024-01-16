package com.capitalone.identity.identitybuilder.policycore.camel.external.dynamic;

import com.capitalone.identity.identitybuilder.model.EntityType;
import com.capitalone.identity.identitybuilder.policycore.camel.external.InvalidItemDefinitionException;
import com.capitalone.identity.identitybuilder.policycore.camel.external.model.CamelDataModel;
import com.capitalone.identity.identitybuilder.policycore.camel.external.model.CamelDataModelImpl;
import org.apache.camel.CamelContext;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Fast incremental Policy validator.
 */
@Component
public class PolicyValidator {

    /**
     * @return set of values that appear more than once in an argument list
     */
    private static <T> Set<T> getDuplicates(List<T> source) {
        return source.stream()
                .filter(it -> Collections.frequency(source, it) > 1)
                .collect(Collectors.toSet());
    }
    private static final String CHILD_POLICY_FLAG = "childPolicy=true";
    private final Set<String> modelNames = new HashSet<>();
    private final Set<String> routeIds = new HashSet<>();
    private final Set<String> internalUris = new HashSet<>();
    private final Set<String> libraryUris = new HashSet<>();
    private final Set<String> processUris = new HashSet<>();
    private final Set<String> childPolicies = new HashSet<>();
    private final HashMap<String, Integer> internalUrisOutbound = new HashMap<>();
    private final HashMap<String, Integer> libraryUrisOutbound = new HashMap<>();
    private final HashMap<String, Integer> processUrisOutbound = new HashMap<>();
    private final List<CamelModelValidator> commonValidators = new ArrayList<>();
    private final List<CamelModelValidator> internalValidators;
    private final List<CamelModelValidator> libraryValidators;
    private final List<CamelModelValidator> processValidators;
    private boolean isInternalModelPopulated = false;

    @Inject
    public PolicyValidator() {
        internalValidators = Arrays.asList(
                new RouteIdsDistinctValidator(routeIds),
                new EndpointUniqueInternalValidator()
        );

        // set up common validators
        commonValidators.addAll(internalValidators);
        commonValidators.addAll(Arrays.asList(
                new RouteIdExistsValidator(),
                new EndpointUniqueValidator(internalUris),
                new EndpointUniqueValidator(libraryUris),
                new EndpointUniqueValidator(processUris),
                new CallsToProhibitedValidator(processUris),
                new CallsFromProhibitedValidator(internalUrisOutbound, "internal"),
                new PolicyToPolicyValidator()

        ));

        libraryValidators = Arrays.asList(
                new CallsToProhibitedValidator(libraryUris),
                new CallsToProhibitedValidator(processUris),
                new CallsToProhibitedValidator(internalUris)
        );

        // prevent policies from supporting calls from libraries
        processValidators = Arrays.asList(
                new CallsFromProhibitedValidator(libraryUrisOutbound, "libraries"),
                // prevent policies from supporting calls from other policies
                new CallsFromProhibitedValidator(processUrisOutbound, "process URI outbound")
        );
    }

    /**
     * @return a set of Strings containing the full policy name and version (ex: tenant/businessUnit/policyName/policyVersion)
     *         of a policy marked with "childPolicy=true"
     */
    public Set<String> getChildPolicies() {
        return childPolicies;
    }

    public void populateInternalModel(@NonNull CamelContext camelContext) {
        if (!isInternalModelPopulated) {
            CamelDataModelImpl internalModel = new CamelDataModelImpl(camelContext);
            populateInternalModel(internalModel);
        }
    }

    void populateInternalModel(@NonNull CamelDataModel internalModel) {
        internalUris.addAll(internalModel.getInboundUris());
        internalModel.getOutboundUris().forEach(uri -> internalUrisOutbound.putIfAbsent(uri, 1));
        internalValidators.forEach(validator -> validator.validateOrThrow(internalModel));
        routeIds.addAll(internalModel.getRouteIds());
        isInternalModelPopulated = true;
    }

    /**
     * Not threadsafe. Caller responsible for coordinating calls to this method
     * and {@link #removeModel(CamelDataModel)}
     */
    public PolicyValidator addModel(CamelDataModel model) {
        if (!isInternalModelPopulated) {
            throw new IllegalStateException("internal models not populated yet");
        }

        if (modelNames.contains(model.getName())) {
            String message = String.format("This model name already loaded: '%s'", model.getName());
            throw new InvalidItemDefinitionException(message);
        }

        EntityType type = Objects.requireNonNull(model.getType());
        validateModel(model);
        if (EntityType.PIP.equals(type)) {
            addLibrary(model);
        } else if (EntityType.POLICY.equals(type)) {
            addPolicy(model);
        }
        modelNames.add(model.getName());
        return this;
    }

    /**
     * Not threadsafe. Caller responsible for coordinating calls to {@link #addModel(CamelDataModel)}
     * and this method.
     *
     * @return {@code true} if a model of that name was removed, false if it was never added
     */
    public boolean removeModel(CamelDataModel model) {
        if (!modelNames.contains(model.getName())) return false;

        routeIds.removeAll(model.getRouteIds());
        if (EntityType.POLICY.equals(model.getType())) {
            processUris.removeAll(model.getInboundUris());
            model.getChildPolicyUri().ifPresent(childPolicies::remove);
            model.getOutboundUris().forEach(uri -> processUrisOutbound.put(uri, processUrisOutbound.getOrDefault(uri, 1) - 1));
        } else {
            libraryUris.removeAll(model.getInboundUris());
            model.getOutboundUris().forEach(uri ->
                    libraryUrisOutbound.put(uri, libraryUrisOutbound.getOrDefault(uri, 1) - 1));
        }
        modelNames.remove(model.getName());
        return true;
    }

    private void validateModel(CamelDataModel model) {
        EntityType type = model.getType();
        if (EntityType.POLICY.equals(type)) {
            commonValidators.forEach(validator -> validator.validateOrThrow(model));
            processValidators.forEach(validator -> validator.validateOrThrow(model));
        } else if (EntityType.PIP.equals(type)) {
            commonValidators.forEach(validator -> validator.validateOrThrow(model));
            libraryValidators.forEach(validator -> validator.validateOrThrow(model));
        } else {
            throw new IllegalArgumentException(String.format("unrecognized model type: %s", type));
        }

    }

    private void addLibrary(CamelDataModel model) {
        this.routeIds.addAll(model.getRouteIds());
        this.libraryUris.addAll(model.getInboundUris());
        model.getOutboundUris().forEach(uri ->
                libraryUrisOutbound.put(uri, libraryUrisOutbound.getOrDefault(uri, 0) + 1));
    }

    private void addPolicy(CamelDataModel model) {
        this.routeIds.addAll(model.getRouteIds());
        this.processUris.addAll(model.getInboundUris());
        model.getChildPolicyUri().ifPresent(childPolicies::add);
        model.getOutboundUris().forEach(uri -> processUrisOutbound.put(uri, processUrisOutbound.getOrDefault(uri, 0) + 1));
    }

    interface CamelModelValidator {
        void validateOrThrow(CamelDataModel next);
    }

    private static class CallsToProhibitedValidator implements CamelModelValidator {

        final Set<String> prohibited;

        private CallsToProhibitedValidator(Set<String> prohibited) {
            this.prohibited = prohibited;
        }

        @Override
        public void validateOrThrow(CamelDataModel next) {
            for (String toUri : next.getOutboundUris()) {
                if (prohibited.contains(toUri)) {
                    String message = String.format("Prohibited call to uri: %s, in %s",
                            toUri, next.getName());
                    throw new InvalidItemDefinitionException(message);
                }
            }
        }
    }

    /**
     * Enforces case that a model shouldn't support prohibited 'from' endpoints. Throws if an external policy
     * has a 'from' uri that is called by an internal policy.
     */
    private static class CallsFromProhibitedValidator implements CamelModelValidator {

        final Map<String, Integer> prohibited;
        final String description;

        private CallsFromProhibitedValidator(Map<String, Integer> prohibited, String description) {
            this.prohibited = prohibited;
            this.description = description;
        }

        @Override
        public void validateOrThrow(CamelDataModel next) {
            for (String fromUri : next.getInboundUris()) {
                if (prohibited.getOrDefault(fromUri, 0) > 0) {
                    String message = String.format("Prohibited 'from' endpoint: %s, in %s",
                            fromUri,
                            next.getName());
                    throw new InvalidItemDefinitionException(message);
                }
            }
        }
    }

    private static class EndpointUniqueValidator implements CamelModelValidator {

        final Set<String> existingEndpoints;

        private EndpointUniqueValidator(Set<String> existingEndpoints) {
            this.existingEndpoints = existingEndpoints;
        }

        @Override
        public void validateOrThrow(CamelDataModel next) {
            for (String inboundUri : next.getInboundUris()) {
                if (existingEndpoints.contains(inboundUri)) {
                    String message = String.format("This endpoint conflicts with existing route: %s, in %s",
                            inboundUri, next.getName());
                    throw new InvalidItemDefinitionException(message);
                }
            }
        }
    }

    private static class EndpointUniqueInternalValidator implements CamelModelValidator {

        @Override
        public void validateOrThrow(CamelDataModel next) {
            List<String> provided = next.getInboundUris();
            Set<String> distinct = new HashSet<>(provided);
            if (provided.size() != distinct.size()) {
                Set<String> repeats = getDuplicates(provided);
                String message = String.format("Found multiple occurrences of the same endpoint: %s, in %s",
                        repeats, next.getName());
                throw new InvalidItemDefinitionException(message);
            }
        }
    }

    /**
     * Checks that non-null route IDs are distinct.
     */
    private static class RouteIdsDistinctValidator implements CamelModelValidator {
        final Set<String> existingRouteIds;

        private RouteIdsDistinctValidator(Set<String> existingRouteIds) {
            this.existingRouteIds = existingRouteIds;
        }

        @Override
        public void validateOrThrow(CamelDataModel next) {

            List<String> provided = next.getRouteIds().stream().filter(Objects::nonNull).collect(Collectors.toList());
            Set<String> distinct = new HashSet<>(provided);

            if (provided.size() != distinct.size()) {
                Set<String> repeats = getDuplicates(provided);
                String message = String.format("Found multiple occurrences of routes ids: %s", repeats);
                throw new InvalidItemDefinitionException(message);
            }

            for (String id : distinct) {
                if (existingRouteIds.contains(id)) {
                    String message = String.format("Route ID conflicts with loaded route: %s", id);
                    throw new InvalidItemDefinitionException(message);
                }
            }

        }
    }

    /**
     * Validates that routes have route IDs (so that they can be identified and replaced as required).
     */
    private static class RouteIdExistsValidator implements CamelModelValidator {

        @Override
        public void validateOrThrow(CamelDataModel next) {

            List<String> provided = next.getRouteIds();
            if (provided.contains(null)) {
                String format = String.format("Discovered route without ID in %s. All routes must have IDs.", next.getName());
                throw new InvalidItemDefinitionException(format);
            }

        }
    }

    /**
     * Validates that child policies (childPolicy=true) do not use the P2P Component
     */
    private static class PolicyToPolicyValidator implements CamelModelValidator {

        @Override
        public void validateOrThrow(CamelDataModel next) {
            Set<String> p2PUrisInPolicy = next.getOutboundUris().stream().filter(out -> out.contains("P2P:")).collect(Collectors.toSet());
            int outboundP2PUriCount = p2PUrisInPolicy.size();
            // If a policy marked as a child policy uses the P2P component, throw an exception and do not load the policy
            if (next.getRouteIds().stream().anyMatch(id -> id.contains(CHILD_POLICY_FLAG)) && outboundP2PUriCount > 0) {
                String format = String.format("Policy \"%s\" uses the P2P component and is labeled with \""
                        + CHILD_POLICY_FLAG + "\". A policy labeled with \"" + CHILD_POLICY_FLAG + "\" cannot use the " +
                        "P2P component", next.getName());
                throw new InvalidItemDefinitionException(format);
            }
        }
    }
}
