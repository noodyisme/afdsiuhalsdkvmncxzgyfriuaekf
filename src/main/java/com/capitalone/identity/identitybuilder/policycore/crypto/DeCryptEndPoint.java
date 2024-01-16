package com.capitalone.identity.identitybuilder.policycore.crypto;

import lombok.EqualsAndHashCode;
import org.apache.camel.Component;
import org.apache.camel.Consumer;
import org.apache.camel.Processor;
import org.apache.camel.Producer;
import org.apache.camel.spi.UriEndpoint;
import org.apache.camel.support.DefaultEndpoint;

@UriEndpoint(scheme = "crypto-cos", title = "crypto-cos", syntax = "crypto-cos:decrypt")
@EqualsAndHashCode(callSuper = true)
class DeCryptEndPoint extends DefaultEndpoint {

    private final Processor processor;

    public DeCryptEndPoint(String endpointUri, Component component, Processor processor) {
        super(endpointUri, component);
        this.processor = processor;
    }

    @Override
    public Producer createProducer() {
        return new DeCryptProducer(this, this.processor);
    }

    @Override
    public Consumer createConsumer(Processor processor) {
        throw new UnsupportedOperationException("The CryptoEndPoint endpoint doesn't support consumers.");
    }

    @Override
    public boolean isLenientProperties() {
        return true;
    }

    @Override
    public boolean isSingletonProducer() {
        return true;
    }

}
