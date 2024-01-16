package com.capitalone.identity.identitybuilder.policycore.dmnrules;


import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.support.DefaultProducer;

public class DmnRulesProducer extends DefaultProducer {

    private Processor processor;

    public DmnRulesProducer(DmnRulesEndpoint endpoint, Processor processor) {
        super(endpoint);
        this.processor = processor;
    }

    @Override
    public void process(Exchange exchange) throws Exception {
        this.processor.process(exchange);
    }

}
