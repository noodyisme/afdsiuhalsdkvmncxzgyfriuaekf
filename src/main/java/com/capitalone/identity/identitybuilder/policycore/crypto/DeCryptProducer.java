package com.capitalone.identity.identitybuilder.policycore.crypto;

import org.apache.camel.Endpoint;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.support.DefaultProducer;

public class DeCryptProducer extends DefaultProducer {

    private final Processor processor;

    public DeCryptProducer(Endpoint endpoint, Processor processor) {
        super(endpoint);
        this.processor = processor;
    }

    @Override
    public void process(Exchange exchange) throws Exception {
        this.processor.process(exchange);
    }
}