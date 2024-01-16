package com.capitalone.identity.identitybuilder.policycore.camel.components.profilerefid;

import org.apache.camel.Exchange;
import org.apache.camel.support.DefaultProducer;
import org.apache.logging.log4j.util.Strings;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ProfileRefIdProducer extends DefaultProducer {

    private static final String DELIMITER = "~~";
    private static final Pattern SSO_ID = Pattern.compile("^[\\w]+$");
    private static final Pattern SSO_ID_WITH_IDENTIFIER = Pattern.compile("(escId|ssoId)=([\\w]+)");

    public ProfileRefIdProducer(ProfileRefIdEndpoint endpoint) {
        super(endpoint);
    }

    @Override
    public void process(Exchange exchange) throws Exception {
        final String srcProfileRefId = exchange.getMessage().getHeader("srcProfileRefId", String.class);

        if (srcProfileRefId == null) {
            throw new IllegalArgumentException("required camel header 'srcProfileRefId' missing");
        }

        final String ssoId = doExtractSsoId(srcProfileRefId);
        final Map<String, Object> ssoIdExtractedFields = doBuildSsoIdExtractedFields(ssoId);

        exchange.getMessage().setHeader("profileRefIdExtractedFields", ssoIdExtractedFields);
    }

    private String doExtractSsoId(final String profileRefId) {
        if (Strings.isBlank(profileRefId)) {
            return null;
        }

        if (SSO_ID.matcher(profileRefId).matches()) {
            return profileRefId;
        }

        return Arrays.stream(profileRefId.split(DELIMITER))
                .map(value -> {
                    final Matcher matcher = SSO_ID_WITH_IDENTIFIER.matcher(value);
                    return matcher.matches() ? matcher.group(2) : null;
                }).filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    private Map<String, Object> doBuildSsoIdExtractedFields(final String ssoId) {
        final Map<String, Object> ssoIdExtractedFields = new HashMap<>();
        ssoIdExtractedFields.put("ssoId", ssoId);

        return ssoIdExtractedFields;
    }

}
