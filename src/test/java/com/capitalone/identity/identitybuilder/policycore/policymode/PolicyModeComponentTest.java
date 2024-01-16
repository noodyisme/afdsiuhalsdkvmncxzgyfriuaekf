package com.capitalone.identity.identitybuilder.policycore.policymode;

import com.capitalone.identity.identitybuilder.policycore.configmanagement.ConfigManagementService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;

import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class PolicyModeComponentTest {
    @Mock
    static ConfigManagementService configManagementService;

    @BeforeAll
    static void setupComponents() {
        configManagementService = mock(ConfigManagementService.class);
    }

    @Test
    void testValidEndpoint() {
        PolicyModeComponent policyModeComponent = new PolicyModeComponent(configManagementService, true, "test");

        PolicyModeEndpoint endpoint = (PolicyModeEndpoint) assertDoesNotThrow(()->policyModeComponent.createEndpoint("","",new HashMap<>()));
        assertEquals(configManagementService, endpoint.getConfigManagementService());
        assertTrue(endpoint.isMockFeatureEnabled());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "prod",
            "Prod",
            "PROD",
            "production",
            "prod-env",
            "env_prod",
            " prod "
    })
    void testValidProdEndpoint(String environmentName) {
        PolicyModeComponent policyModeComponent = new PolicyModeComponent(configManagementService, true, environmentName);
        PolicyModeEndpoint endpoint = (PolicyModeEndpoint) assertDoesNotThrow(()->policyModeComponent.createEndpoint("","",new HashMap<>()));
        assertEquals(configManagementService, endpoint.getConfigManagementService());
        assertFalse(endpoint.isMockFeatureEnabled());
    }
}
