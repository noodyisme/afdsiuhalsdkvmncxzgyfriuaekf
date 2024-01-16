package com.capitalone.identity.identitybuilder.policycore.crypto;

import org.apache.camel.Endpoint;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DeCryptProducerTest {

    DeCryptProducer producer;
    @Mock
    DeCryptProcessor processor;

    @Test
    void process() throws Exception {
        Endpoint endpoint = new DeCryptEndPoint("url", new DecryptionComponent(), processor);
        producer = new DeCryptProducer(endpoint, processor);
        producer.process(endpoint.createExchange());
        Assertions.assertEquals("url", producer.getEndpoint().getEndpointUri());
    }
}