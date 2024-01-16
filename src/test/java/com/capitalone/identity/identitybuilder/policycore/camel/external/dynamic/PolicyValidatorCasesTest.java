package com.capitalone.identity.identitybuilder.policycore.camel.external.dynamic;

import com.capitalone.identity.identitybuilder.model.EntityType;
import com.capitalone.identity.identitybuilder.policycore.camel.external.InvalidItemDefinitionException;
import com.capitalone.identity.identitybuilder.policycore.camel.external.model.CamelDataModel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullSource;
import org.springframework.lang.Nullable;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TODO: check for redundancies with to PolicyValidatorTest
 */
class PolicyValidatorCasesTest {

    protected static List<CamelDataModel> listOfCamelModels(CamelDataModel... models) {
        return Arrays.asList(models);
    }

    public static void verifyItemsOrThrow(List<CamelDataModel> models) {

        List<CamelDataModel> internal = models.stream()
                .filter(item -> item.getType() == null)
                .collect(Collectors.toList());

        CamelDataModel model = MockModel.internal()
                .withRouteIds(internal.stream().flatMap(it -> it.getRouteIds().stream()).toArray(String[]::new))
                .withLinksFrom(internal.stream().flatMap(it -> it.getInboundUris().stream()).toArray(String[]::new))
                .withLinksTo(internal.stream().flatMap(it -> it.getOutboundUris().stream()).toArray(String[]::new));

        PolicyValidator validator = new PolicyValidator();
        validator.populateInternalModel(model);

        models.stream().filter(item -> Objects.nonNull(item.getType())).forEach(validator::addModel);

    }

    @Test
    void verifyPolicies_empty() {
        assertDoesNotThrow(() -> verifyItemsOrThrow(Collections.emptyList()));
    }

    @Test
    void verifyPolicies_ok() {
        List<CamelDataModel> models = listOfCamelModels(
                MockModel.policy().withRouteIds("a", "b"),
                MockModel.policy().withLinksFrom("m", "n", "o").withLinksTo("p", "q", "r")
        );

        assertDoesNotThrow(() -> verifyItemsOrThrow(models));
    }

    @Test
    void verifyPolicies_anonymous() {
        MockModel mock = MockModel.internal().withRouteIds(UUID.randomUUID().toString(), null);

        // allow camel context models to contain anonymous routes
        assertDoesNotThrow(() -> verifyItemsOrThrow(Collections.singletonList(mock)));

        List<CamelDataModel> anonPips = Collections.singletonList(mock.withType(EntityType.PIP));
        // do NOT allow external library models to contain anonymous routes
        assertThrows(InvalidItemDefinitionException.class, () -> verifyItemsOrThrow(anonPips));

        // process models from external source may not contain anonymous routes (may complicate replacement)
        List<CamelDataModel> anonPolicies = Collections.singletonList(mock.withType(EntityType.POLICY));
        assertThrows(InvalidItemDefinitionException.class, () -> verifyItemsOrThrow(anonPolicies));
    }

    /**
     * Verifier should catch route ID conflicts in all provided models (or camel will).
     */
    @ParameterizedTest
    @EnumSource(names = {"PIP", "POLICY"})
    @NullSource
    void verifyPolicies_routeIdConflicts_internal(@Nullable EntityType type) {
        List<CamelDataModel> modelsWithInternalRouteIdConflicts = listOfCamelModels(
                MockModel.ofType(type).withRouteIds("a", "b"),
                MockModel.ofType(type).withRouteIds("b", "z")
        );

        assertThrows(InvalidItemDefinitionException.class, () -> verifyItemsOrThrow(modelsWithInternalRouteIdConflicts));
    }

    /**
     * Verifier should catch route id conflicts between external models and internal models that may be valid on
     * their own.
     */
    @Test
    void verifyPolicies_routeIdConflicts_cross_context() {
        List<CamelDataModel> internal = listOfCamelModels(
                MockModel.internal().withRouteIds("a", "b"),
                MockModel.internal().withRouteIds("c", "d")
        );

        List<CamelDataModel> policy = listOfCamelModels(
                MockModel.policy().withRouteIds("x", "b"),
                MockModel.policy().withRouteIds("c", "y")
        );

        List<CamelDataModel> pip = listOfCamelModels(
                MockModel.pip().withRouteIds("a", "x"),
                MockModel.pip().withRouteIds("y", "d")
        );

        List<CamelDataModel> internalAndPolicyConflict = Stream.concat(internal.stream(), policy.stream()).collect(Collectors.toList());
        assertThrows(InvalidItemDefinitionException.class, () -> verifyItemsOrThrow(internalAndPolicyConflict));

        List<CamelDataModel> policyAndPipConflict = Stream.concat(policy.stream(), pip.stream()).collect(Collectors.toList());
        assertThrows(InvalidItemDefinitionException.class, () -> verifyItemsOrThrow(policyAndPipConflict));

        List<CamelDataModel> internalAndPipConflict = Stream.concat(pip.stream(), internal.stream()).collect(Collectors.toList());
        assertThrows(InvalidItemDefinitionException.class, () -> verifyItemsOrThrow(internalAndPipConflict));

    }


    /**
     * Prevent conflicting endpoint uri exceptions by enforcing uniqueness of 'from' uris.
     */
    @ParameterizedTest
    @EnumSource(names = {"PIP", "POLICY"})
    @NullSource
    void verifyPolicies_endpointUriConflicts_internal(@Nullable EntityType type) {
        List<CamelDataModel> crossInternal = listOfCamelModels(
                MockModel.ofType(type).withLinksFrom("a", "b", "c"),
                MockModel.ofType(type).withLinksFrom("c", "d", "e")
        );
        assertThrows(InvalidItemDefinitionException.class, () -> verifyItemsOrThrow(crossInternal));

        List<CamelDataModel> internal = listOfCamelModels(MockModel.ofType(type).withLinksFrom("a", "b", "a"));
        assertThrows(InvalidItemDefinitionException.class, () -> verifyItemsOrThrow(internal));

    }

