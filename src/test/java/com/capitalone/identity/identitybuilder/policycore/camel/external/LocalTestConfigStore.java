package com.capitalone.identity.identitybuilder.policycore.camel.external;

import com.capitalone.identity.identitybuilder.client.ConfigStoreClient;
import com.capitalone.identity.identitybuilder.model.ConfigStoreItem;
import com.capitalone.identity.identitybuilder.model.Entity;
import com.capitalone.identity.identitybuilder.model.EntityType;
import com.capitalone.identity.identitybuilder.policycore.camel.external.dynamic.DynamicPolicyLoaderImpl;
import com.capitalone.identity.identitybuilder.policycore.camel.external.dynamic.ExternalRouteParserImpl;
import com.capitalone.identity.identitybuilder.policycore.camel.external.model.ExternalItemDefinitionHolder;
import com.capitalone.identity.identitybuilder.policycore.dmnrules.RuleDefinitionModelGroup;
import com.capitalone.identity.identitybuilder.policycore.dmnrules.RuleDefinitionModelGroupFactory;
import org.apache.camel.CamelContext;
import org.apache.camel.spring.boot.CamelConfigurationProperties;
import org.springframework.core.io.Resource;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * An Item store based on local {@link Resource}s. Will scan a directory for policy files in a way that is
 * similar to the patterns set in {@code EXTERNAL_XML_ROUTES} property. This property works the same as
 * {@link CamelConfigurationProperties} xmlRoutes.
 */
public class LocalTestConfigStore {

    final ConfigStoreClient client;

    private final RuleDefinitionModelGroupFactory ruleDefinitionModelGroupFactory = new RuleDefinitionModelGroupFactory();

    public LocalTestConfigStore(ConfigStoreClient client) {
        this.client = client;
    }

    public List<ExternalItemDefinitionHolder> getItemDefinitions(CamelContext context) {
        ExternalRouteParserImpl parser = new ExternalRouteParserImpl(context);
        return client.getEntityInfo(EntityType.POLICY, EntityType.PIP, EntityType.ACCESS, EntityType.UNDEFINED)
                .map(client::getEntity)
                .map(parser::parseRouteResource).collectList().block();
    }

    public List<RuleDefinitionModelGroup> getDmnRules() {
        final List<Entity> policyEntities = client.getEntityInfo(EntityType.POLICY)
                .map(client::getEntity).collectList().block();
        final List<RuleDefinitionModelGroup> ruleDefinitionModelGroupList = new ArrayList<>();
        policyEntities.forEach(entity -> {
            String idWithVersionNumber = DynamicPolicyLoaderImpl.getPatchIdentifier(entity.getInfo());
            final Set<ConfigStoreItem> ruleItems = ((Entity.Policy) entity).getRuleItems();
            if (!ruleItems.isEmpty()) {
                ruleDefinitionModelGroupList.add(ruleDefinitionModelGroupFactory.create(
                        idWithVersionNumber,
                        RuleDefinitionModelGroupFactory.convertItemsToMap(ruleItems)
                ));
            }
        });
        return ruleDefinitionModelGroupList;
    }
}
