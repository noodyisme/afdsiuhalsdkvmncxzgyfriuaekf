package com.capitalone.identity.identitybuilder.policycore.externalization.dynamic_loading_tests;

import com.capitalone.api.security.AuthenticationResponseMarshaller;
import com.capitalone.identity.identitybuilder.decisionengine.service.api.DecisionEngineService;
import com.capitalone.identity.identitybuilder.policycore.abac.config.PolicyAccessRegistry;
import com.capitalone.identity.identitybuilder.policycore.camel.BodyEnrichStrategy;
import com.capitalone.identity.identitybuilder.policycore.camel.ListStrategy;
import com.capitalone.identity.identitybuilder.policycore.configmanagement.ConfigManagementService;
import com.capitalone.identity.identitybuilder.policycore.decisionengine.DecisionAuditLogger;
import com.capitalone.identity.identitybuilder.policycore.dmnrules.DmnRulesComponent;
import com.capitalone.identity.identitybuilder.policycore.externalization.EntityUpdateService;
import com.capitalone.identity.identitybuilder.policycore.schema.SchemaManager;
import com.capitalone.identity.identitybuilder.policycore.service.dmn.PolicyRulesProcessor;
import com.capitalone.identity.identitybuilder.policycore.service.versions.PolicyVersionService;
import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.model.ModelCamelContext;
import org.apache.camel.model.dataformat.JsonDataFormat;
import org.apache.camel.spring.SpringCamelContext;
import org.mockito.Mockito;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.Collections;

@Configuration
@EnableAutoConfiguration
@ComponentScan(
        basePackages = {"com.capitalone.identity.identitybuilder.client",
                "com.capitalone.identity.identitybuilder.policycore.decisionengine",
                "com.capitalone.identity.identitybuilder.decisionengine"
        },
        basePackageClasses = {SchemaManager.class, ConfigManagementService.class, PolicyAccessRegistry.class,
                EntityUpdateService.class, PolicyVersionService.class}
)
public class StartupTestConfig {

    @Bean
    @Primary
    DecisionAuditLogger logger() {
        return Mockito.mock(DecisionAuditLogger.class);
    }

    @Bean
    PolicyRulesProcessor processor(DecisionAuditLogger auditLogger, DecisionEngineService decisionEngineService) {
        return new PolicyRulesProcessor(decisionEngineService, auditLogger);
    }

    @Bean
    DmnRulesComponent dmnRulesComponent(PolicyRulesProcessor processor) {
        return new DmnRulesComponent(processor);
    }

    @Bean
    protected CamelContext camelContext(ApplicationContext applicationContext, DmnRulesComponent dmnRulesComponent) {
        CamelContext camelContext = new SpringCamelContext(applicationContext);
        camelContext.addComponent("DmnRulesComponent", dmnRulesComponent);
        camelContext.adapt(ModelCamelContext.class)
                .setDataFormats(Collections.singletonMap("jackson", new JsonDataFormat()));
        camelContext.build();
        return camelContext;
    }

    @Bean
    protected ProducerTemplate producerTemplate(CamelContext camelContext) {
        return camelContext.createProducerTemplate();
    }

    @Bean
    AuthenticationResponseMarshaller myResponseMarshaller() {
        return (httpServletRequest, httpServletResponse, o) -> "";
    }

    @Bean
    ListStrategy listStrategy() { return new ListStrategy();}

    @Bean BodyEnrichStrategy bodyEnrichStrategy(){
        return new BodyEnrichStrategy();
    }

}
