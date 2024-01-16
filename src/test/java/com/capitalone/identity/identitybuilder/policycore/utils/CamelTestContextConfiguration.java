package com.capitalone.identity.identitybuilder.policycore.utils;

import lombok.SneakyThrows;
import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.ModelCamelContext;
import org.apache.camel.model.dataformat.JsonDataFormat;
import org.apache.camel.spring.SpringCamelContext;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;

import java.util.Collections;
import java.util.List;

import static java.util.Collections.singletonMap;

public abstract class CamelTestContextConfiguration {

    @SneakyThrows
    @Bean
    protected CamelContext createCamelContext(ApplicationContext applicationContext) {
        CamelContext camelContext = new SpringCamelContext(applicationContext);
        handleDataFormats(camelContext);
        camelContext.addRoutes(route());
        return camelContext;
    }

    @SneakyThrows
    @Bean
    protected ProducerTemplate producerTemplate(CamelContext camelContext) {
        return camelContext.createProducerTemplate();
    }

    protected void handleDataFormats(CamelContext camelContext) {
        camelContext.adapt(ModelCamelContext.class)
                .setDataFormats(singletonMap("jackson", new JsonDataFormat()));
    }

    protected abstract RouteBuilder route();

    @Bean
    public List<RouteBuilder> routes() {
        return Collections.singletonList(route());
    }
}
