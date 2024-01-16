package com.capitalone.identity.identitybuilder.policycore.dmnrules;

import com.capitalone.identity.identitybuilder.policycore.service.dmn.PolicyRulesProcessor;
import lombok.AllArgsConstructor;
import org.apache.camel.Endpoint;
import org.apache.camel.Processor;
import org.apache.camel.support.DefaultComponent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;

@AllArgsConstructor
@org.apache.camel.spi.annotations.Component("DmnRulesComponent")
public class DmnRulesComponent extends DefaultComponent {

    private static final Logger logger = LogManager.getLogger(DmnRulesComponent.class);

    private final PolicyRulesProcessor rulesProcessor;

    private static final String COLLECT_KEY = "collectToBody";

    /**
     * Writes result to BODY
     */
    private static final String WRITE_KEY = "writeToBody";


    private static final String EXECUTE = "execute";

    /**
     * @deprecated
     * Remove when COP dmn files are updated to not return {@link Map<String,Boolean>} in output
     */
    @Deprecated
    private static final String LEGACY_MAP_PREFIX = "legacyParse";

    /**
     * @deprecated
     * Delete after all 'audit' endpoints removed from all config stores.
     */
    @Deprecated
    private static final String AUDIT_KEY = "audit";

    @Override
    protected Endpoint createEndpoint(String uri, String remaining, Map<String, Object> parameters) throws Exception {

        Processor processor = createProcessor(remaining);

        Endpoint endpoint = new DmnRulesEndpoint(uri, this, processor);
        setProperties(endpoint, parameters);
        return endpoint;
    }

    private Processor createProcessor(String remaining) {
        Processor processor;
        if (remaining.equals(LEGACY_MAP_PREFIX) || remaining.equals(EXECUTE)) {
            processor = new DMNRulesProcessor(rulesProcessor, false, remaining.equals(LEGACY_MAP_PREFIX));
        } else if (remaining.endsWith(WRITE_KEY)) {
            processor = new DMNRulesProcessor(rulesProcessor, true, remaining.startsWith(LEGACY_MAP_PREFIX));
        } else if (COLLECT_KEY.equals(remaining)) {
            processor = new DMNRulesCollectProcessor();
        } else if (AUDIT_KEY.equals(remaining)) {
            processor = exchange -> logger.warn("Deprecated dmn component endpoint called: {}", remaining);
        } else {
            throw new IllegalArgumentException(String.format("DmnComponent unrecognized uri argument '%s'", remaining));
        }
        return processor;
    }

}
