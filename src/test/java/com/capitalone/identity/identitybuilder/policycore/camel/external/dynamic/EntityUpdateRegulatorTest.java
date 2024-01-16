package com.capitalone.identity.identitybuilder.policycore.camel.external.dynamic;

import com.capitalone.identity.identitybuilder.ClientEnvironment;
import com.capitalone.identity.identitybuilder.model.ConfigStoreItemInfo;
import com.capitalone.identity.identitybuilder.model.EntityInfo;
import com.capitalone.identity.identitybuilder.model.EntityType;
import com.capitalone.identity.identitybuilder.model.IllegalProdPolicyUpdateException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.lenient;

class EntityUpdateRegulatorTest {
    private static final Pattern patchVersion = Pattern.compile("^.*[-_a-zA-Z\\d]+/\\d+\\.\\d+/(?<policyPatchVersion>\\d+)$");

    private static EntityInfo getPolicyEntity(String entityLocation, ConfigStoreItemInfo... infos) {
        EntityInfo mock = Mockito.mock(EntityInfo.Policy.class);
        List<ConfigStoreItemInfo> locationPrefixedItems = Arrays.stream(infos)
                .map(info -> new ConfigStoreItemInfo(entityLocation + info.getName(), info.getTag()))
                .collect(Collectors.toList());
        Matcher matcher = patchVersion.matcher(entityLocation);
        int policyPatchVersion = matcher.matches() ? Integer.parseInt(matcher.group("policyPatchVersion")) : 0;
        lenient().when(mock.getItemInfo()).thenReturn(new HashSet<>(locationPrefixedItems));
        lenient().when(mock.getLocationPrefix()).thenReturn(entityLocation);
        lenient().when(mock.getPatchVersion()).thenReturn(policyPatchVersion);
        return mock;
    }

    private static EntityInfo getTestEntity(EntityType type, ConfigStoreItemInfo... infos) {
        EntityInfo mock = EntityType.POLICY == type
                ? Mockito.mock(EntityInfo.Policy.class)
                : Mockito.mock(EntityInfo.Pip.class);
        lenient().when(mock.getItemInfo()).thenReturn(new HashSet<>(Arrays.asList(infos)));
        return mock;
    }

    @ParameterizedTest
    @EnumSource(value = EntityType.class)
    void default_checkUpdateAllowedOrThrow_nonProdContentUpdateDoesNotThrow(EntityType type) {
        EntityUpdateRegulator regulator = new EntityUpdateRegulator(ClientEnvironment.QA);

        EntityInfo infoA = getTestEntity(type,
                new ConfigStoreItemInfo("a", "a"));
        EntityInfo infoB = getTestEntity(type,
                new ConfigStoreItemInfo("a", "b"));

        assertDoesNotThrow(() -> regulator.checkUpdateAllowedOrThrow(infoA, infoB));
    }

    @Test
    void checkUpdateAllowedOrThrow_nonProdPipContentUpdateDoesNotThrow() {
        EntityUpdateRegulator regulator = new EntityUpdateRegulator(ClientEnvironment.QA);

        EntityInfo infoA = getTestEntity(EntityType.PIP,
                new ConfigStoreItemInfo("a", "a"));
        EntityInfo infoB = getTestEntity(EntityType.PIP,
                new ConfigStoreItemInfo("a", "b"));

        assertDoesNotThrow(() -> regulator.checkUpdateAllowedOrThrow(infoA, infoB));
    }

    @Test
    void checkUpdateAllowedOrThrow_nonProdPolicyContentUpdateDoesNotThrow() {
        EntityUpdateRegulator regulator = new EntityUpdateRegulator(ClientEnvironment.QA);

        EntityInfo infoA = getTestEntity(EntityType.POLICY,
                new ConfigStoreItemInfo("a", "a"));
        EntityInfo infoB = getTestEntity(EntityType.POLICY,
                new ConfigStoreItemInfo("a", "b"));

        assertDoesNotThrow(() -> regulator.checkUpdateAllowedOrThrow(infoA, infoB));
    }

