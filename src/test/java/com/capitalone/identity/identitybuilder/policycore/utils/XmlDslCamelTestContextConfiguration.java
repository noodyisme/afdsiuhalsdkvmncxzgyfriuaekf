package com.capitalone.identity.identitybuilder.policycore.utils;

import org.apache.camel.builder.RouteBuilder;

public class XmlDslCamelTestContextConfiguration extends CamelTestContextConfiguration {

    @Override
    public RouteBuilder route() {
        return new RouteBuilder() {
            @Override
            public void configure() {
                from("direct:start")
                        .log("${body}");
            }
        };
    }

}
