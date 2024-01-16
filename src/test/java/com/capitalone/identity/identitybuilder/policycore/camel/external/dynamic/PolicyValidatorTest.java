package com.capitalone.identity.identitybuilder.policycore.camel.external.dynamic;

import com.capitalone.identity.identitybuilder.policycore.camel.external.InvalidItemDefinitionException;
import com.capitalone.identity.identitybuilder.policycore.camel.external.model.CamelDataModel;
import com.capitalone.identity.identitybuilder.policycore.camel.external.model.CamelDataModelImpl;
import org.apache.camel.CamelContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atMost;
import static org.mockito.Mockito.mockStatic;

@ExtendWith(MockitoExtension.class)
class PolicyValidatorTest {

    private static List<CamelDataModel> provideInvalidInternalParams() {
        return Arrays.asList(
                MockModel.internal().withRouteIds("a", "a"),
                MockModel.internal().withRouteIds("a", "b", "b"),
                MockModel.internal().withRouteIds("c", "b", "c")
        );
    }

    private static List<CamelDataModel> provideValidInternalParams() {
        return Arrays.asList(
                MockModel.internal().withRouteIds(),
                MockModel.internal().withRouteIds("a"),
                MockModel.internal().withRouteIds("a", "b", "c"),
                // allow anonymous routeIDs for internal camel routes
                MockModel.internal().withRouteIds("a", null, "c"),
                MockModel.internal().withRouteIds("a"),
                MockModel.internal().withLinksTo("r", "r")
        );
    }

    @Mock
    CamelContext camelContext;

    @Test
    void construct_order_fail() {
        PolicyValidator policyValidator = new PolicyValidator();
        MockModel model = MockModel.policy().withRouteIds("a");
        assertThrows(IllegalStateException.class, () -> policyValidator.addModel(model));
    }

    @Test
    void construct_order_ok() {
        try (MockedStatic<DynamicPolicyHelper> mocked = mockStatic(DynamicPolicyHelper.class)) {
            MockedStatic.Verification getRouteDefinitionVerification = getRouteDefinitionVerification();
            mocked.when(getRouteDefinitionVerification).then((Answer<List>) invocation -> Collections.emptyList());
            mocked.verify(getRouteDefinitionVerification, atMost(1));
            mocked.verifyNoMoreInteractions();

            PolicyValidator policyValidator = new PolicyValidator();
            CamelDataModelImpl internalModel = new CamelDataModelImpl(camelContext);
            policyValidator.populateInternalModel(internalModel);
            MockModel model = MockModel.policy().withRouteIds("a");
            assertDoesNotThrow(() -> policyValidator.addModel(model));
        }
    }

    private PolicyValidator newValidator(CamelDataModel model) {
        PolicyValidator validator = new PolicyValidator();
        validator.populateInternalModel(model);
        return validator;
    }

    @ParameterizedTest
    @MethodSource("provideValidInternalParams")
    void create_valid(CamelDataModel model) {
        assertDoesNotThrow(() -> newValidator(model));
    }

    @ParameterizedTest
    @MethodSource("provideInvalidInternalParams")
    void create_invalid(CamelDataModel model) {
        assertThrows(InvalidItemDefinitionException.class, () -> newValidator(model));
    }

    @Test
    void id_conflict_pip() {
        PolicyValidator validator = newValidator(MockModel.internal().withRouteIds("a", "b", "c"));
        Assertions.assertDoesNotThrow(() -> validator.addModel(MockModel.pip().withRouteIds("d", "e")));
        Assertions.assertDoesNotThrow(() -> validator.addModel(MockModel.policy().withRouteIds("f")));

        Arrays.asList(
                MockModel.pip().withRouteIds((String) null),
                // conflict w/ self route ID
                MockModel.pip().withRouteIds("x", "y", "y"),
                // conflict w/ internal route ID
                MockModel.pip().withRouteIds("b"),
                // conflict w/ library route ID
                MockModel.pip().withRouteIds("d"),
                // conflict w/ process route ID
                MockModel.pip().withRouteIds("f")
        ).forEach(model -> assertThrows(InvalidItemDefinitionException.class, () -> validator.addModel(model)));
    }

    @Test
    void id_conflict_policy() {
        PolicyValidator validator = newValidator(MockModel.internal().withRouteIds("a", "b", "c"));
        Assertions.assertDoesNotThrow(() -> validator.addModel(MockModel.pip().withRouteIds("d", "e")));
        Assertions.assertDoesNotThrow(() -> validator.addModel(MockModel.policy().withRouteIds("f")));

        Arrays.asList(
                MockModel.policy().withRouteIds((String) null),
                // conflict w/ self route ID
                MockModel.policy().withRouteIds("x", "y", "y"),
                // conflict w/ internal route ID
                MockModel.policy().withRouteIds("b"),
                // conflict w/ library route ID
                MockModel.policy().withRouteIds("d"),
                // conflict w/ process route ID
                MockModel.policy().withRouteIds("f")
        ).forEach(model -> assertThrows(InvalidItemDefinitionException.class, () -> validator.addModel(model)));
    }

    @Test
    void add_pip_support_after_policy() {
        PolicyValidator validator = newValidator(MockModel.internal());
        Assertions.assertDoesNotThrow(() -> validator.addModel(MockModel.policy().withLinksTo("a")));
        Assertions.assertDoesNotThrow(() -> validator.addModel(MockModel.pip().withLinksFrom("a")));
    }

    @Test
    void add_pip_support_before_policy() {
        PolicyValidator validator = newValidator(MockModel.internal());
        Assertions.assertDoesNotThrow(() -> validator.addModel(MockModel.pip().withLinksFrom("a")));
        Assertions.assertDoesNotThrow(() -> validator.addModel(MockModel.policy().withLinksTo("a")));
    }