    @Test
    void checkUpdateAllowedOrThrow_prodPolicyNonContentUpdateDoesNotThrow() {
        EntityUpdateRegulator regulator = new EntityUpdateRegulator(ClientEnvironment.PROD);

        EntityInfo infoA = getPolicyEntity(
                "policy_a/1.0",
                new ConfigStoreItemInfo("/process/policy_a.xml", "a"),
                new ConfigStoreItemInfo("/rules/decision_a.dmn", "a"),
                new ConfigStoreItemInfo("/config/defaults.json", "a"),
                new ConfigStoreItemInfo("/config/schema.json", "a"),
                new ConfigStoreItemInfo("/config/usecase-a.dmn", "a"));

        EntityInfo infoB = getPolicyEntity(
                "policy_a/1.0/1",
                new ConfigStoreItemInfo("/policy-metadata.json", "a"),
                new ConfigStoreItemInfo("/process/policy_a.xml", "a"),
                new ConfigStoreItemInfo("/rules/decision_a.dmn", "a"),
                new ConfigStoreItemInfo("/config/defaults.json", "a"),
                new ConfigStoreItemInfo("/config/schema.json", "a"),
                new ConfigStoreItemInfo("/config/usecase-a.dmn", "a"));

        assertDoesNotThrow(() -> regulator.checkUpdateAllowedOrThrow(infoA, infoB));
    }


    @Test
    void checkUpdateAllowedOrThrow_prodPipContentUpdateThrows() {
        EntityUpdateRegulator regulator = new EntityUpdateRegulator(ClientEnvironment.PROD);

        EntityInfo infoA = getTestEntity(EntityType.PIP,
                new ConfigStoreItemInfo("a", "a"));
        EntityInfo infoB = getTestEntity(EntityType.PIP,
                new ConfigStoreItemInfo("a", "b"));

        assertThrows(IllegalProdPolicyUpdateException.class, () -> regulator.checkUpdateAllowedOrThrow(infoA, infoB));
    }

    @Test
    void checkUpdateAllowedOrThrow_prodPolicyContentUpdateThrows() {
        EntityUpdateRegulator regulator = new EntityUpdateRegulator(ClientEnvironment.PROD);

        EntityInfo infoA = getTestEntity(EntityType.POLICY,
                new ConfigStoreItemInfo("a", "a"));
        EntityInfo infoB = getTestEntity(EntityType.POLICY,
                new ConfigStoreItemInfo("a", "b"));

        assertThrows(IllegalProdPolicyUpdateException.class, () -> regulator.checkUpdateAllowedOrThrow(infoA, infoB));
    }

