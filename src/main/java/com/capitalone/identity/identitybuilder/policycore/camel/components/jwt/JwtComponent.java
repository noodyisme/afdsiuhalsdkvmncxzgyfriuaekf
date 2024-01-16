package com.capitalone.identity.identitybuilder.policycore.camel.components.jwt;

import com.capitalone.identity.identitybuilder.policycore.service.jws.JwsService;
import org.apache.camel.Endpoint;
import org.apache.camel.support.DefaultComponent;

import java.util.Map;
import java.util.regex.Pattern;

import static java.lang.String.format;

/**
 * Represents the component that validates JSON Web Tokens
 */
public class JwtComponent extends DefaultComponent {

    public static final String INVALID_ARGUMENTS = "Invalid uri %s - valid syntax crypto-jwt:validate|extract|generate";
    private static final Pattern PATTERN = Pattern.compile("validate|extract|generate");

    private final JwsService jwsService;

    public JwtComponent(JwsService jwsService) {
        this.jwsService = jwsService;
    }

    @Override
    protected Endpoint createEndpoint(String uri, String remaining, Map<String, Object> parameters) throws Exception {

        if (!PATTERN.matcher(remaining).matches()) {
            throw new IllegalArgumentException(format(INVALID_ARGUMENTS, uri));
        }

        Endpoint endpoint = new JwtEndpoint(uri, this, JwtOperation.valueOf(remaining.toUpperCase()), jwsService);
        setProperties(endpoint, parameters);
        return endpoint;
    }
}
