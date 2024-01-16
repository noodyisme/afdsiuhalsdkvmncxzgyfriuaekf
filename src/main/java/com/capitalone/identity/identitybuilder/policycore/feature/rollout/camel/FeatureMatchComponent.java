package com.capitalone.identity.identitybuilder.policycore.feature.rollout.camel;

import com.capitalone.identity.identitybuilder.policycore.feature.rollout.criteria.Criteria;
import org.apache.camel.Endpoint;
import org.apache.camel.support.DefaultComponent;
import org.apache.camel.support.LRUCacheFactory;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.util.Strings;

import java.util.Map;

import static java.lang.String.format;

public class FeatureMatchComponent extends DefaultComponent {

    public static final String INVALID_ARGUMENTS = "Invalid uri %s - valid syntax feature-match:match?outputHeaderName=<outputHeader>";
    public static final String INVALID_ARGUMENTS_MISSING_OUTPUT_HEADER_PARAM = "Invalid uri '%s' - missing 'outputHeaderName' query parameter. Valid syntax feature-match:match?outputHeaderName=<outputHeader>";

    private final Map<String, Criteria> criteriaCache = LRUCacheFactory.newLRUCache(10, 1000, false);

    @Override
    protected Endpoint createEndpoint(String uri, String remaining, Map<String, Object> parameters) throws Exception {

        if (!"match".equals(remaining)) {
            throw new IllegalArgumentException(format(INVALID_ARGUMENTS, uri));
        }

        FeatureMatchEndpoint endpoint = new FeatureMatchEndpoint(uri, this, criteriaCache);
        parameters.keySet().forEach(key -> parameters.computeIfPresent(key,
                (s, o) -> o instanceof String ? StringUtils.trim((String) o) : o));
        setProperties(endpoint, parameters);
        if (Strings.isEmpty(endpoint.getOutputHeaderName())) {
            throw new IllegalArgumentException(String.format(INVALID_ARGUMENTS_MISSING_OUTPUT_HEADER_PARAM, uri));
        }
        endpoint.setQueryBasedMatchParameters();
        return endpoint;

    }

}
