package com.capitalone.identity.identitybuilder.policycore.service;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.ContextClosedEvent;

public class MockServerInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {

        WireMockServer server = new WireMockServer(new WireMockConfiguration().port(9123));
        server.start();

        applicationContext.getBeanFactory().registerSingleton("wireMock", server);
        applicationContext.addApplicationListener(event -> {
            if (event instanceof ContextClosedEvent){
                server.stop();
            }
        });
    }
}
