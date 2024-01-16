package com.capitalone.identity.identitybuilder.policycore.dmn_rules;

import com.capitalone.identity.identitybuilder.client.ConfigStoreClient;
import com.capitalone.identity.identitybuilder.decisionengine.service.api.DecisionEngineService;
import com.capitalone.identity.identitybuilder.policycore.camel.BodyEnrichStrategy;
import com.capitalone.identity.identitybuilder.policycore.camel.DXResultsStrategy;
import com.capitalone.identity.identitybuilder.policycore.camel.ListStrategy;
import com.capitalone.identity.identitybuilder.policycore.camel.external.LocalTestConfigStore;
import com.capitalone.identity.identitybuilder.policycore.camel.external.dynamic.DynamicPolicyHelper;
import com.capitalone.identity.identitybuilder.policycore.camel.external.model.ExternalItemDefinitionHolder;
import com.capitalone.identity.identitybuilder.policycore.camel.util.HeaderUtil;
import com.capitalone.identity.identitybuilder.policycore.camel.util.ISO8601DateFormatter;
import com.capitalone.identity.identitybuilder.policycore.configmanagement.ConfigManagementService;
import com.capitalone.identity.identitybuilder.policycore.decisionengine.DecisionAuditLogger;
import com.capitalone.identity.identitybuilder.policycore.decisionengine.DecisionEngineRuntimeLoadService;
import com.capitalone.identity.identitybuilder.policycore.dmnrules.DmnEnrichStrategy;
import com.capitalone.identity.identitybuilder.policycore.dmnrules.DmnRulesComponent;
import com.capitalone.identity.identitybuilder.policycore.service.dmn.PolicyRulesProcessor;
import lombok.SneakyThrows;
import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.spi.Registry;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
@ComponentScan(
        basePackages = {"com.capitalone.identity.identitybuilder.client",
                "com.capitalone.identity.identitybuilder.policycore.decisionengine",
                "com.capitalone.identity.identitybuilder.decisionengine"
        })
public class DmnRulesComponentTestConfig {

    LocalTestConfigStore localTestConfigStore;

    @Bean
    LocalTestConfigStore getLocalTestConfigStore(ConfigStoreClient client,
            DecisionEngineRuntimeLoadService decisionEngineRuntimeLoadService) {
        localTestConfigStore = new LocalTestConfigStore(client);
        localTestConfigStore.getDmnRules().forEach(decisionEngineRuntimeLoadService::loadRulesIntoDecisionRuntime);
        return localTestConfigStore;
    }

    @Bean
    DecisionEngineRuntimeLoadService decisionEngineRuntimeLoadService(final DecisionEngineService decisionEngineService) {
        return new DecisionEngineRuntimeLoadService(decisionEngineService);
    }

    @Bean
    DmnEnrichStrategy dmnEnrichStrategy() {
        return new DmnEnrichStrategy();
    }

    @Bean
    ListStrategy listStrategy() {
        return new ListStrategy();
    }

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
    protected CamelContext createCamelContext(DmnRulesComponent dmnRulesComponent) throws Exception {
        CamelContext camelContext = new DefaultCamelContext();
        camelContext.addComponent("DmnRulesComponent", dmnRulesComponent);
        Registry registry = camelContext.getRegistry();
        registry.bind("DXResultsStrategy", new DXResultsStrategy());
        registry.bind("bodyEnrichStrategy", new BodyEnrichStrategy());
        registry.bind("dmnEnrichStrategy", new DmnEnrichStrategy());
        registry.bind("listStrategy", new ListStrategy());
        registry.bind("headerUtil", new HeaderUtil());
        registry.bind("ISO8601DateFormatter", new ISO8601DateFormatter());
        camelContext.addRoutes(route(camelContext));
        return camelContext;
    }


    public RouteBuilder route(CamelContext camelContext) {
        return new RouteBuilder() {
            @Override
            @SneakyThrows
            public void configure() {
                for (ExternalItemDefinitionHolder model : localTestConfigStore.getItemDefinitions(camelContext)) {
                    DynamicPolicyHelper.addRouteDefinitions(camelContext, model.getRoutes());
                }
            }
        };
    }

    @Bean
    ConfigManagementService configManagementService() {
        return new ConfigManagementService();
    }

    @Bean
    protected ProducerTemplate producerTemplate(CamelContext camelContext) {
        ProducerTemplate producerTemplate = camelContext.createProducerTemplate();
        return producerTemplate;
    }
}
