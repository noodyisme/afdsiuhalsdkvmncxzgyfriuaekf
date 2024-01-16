package com.capitalone.identity.identitybuilder.policycore.camel.components.profilerefid;

import org.apache.camel.Endpoint;
import org.apache.camel.support.DefaultComponent;

import java.util.Map;

import static java.lang.String.format;

public class ProfileRefIdComponent extends DefaultComponent {

    @Override
    protected Endpoint createEndpoint(String uri, String remaining, Map<String, Object> parameters) throws Exception {
        if (!ProfileRefIdOperation.EXTRACT.name().toLowerCase().equals(remaining)) {
            throw new IllegalArgumentException(format("Invalid uri %s - valid syntax cof-profile-ref-id:extract", uri));
        }

        final Endpoint endpoint = new ProfileRefIdEndpoint(uri, ProfileRefIdOperation.valueOf(remaining.toUpperCase()), this);
        setProperties(endpoint, parameters);
        return endpoint;
    }

}
