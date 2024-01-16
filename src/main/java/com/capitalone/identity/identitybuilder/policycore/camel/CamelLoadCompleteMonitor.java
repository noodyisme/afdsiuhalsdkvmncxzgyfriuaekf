package com.capitalone.identity.identitybuilder.policycore.camel;

import org.apache.camel.CamelContext;
import org.apache.camel.spring.boot.CamelContextConfiguration;
import org.springframework.stereotype.Component;

@Component
public class CamelLoadCompleteMonitor implements CamelContextConfiguration {

    private boolean isLoadComplete = false;

    public boolean isDoneLoading() {
        return isLoadComplete;
    }

    @Override
    public void beforeApplicationStart(CamelContext camelContext) {
        // unused
    }

    @Override
    public void afterApplicationStart(CamelContext camelContext) {
        isLoadComplete = true;
    }
}
