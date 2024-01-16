package com.capitalone.identity.identitybuilder.policycore.policymode;

import com.capitalone.identity.identitybuilder.configmanagement.ConfigManagementModel;
import com.capitalone.identity.identitybuilder.policycore.configmanagement.ConfigManagementService;
import org.apache.camel.Processor;
import org.apache.camel.Producer;
import org.apache.camel.support.DefaultProducer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PolicyModeEndpointTest {
    @Mock
    ConfigManagementService configManagementService;

    @Mock
    ConfigManagementModel configManagementModel;

    @Test
    void testConfigureProperties() {
        String endpointUri = "policy-mode://non-prod/policy-core.mockMode?policyId=abc123";
        PolicyModeEndpoint endpoint = new PolicyModeEndpoint(endpointUri, null, configManagementService, true, "test");
        Map<String, Object> properties = new HashMap<>();
        properties.put("policyId", "abc123");

        assertDoesNotThrow(() -> endpoint.configureProperties(properties));
        assertEquals("abc123", endpoint.getPolicyId());
    }

    @Test
    void testConfigurePropertiesInvalidURI() {
        String endpointUri = "policy-modes://non-prod/policy-core.mockMode?policyId=abc123";
        PolicyModeEndpoint endpoint = new PolicyModeEndpoint(endpointUri, null, configManagementService, true, "test");
        Map<String, Object> properties = new HashMap<>();
        properties.put("policyId", "abc123");

        UnsupportedOperationException exception = assertThrows(UnsupportedOperationException.class, () -> endpoint.configureProperties(properties));
        assertEquals("Policy Mode URI calls must start with 'policy-mode:'", exception.getMessage());
    }

    @Test
    void testCreateProducer() {
        when(configManagementService.getPolicyConfiguration(anyString())).thenReturn(configManagementModel);
        when(configManagementModel.getValue(anyString(),any(),any())).thenReturn(Optional.of(true));

        String endpointUri = "policy-mode://non-prod/policy-core.mockMode?policyId=abc123";
        PolicyModeEndpoint endpoint = new PolicyModeEndpoint(endpointUri, null, configManagementService, true, "test");
        Map<String, Object> properties = new HashMap<>();
        properties.put("policyId", "abc123");
        endpoint.configureProperties(properties);

        PolicyModeProducer producer = (PolicyModeProducer) assertDoesNotThrow(() -> endpoint.createProducer());
        assertTrue(producer.isMockModeEnabled());
    }

    @Test
    void testCreateProducerMockFeatureDisabled() {
        String endpointUri = "policy-mode://non-prod/policy-core.mockMode?policyId=abc123";
        PolicyModeEndpoint endpoint = new PolicyModeEndpoint(endpointUri, null, configManagementService, false, "test");
        Map<String, Object> properties = new HashMap<>();
        properties.put("policyId", "abc123");
        endpoint.configureProperties(properties);

        Producer producer = assertDoesNotThrow(() -> endpoint.createProducer());
        assertInstanceOf(DefaultProducer.class, producer);
        assertFalse(producer instanceof PolicyModeProducer);
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
    void testCreateProducerProdEnv(String environment) {
        String endpointUri = "policy-mode://non-prod/policy-core.mockMode?policyId=abc123";
        PolicyModeEndpoint endpoint = new PolicyModeEndpoint(endpointUri, null, configManagementService, true, environment);
        Map<String, Object> properties = new HashMap<>();
        properties.put("policyId", "abc123");
        endpoint.configureProperties(properties);

        Producer producer = assertDoesNotThrow(() -> endpoint.createProducer());
        assertInstanceOf(DefaultProducer.class, producer);
        assertFalse(producer instanceof PolicyModeProducer);
    }

    @Test
    void testCreateProducerNoConfig() {
        when(configManagementService.getPolicyConfiguration(anyString())).thenReturn(null);

        String endpointUri = "policy-mode://non-prod/policy-core.mockMode?policyId=abc123";
        PolicyModeEndpoint endpoint = new PolicyModeEndpoint(endpointUri, null, configManagementService, true, "test");
        Map<String, Object> properties = new HashMap<>();
        properties.put("policyId", "abc123");
        endpoint.configureProperties(properties);

        UnsupportedOperationException exception = assertThrows(UnsupportedOperationException.class, () -> endpoint.createProducer());
        String expectedExceptionMessage = "Policy Configuration not found for Policy Mode 'non-prod/policy-core.mockMode'" +
                " [resolved endpoint = 'policy-mode://non-prod/policy-core.mockMode?policyId=abc123']";
        assertEquals(expectedExceptionMessage,exception.getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "policy-mode://non-prod/policy-core.randomMode?policyId=abc123",
            "policy-mode://non-prod/mockMode?policyId=abc123",
            "policy-mode://prod/policy-core.mockMode?policyId=abc123",
            "policy-mode://policy-core.mockMode?policyId=abc123"
    })
    void testCreateProducerInvalidPolicyMode(String endpointUri) {
        when(configManagementService.getPolicyConfiguration(anyString())).thenReturn(configManagementModel);


        PolicyModeEndpoint endpoint = new PolicyModeEndpoint(endpointUri, null, configManagementService, true, "test");
        Map<String, Object> properties = new HashMap<>();
        properties.put("policyId", "abc123");
        endpoint.configureProperties(properties);

        UnsupportedOperationException exception = assertThrows(UnsupportedOperationException.class, () -> endpoint.createProducer());
        String mode = endpointUri.split("://")[1].split("\\?")[0];
        String expectedExceptionMessage = String.format(
                "Policy Mode provided is invalid [mode = '%s', resolved endpoint = '%s']"
                , mode, endpointUri
        );
        assertEquals(expectedExceptionMessage,exception.getMessage());
    }

    @Test
    void testCreateProducerNoConfigProperty() {
        when(configManagementService.getPolicyConfiguration(anyString())).thenReturn(configManagementModel);
        when(configManagementModel.getValue(anyString(),any(),any())).thenReturn(Optional.empty());

        String endpointUri = "policy-mode://non-prod/policy-core.mockMode?policyId=abc123";
        PolicyModeEndpoint endpoint = new PolicyModeEndpoint(endpointUri, null, configManagementService, true, "test");
        Map<String, Object> properties = new HashMap<>();
        properties.put("policyId", "abc123");
        endpoint.configureProperties(properties);

        UnsupportedOperationException exception = assertThrows(UnsupportedOperationException.class, () -> endpoint.createProducer());
        String expectedExceptionMessage = "Policy Configuration is invalid for Policy Mode 'non-prod/policy-core.mockMode'" +
                " [expected property name: 'policy-core.mockMode', expected property type: boolean, resolved endpoint = " +
                "'policy-mode://non-prod/policy-core.mockMode?policyId=abc123']";

        assertEquals(expectedExceptionMessage,exception.getMessage());
    }

    @Test
    void testCreateProducerInvalidConfigType() {
        when(configManagementService.getPolicyConfiguration(anyString())).thenReturn(configManagementModel);
        when(configManagementModel.getValue(anyString(),any(),any())).thenReturn(Optional.of("StringValue"));

        String endpointUri = "policy-mode://non-prod/policy-core.mockMode?policyId=abc123";
        PolicyModeEndpoint endpoint = new PolicyModeEndpoint(endpointUri, null, configManagementService, true, "test");
        Map<String, Object> properties = new HashMap<>();
        properties.put("policyId", "abc123");
        endpoint.configureProperties(properties);

        UnsupportedOperationException exception = assertThrows(UnsupportedOperationException.class, () -> endpoint.createProducer());
        String expectedExceptionMessage = "Policy Configuration is invalid for Policy Mode 'non-prod/policy-core.mockMode'" +
                " [expected property name: 'policy-core.mockMode', expected property type: boolean, resolved endpoint = " +
                "'policy-mode://non-prod/policy-core.mockMode?policyId=abc123']";

        assertEquals(expectedExceptionMessage,exception.getMessage());
    }

    @Test
    void testCreateConsumer() {
        String endpointUri = "policy-mode://non-prod/policy-core.mockMode?policyId=abc123";
        PolicyModeEndpoint endpoint = new PolicyModeEndpoint(endpointUri, null, configManagementService, true, "test");
        Processor mockProcessor = Mockito.mock(Processor.class);
        UnsupportedOperationException exception = assertThrows(UnsupportedOperationException.class,
                () -> endpoint.createConsumer(mockProcessor));
        assertEquals("The PolicyMode endpoint doesn't support consumers.", exception.getMessage());
    }
}
