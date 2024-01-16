package com.capitalone.identity.identitybuilder.policycore.configmanagement.camel;

import com.capitalone.identity.identitybuilder.configmanagement.ConfigManagementModel;
import com.capitalone.identity.identitybuilder.policycore.configmanagement.ConfigManagementService;
import org.apache.camel.Processor;
import org.apache.camel.Producer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConfigManagementEndpointTest {

    @Mock
    ConfigManagementService service;

    @Test
    void configureProperties() {
        String endpointUri = "policy-configuration://property/config.hello?outputHeader=testOutputHeader&policyId=abc123";
        ConfigManagementEndpoint endpoint = new ConfigManagementEndpoint(endpointUri, null, service);
        Map<String, Object> properties = new HashMap<>();
        assertThrows(IllegalArgumentException.class, () -> endpoint.configureProperties(properties));

        properties.put("outputHeaderName", "headerName");
        properties.put("policyId", "abc123");
        assertDoesNotThrow(() -> endpoint.configureProperties(properties));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "policy-configuration://propertty/config.hello?outputHeader=testOutputHeader&policyId=abc123",
            "policy-configuration://property?config.hello?outputHeader=testOutputHeader&policyId=abc123",
            "policy-configuration//property/config.hello?outputHeader=testOutputHeader&policyId=abc123",
            "policy-configuration:///config.hello?outputHeader=testOutputHeader&policyId=abc123",
            "policy://property/config.hello?outputHeader=testOutputHeader&policyId=abc123",
    })
    void configurePropertiesInvalidURI(String endpointUri) {
        ConfigManagementEndpoint endpoint = new ConfigManagementEndpoint(endpointUri, null, service);
        Map<String, Object> properties = new HashMap<>();
        assertThrows(IllegalArgumentException.class, () -> endpoint.configureProperties(properties));

        properties.put("outputHeaderName", "headerName");
        properties.put("policyId", "abc123");
        RuntimeException exception = assertThrows(UnsupportedOperationException.class, () ->
                endpoint.configureProperties(properties));
        assertTrue(exception.getMessage().contains("Config Management URI calls must start with " +
                "'policy-configuration:property/'"));
    }

    @Test
    void createProducer() {
        String endpointUri = "policy-configuration://property/config.hello?outputHeader=testOutputHeader&policyId=abc123";
        ConfigManagementEndpoint endpoint = new ConfigManagementEndpoint(endpointUri, null, service);
        Map<String, Object> properties = new HashMap<>();
        properties.put("outputHeaderName", "headerName");
        properties.put("policyId", "abc123");
        endpoint.configureProperties(properties);

        when(service.getPolicyConfiguration(any())).thenReturn(null);
        assertThrows(UnsupportedOperationException.class, endpoint::createProducer);

        Map<String, Serializable> defaults = new HashMap<>();
        defaults.put("config.hello", "123");
        ConfigManagementModel config = ConfigManagementModel.newInstance(defaults, new HashMap<>());
        when(service.getPolicyConfiguration(any())).thenReturn(config);

        Producer producer = endpoint.createProducer();
        assertTrue(producer instanceof ConfigManagementProducer);
        assertDoesNotThrow(((ConfigManagementProducer) producer)::doStart);

    }

    @Test
    void createConsumer() {
        String endpointUri = "policy-configuration://property/config.hello?outputHeader=testOutputHeader&policyId=abc123";
        Processor mock = Mockito.mock(Processor.class);
        ConfigManagementEndpoint endpoint = new ConfigManagementEndpoint(endpointUri, null, service);
        assertThrows(UnsupportedOperationException.class, () -> endpoint.createConsumer(mock));
    }
}