    @Test
    void verifyPolicies_endpointUriConflicts_cross_context() {
        List<CamelDataModel> internal = listOfCamelModels(
                MockModel.internal().withLinksFrom("a", "b"),
                MockModel.internal().withLinksFrom("c", "d")
        );

        List<CamelDataModel> policy = listOfCamelModels(
                MockModel.policy().withLinksFrom("x", "b"),
                MockModel.policy().withLinksFrom("c", "y")
        );

        List<CamelDataModel> pip = listOfCamelModels(
                MockModel.pip().withLinksFrom("a", "x"),
                MockModel.pip().withLinksFrom("y", "d")
        );

        List<CamelDataModel> internalAndPolicyConflict = Stream.concat(internal.stream(), policy.stream()).collect(Collectors.toList());
        assertThrows(InvalidItemDefinitionException.class, () -> verifyItemsOrThrow(internalAndPolicyConflict));

        List<CamelDataModel> policyAndPipConflic = Stream.concat(policy.stream(), pip.stream()).collect(Collectors.toList());
        assertThrows(InvalidItemDefinitionException.class, () -> verifyItemsOrThrow(policyAndPipConflic));

        List<CamelDataModel> internalAndPipConflic = Stream.concat(pip.stream(), internal.stream()).collect(Collectors.toList());
        assertThrows(InvalidItemDefinitionException.class, () -> verifyItemsOrThrow(internalAndPipConflic));

    }

    @Test
    void verifyPolicies_endpointUriConflicts_cross_context_external() {
        List<CamelDataModel> caseA = listOfCamelModels(
                MockModel.policy().withLinksFrom("a", "b"),
                MockModel.policy().withLinksFrom("c", "d"),
                MockModel.pip().withLinksFrom("a", "q", "r")
        );
        assertThrows(InvalidItemDefinitionException.class, () -> verifyItemsOrThrow(caseA));

        List<CamelDataModel> caseB = listOfCamelModels(
                MockModel.pip().withLinksFrom("a", "b"),
                MockModel.pip().withLinksFrom("c", "d"),
                MockModel.policy().withLinksFrom("a", "q", "r")
        );
        assertThrows(InvalidItemDefinitionException.class, () -> verifyItemsOrThrow(caseB));

    }

    @Test
    void verifyPolicies_outbound_links_from_policies() {
        List<CamelDataModel> policyToInternal = listOfCamelModels(
                MockModel.policy().withLinksTo("c"),
                MockModel.internal().withLinksFrom("c")
        );
        assertDoesNotThrow(() -> verifyItemsOrThrow(policyToInternal));

        List<CamelDataModel> policyToPip = listOfCamelModels(
                MockModel.policy().withLinksTo("c"),
                MockModel.pip().withLinksFrom("c")
        );
        assertDoesNotThrow(() -> verifyItemsOrThrow(policyToPip));

        List<CamelDataModel> policyToPolicy = listOfCamelModels(
                MockModel.policy().withLinksTo("c"),
                MockModel.policy().withLinksFrom("c")
        );
        assertThrows(InvalidItemDefinitionException.class, () -> verifyItemsOrThrow(policyToPolicy));

    }

    @Test
    void verifyPolicies_outbound_links_from_internal() {
        List<CamelDataModel> internalToPolicyModels = listOfCamelModels(
                MockModel.internal().withLinksTo("c"),
                MockModel.policy().withLinksFrom("c")
        );
        assertThrows(InvalidItemDefinitionException.class, () -> verifyItemsOrThrow(internalToPolicyModels));

        List<CamelDataModel> internalToPipModels = listOfCamelModels(
                MockModel.internal().withLinksTo("c"),
                MockModel.pip().withLinksFrom("c")
        );
        assertThrows(InvalidItemDefinitionException.class, () -> verifyItemsOrThrow(internalToPipModels));

        List<CamelDataModel> internalModels = listOfCamelModels(
                MockModel.internal().withLinksTo("c"),
                MockModel.internal().withLinksFrom("c")
        );
        assertDoesNotThrow(() -> verifyItemsOrThrow(internalModels));

    }

    @Test
    void verifyPolicies_illegal_outbound_links_from_pips() {
        List<CamelDataModel> pipToPolicyModels = listOfCamelModels(
                MockModel.pip().withLinksTo("c"),
                MockModel.policy().withLinksFrom("c")
        );
        assertThrows(InvalidItemDefinitionException.class, () -> verifyItemsOrThrow(pipToPolicyModels));

        List<CamelDataModel> pipToInternalModels = listOfCamelModels(
                MockModel.pip().withLinksTo("c"),
                MockModel.internal().withLinksFrom("c")
        );
        assertThrows(InvalidItemDefinitionException.class, () -> verifyItemsOrThrow(pipToInternalModels));

        List<CamelDataModel> pipToPip = listOfCamelModels(
                MockModel.pip().withLinksTo("c"),
                MockModel.pip().withLinksFrom("c")
        );
        assertDoesNotThrow(() -> verifyItemsOrThrow(pipToPip));
    }
    @Test
    void verifyPolicies_illegal_P2P() {
        List<CamelDataModel> childPolicyFlagTrueUsingP2P = listOfCamelModels(
                MockModel.policy().withLinksTo("P2P:policyA/1.0").withRouteIds("policyB_1.0?childPolicy=true")
        );
        assertThrows(InvalidItemDefinitionException.class, () -> verifyItemsOrThrow(childPolicyFlagTrueUsingP2P));
    }
}
