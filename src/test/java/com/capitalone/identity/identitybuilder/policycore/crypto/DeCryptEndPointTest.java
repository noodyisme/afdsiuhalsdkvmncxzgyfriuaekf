package com.capitalone.identity.identitybuilder.policycore.crypto;

import org.apache.camel.Component;
import org.apache.camel.Processor;
import org.apache.camel.Producer;
import org.apache.camel.test.spring.junit5.CamelSpringTestSupport;
import org.apache.commons.collections4.map.PassiveExpiringMap;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.core.env.Environment;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeCryptEndPointTest extends CamelSpringTestSupport {
    DeCryptEndPoint endpoint;
    @Mock
    Environment environment;

    @Test
    void createProducerTest() {
        Component component = new DecryptionComponent();

        Map<String, String> cache = Collections.synchronizedMap(new PassiveExpiringMap<>(900, TimeUnit.SECONDS));
        when(environment.getActiveProfiles()).thenReturn(new String[]{"local"});
        Processor processor = new DeCryptProcessor(new DeCryptService(environment, "v1", cache));
        endpoint = new DeCryptEndPoint("url", component, processor);
        Producer producer = endpoint.createProducer();
        assertEquals("url", producer.getEndpoint().getEndpointUri());
        assertTrue(endpoint.isLenientProperties());
        assertTrue(endpoint.isSingletonProducer());
        assertEquals(endpoint.hashCode(), endpoint.hashCode());
    }


    @Test
    void createConsumerTest() {
        Component component = new DecryptionComponent();
        Map<String, String> cache = Collections.synchronizedMap(new PassiveExpiringMap<>(900, TimeUnit.SECONDS));
        when(environment.getActiveProfiles()).thenReturn(new String[]{"local"});
        Processor processor = new DeCryptProcessor(new DeCryptService(environment, "v1", cache));
        endpoint = new DeCryptEndPoint("url", component, processor);
        assertThrows(UnsupportedOperationException.class, () -> endpoint.createConsumer(processor));
    }

    @Override
    protected AbstractApplicationContext createApplicationContext() {
        return new AnnotationConfigApplicationContext("com.capitalone.identity.identitybuilder.policycore.crypto");
    }
}