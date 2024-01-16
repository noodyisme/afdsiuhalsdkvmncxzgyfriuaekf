package com.capitalone.identity.identitybuilder.policycore.configmanagement;

import com.capitalone.identity.identitybuilder.configmanagement.ConfigManagementModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class ConfigManagementServiceTest {

    ConfigManagementService service;

    @Mock
    ConfigManagementModel configuration;

    private final String policyId = "abc";

    @BeforeEach
    void setUp() {
        service = new ConfigManagementService();
    }

    @Test
    void setPolicyConfiguration() {
        assertDoesNotThrow(() -> service.setPolicyConfiguration(policyId, configuration));
        assertDoesNotThrow(() -> service.setPolicyConfiguration("random", null));
    }

    @Test
    void getPolicyConfiguration() {
        service.setPolicyConfiguration(policyId, configuration);
        assertEquals(configuration, service.getPolicyConfiguration(policyId));
    }

    @Test
    void deletePolicyConfiguration() {
        service.setPolicyConfiguration(policyId, configuration);
        assertEquals(configuration, service.deletePolicyConfiguration(policyId));
        assertNull(service.getPolicyConfiguration(policyId));

    }
}