    @Test
    void test_conflicting_model_removal() {
        Map<CamelDataModel, CamelDataModel> testData = new HashMap<>();
        //routeID conflicts
        testData.put(MockModel.policy().withRouteIds("a"), MockModel.policy().withRouteIds("a"));
        testData.put(MockModel.policy().withRouteIds("a"), MockModel.pip().withRouteIds("a"));
        testData.put(MockModel.pip().withRouteIds("a"), MockModel.policy().withRouteIds("a"));
        testData.put(MockModel.pip().withRouteIds("a"), MockModel.pip().withRouteIds("a"));
        //endpoint conflicts
        testData.put(MockModel.policy().withLinksFrom("a"), MockModel.policy().withLinksFrom("a"));
        testData.put(MockModel.policy().withLinksFrom("a"), MockModel.pip().withLinksFrom("a"));
        testData.put(MockModel.pip().withLinksFrom("a"), MockModel.policy().withLinksFrom("a"));
        testData.put(MockModel.pip().withLinksFrom("a"), MockModel.pip().withLinksFrom("a"));
        // name conflicts
        testData.put(MockModel.policy().withRouteIds("a").withName("NAME"),
                MockModel.policy().withRouteIds("b").withName("NAME"));

        testData.forEach((existing, conflicting) -> {
            PolicyValidator validator = newValidator(MockModel.internal());
            Assertions.assertDoesNotThrow(() -> validator.addModel(existing));
            assertThrows(InvalidItemDefinitionException.class, () -> validator.addModel(conflicting));
            assertTrue(validator.removeModel(existing));
            Assertions.assertDoesNotThrow(() -> validator.addModel(conflicting));
        });

    }

    @Test
    void test_model_remove_result() {
        MockModel a = MockModel.policy().withRouteIds("a");
        MockModel b = MockModel.pip().withRouteIds("b");

        PolicyValidator validator = newValidator(MockModel.internal());
        validator.addModel(a);

        assertFalse(validator.removeModel(b));
        assertTrue(validator.removeModel(a));
    }

    @Test
    void add_prohibited_from_uri_support() {

        PolicyValidator validator = newValidator(MockModel.internal());
        MockModel policyA = MockModel.policy().withLinksTo("a");
        MockModel pipX = MockModel.policy().withLinksTo("x");

        validator.addModel(policyA).addModel(pipX);

        MockModel policyO = MockModel.policy().withLinksFrom("a");
        MockModel policyP = MockModel.policy().withLinksFrom("x");

        // cannot add a policy that supports a 'to' uri from another policy
        assertThrows(InvalidItemDefinitionException.class, () -> validator.addModel(policyO));
        assertThrows(InvalidItemDefinitionException.class, () -> validator.addModel(policyP));

    }

    /**
     * Verifies the validator keeps track of multiple to routes for policy and pip
     */
    @Test
    void add_prohibited_from_uri_support_multiple() {

        PolicyValidator validator = newValidator(MockModel.internal());
        MockModel policyM = MockModel.policy().withLinksTo("a");
        MockModel policyN = MockModel.policy().withLinksTo("a");
        MockModel pipX = MockModel.policy().withLinksTo("c");
        MockModel pipY = MockModel.policy().withLinksTo("c");

        validator.addModel(policyM)
                .addModel(policyN)
                .addModel(pipX)
                .addModel(pipY);


        MockModel policyO = MockModel.policy().withLinksFrom("a");
        MockModel policyZ = MockModel.policy().withLinksFrom("c");
        // cannot add a policy that supports a 'to' uri from another policy
        assertThrows(InvalidItemDefinitionException.class, () -> validator.addModel(policyO));
        assertThrows(InvalidItemDefinitionException.class, () -> validator.addModel(policyZ));

        validator.removeModel(policyM);
        validator.removeModel(pipX);
        assertThrows(InvalidItemDefinitionException.class, () -> validator.addModel(policyO));
        assertThrows(InvalidItemDefinitionException.class, () -> validator.addModel(policyZ));

        // succeeds when all policies are removed
        validator.removeModel(policyN);
        validator.removeModel(pipY);
        Assertions.assertDoesNotThrow(() -> validator.addModel(policyO));
        Assertions.assertDoesNotThrow(() -> validator.addModel(policyZ));
    }

    @Test
    void removePolicy() {
        PolicyValidator validator = newValidator(MockModel.internal().withRouteIds("a", "b", "c"));
        Assertions.assertDoesNotThrow(() -> validator.addModel(MockModel.pip().withRouteIds("d", "e")));
        Assertions.assertDoesNotThrow(() -> validator.addModel(MockModel.policy().withRouteIds("f")));
    }

    @Test
    void getChildPolicies() {
        String expectedName = "policy:batman";
        Set<String> expectedNameList = new HashSet<>();
        expectedNameList.add(expectedName);

        CamelDataModel model = MockModel.policy().withChildPolicyUri(expectedName);

        PolicyValidator validator = new PolicyValidator();
        validator.populateInternalModel(MockModel.internal());
        validator.addModel(model);

        assertEquals(expectedNameList, validator.getChildPolicies());

        validator.removeModel(model);

        assertEquals(0, validator.getChildPolicies().size());
    }

    @Test
    void getChildPoliciesNoChildPolicyUri() {
        CamelDataModel model = MockModel.policy();

        PolicyValidator validator = new PolicyValidator();
        validator.populateInternalModel(MockModel.internal());
        validator.addModel(model);

        assertEquals(0, validator.getChildPolicies().size());

        validator.removeModel(model);

        assertEquals(0, validator.getChildPolicies().size());
    }

    private MockedStatic.Verification getRouteDefinitionVerification() {
        return () -> DynamicPolicyHelper.getRouteDefinitions(any());
    }

}