    @Test
    void checkUpdateAllowedOrThrow_prodPolicyContentUpdateThrows_setDifference() {
        EntityUpdateRegulator regulator = new EntityUpdateRegulator(ClientEnvironment.PROD);

        EntityInfo infoA = getTestEntity(EntityType.POLICY,
                new ConfigStoreItemInfo("a", "a"));
        EntityInfo infoB = getTestEntity(EntityType.POLICY,
                new ConfigStoreItemInfo("a", "a"),
                new ConfigStoreItemInfo("b", "b")
        );

        assertThrows(IllegalProdPolicyUpdateException.class, () -> regulator.checkUpdateAllowedOrThrow(infoA, infoB));
        assertThrows(IllegalProdPolicyUpdateException.class, () -> regulator.checkUpdateAllowedOrThrow(infoB, infoA));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/config/schema.json",
            "/config/defaults.json",
            "/process/policy_a.xml",
            "/rules/dmn_a.dmn",
    })
    void checkUpdateAllowedOrThrow_prodContentChangesNotAllowed(String item) {
        EntityUpdateRegulator regulator = new EntityUpdateRegulator(ClientEnvironment.PROD);

        EntityInfo infoA = getPolicyEntity(
                "policy_a/1.0/0",
                new ConfigStoreItemInfo(item, "a"));
        EntityInfo infoB = getPolicyEntity(
                "policy_a/1.0/0",
                new ConfigStoreItemInfo(item, "b"));
        EntityInfo infoC = getPolicyEntity(
                "policy_a/1.0/1/",
                new ConfigStoreItemInfo(item, "c"));

        assertThrows(IllegalProdPolicyUpdateException.class, () -> regulator.checkUpdateAllowedOrThrow(infoA, infoB));
        assertThrows(IllegalProdPolicyUpdateException.class, () -> regulator.checkUpdateAllowedOrThrow(infoA, infoC));
        assertThrows(IllegalProdPolicyUpdateException.class, () -> regulator.checkUpdateAllowedOrThrow(infoB, infoA));
        assertThrows(IllegalProdPolicyUpdateException.class, () -> regulator.checkUpdateAllowedOrThrow(infoB, infoC));
        assertThrows(IllegalProdPolicyUpdateException.class, () -> regulator.checkUpdateAllowedOrThrow(infoC, infoA));
        assertThrows(IllegalProdPolicyUpdateException.class, () -> regulator.checkUpdateAllowedOrThrow(infoC, infoB));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/policy-metadata.json",
            "/config/features.json",
    })
    void checkUpdateAllowedOrThrow_prodItemContentChangesAllowed(String itemName) {
        EntityUpdateRegulator regulator = new EntityUpdateRegulator(ClientEnvironment.PROD);

        EntityInfo infoA = getPolicyEntity("policy_a/1.0/5",
                new ConfigStoreItemInfo(itemName, "a"));
        EntityInfo infoB = getPolicyEntity("policy_a/1.0/6",
                new ConfigStoreItemInfo(itemName, "b"));

        // allowed update
        assertDoesNotThrow(() -> regulator.checkUpdateAllowedOrThrow(infoA, infoB));

        // rejected for patch version being out of order
        assertThrows(IllegalProdPolicyUpdateException.class, () -> regulator.checkUpdateAllowedOrThrow(infoA, infoA));
        assertThrows(IllegalProdPolicyUpdateException.class, () -> regulator.checkUpdateAllowedOrThrow(infoB, infoA));
        assertThrows(IllegalProdPolicyUpdateException.class, () -> regulator.checkUpdateAllowedOrThrow(infoB, infoB));
    }


    @Test
    void checkUpdateAllowedOrThrow_prodConfigChangesAllowed() {
        EntityUpdateRegulator regulator = new EntityUpdateRegulator(ClientEnvironment.PROD);

        EntityInfo infoA = getPolicyEntity("policy_a/1.0/0",
                new ConfigStoreItemInfo("/config/usecase-a.json", "a"),
                new ConfigStoreItemInfo("/config/usecase-b.json", "b"));
        EntityInfo infoB = getPolicyEntity("policy_a/1.0/1",
                new ConfigStoreItemInfo("/config/usecase-a.json", "a"),
                new ConfigStoreItemInfo("/config/usecase-b.json", "b"),
                new ConfigStoreItemInfo("/config/usecase-c.json", "c"));
        EntityInfo infoC = getPolicyEntity("policy_a/1.0/2",
                new ConfigStoreItemInfo("/config/usecase-a.json", "x"),
                new ConfigStoreItemInfo("/config/usecase-b.json", "y"),
                new ConfigStoreItemInfo("/config/usecase-c.json", "z"));
        EntityInfo infoD = getPolicyEntity("policy_a/1.0/3",
                new ConfigStoreItemInfo("/config/usecase-a.json", "x"));
        assertDoesNotThrow(() -> regulator.checkUpdateAllowedOrThrow(infoA, infoB));
        assertDoesNotThrow(() -> regulator.checkUpdateAllowedOrThrow(infoB, infoC));
        assertDoesNotThrow(() -> regulator.checkUpdateAllowedOrThrow(infoC, infoD));
    }
}
