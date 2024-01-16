package com.capitalone.identity.identitybuilder.policycore.camel.components.profilerefid;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.apache.camel.Consumer;
import org.apache.camel.Processor;
import org.apache.camel.Producer;
import org.apache.camel.spi.UriEndpoint;
import org.apache.camel.support.DefaultEndpoint;

@UriEndpoint(
        firstVersion = "1.17.0-SNAPSHOT",
        scheme = "cof-profile-ref-id",
        title = "ProfileRefIdEndpoint",
        syntax="cof-profile-ref-id:operation")
@EqualsAndHashCode(callSuper = true)
public class ProfileRefIdEndpoint extends DefaultEndpoint {

    private static final String NO_CONSUMER_SUPPORT_ERROR_MESSAGE = "ProfileRefIdComponent does not support consumers";

    @Getter
    private final ProfileRefIdOperation profileRefIdOperation;

    public ProfileRefIdEndpoint(String uri, ProfileRefIdOperation profileRefIdOperation, ProfileRefIdComponent component) {
        super(uri, component);
        this.profileRefIdOperation = profileRefIdOperation;
    }

    @Override
    public Producer createProducer() throws Exception {
        return new ProfileRefIdProducer(this);
    }

    @Override
    public Consumer createConsumer(Processor processor) throws Exception {
        throw new UnsupportedOperationException(NO_CONSUMER_SUPPORT_ERROR_MESSAGE);
    }

}
