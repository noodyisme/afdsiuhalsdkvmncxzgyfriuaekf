package com.capitalone.identity.identitybuilder.policycore.crypto;

import com.capitalone.identity.identitybuilder.policycore.utils.CamelSpringBootContextAwareTest;
import org.apache.camel.CamelContext;
import org.apache.camel.Endpoint;
import org.apache.camel.impl.DefaultCamelContext;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

@CamelSpringBootContextAwareTest(contextConfigClasses = DeCryptComponentTest.ContextConfigCamel.class,
        excludeFromComponentScan = @Filter(type = FilterType.ASPECTJ,
                pattern = "com.capitalone.identity.identitybuilder.policycore.crypto.*"))
public class DeCryptComponentTest {

    private static DecryptionComponent decryptionComponent;

    @BeforeAll
    public static void setComponents() {
        decryptionComponent = new DecryptionComponent();
    }

    @Test
    void testWithParameters() throws Exception {
        // All the parameters passed to the endpoint
        Map<String, Object> parameters = new HashMap<>();
        Endpoint endpoint = decryptionComponent.createEndpoint("crypto-cos", "decrypt", parameters);
        assertEquals("crypto-cos", endpoint.getEndpointUri());
    }


    @Test
    void test_ErrorIfEndPointNotFound() {
        // All the parameters passed to the endpoint
        Map<String, Object> parameters = new HashMap<>();

        assertThrows(
                IllegalArgumentException.class,
                () -> decryptionComponent.createEndpoint("crypto-cos", "encrypt", parameters),
                "path not matching");
    }

    @Configuration
    public static class ContextConfigCamel {

        @Bean
        protected CamelContext createCamelContext() {
            CamelContext camelContext = new DefaultCamelContext();
            DeCryptService devExchangeDAO = mock(DeCryptService.class);
            lenient().when(devExchangeDAO.decrypt(anyString(), any())).thenReturn("decryptedImage");
            return camelContext;
        }

    }
}
