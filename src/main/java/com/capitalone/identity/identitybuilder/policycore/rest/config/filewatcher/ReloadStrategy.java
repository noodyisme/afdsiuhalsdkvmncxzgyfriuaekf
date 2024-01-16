package com.capitalone.identity.identitybuilder.policycore.rest.config.filewatcher;

import org.apache.camel.CamelContext;
import org.apache.camel.CamelContextAware;
import org.apache.camel.Service;
import org.apache.camel.StaticService;

import java.io.InputStream;

public interface ReloadStrategy extends Service, StaticService, CamelContextAware {

    /**
     * A reload is triggered when a XML resource is changed which contains Camel routes.
     *
     * @param camelContext  the running CamelContext
     * @param name          name of resource such as a file name (can be null)
     * @param resource      the changed resource
     */
    void onReloadXml(CamelContext camelContext, String name, InputStream resource);

    /**
     * Number of reloads succeeded.
     */
    int getReloadCounter();

    /**
     * Number of reloads failed.
     */
    int getFailedCounter();

    /**
     * Reset the counters.
     */
    void resetCounters();
}
