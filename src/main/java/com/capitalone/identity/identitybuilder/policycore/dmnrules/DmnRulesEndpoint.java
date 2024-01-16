package com.capitalone.identity.identitybuilder.policycore.dmnrules;

import org.apache.camel.Component;
import org.apache.camel.Consumer;
import org.apache.camel.Processor;
import org.apache.camel.Producer;
import org.apache.camel.spi.UriEndpoint;
import org.apache.camel.spi.UriParam;
import org.apache.camel.support.DefaultEndpoint;

@UriEndpoint(
        firstVersion = "1.0-SNAPSHOT",
        scheme = "DmnRulesEndpoint",
        title = "DmnRulesEndpoint",
        syntax="DmnRulesEndpoint:name")
public class DmnRulesEndpoint extends DefaultEndpoint {

    @UriParam()
    private String name;

    private Processor processor;

    public DmnRulesEndpoint(String endpointUri, Component component, Processor processor) {
        super(endpointUri, component);
        this.processor = processor;
    }

    public DmnRulesEndpoint() {
    }

    @Override
    public Producer createProducer() throws Exception {
        return new DmnRulesProducer(this, this.processor);
    }

    @Override
    public Consumer createConsumer(Processor processor) throws Exception {
        throw new UnsupportedOperationException("The DmnRulesEndpoint endpoint doesn't support consumers.");
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public boolean isLenientProperties() {
        return true;
    }

    @Override
    public boolean isSingletonProducer() {
        return true;
    }

    public Processor getProcessor() {
        return this.processor;
    }
}
