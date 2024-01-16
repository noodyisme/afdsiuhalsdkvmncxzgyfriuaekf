package com.capitalone.identity.identitybuilder.policycore.camel.external.model;

import com.capitalone.identity.identitybuilder.model.EntityType;
import org.springframework.lang.Nullable;

import java.util.List;
import java.util.Optional;

/**
 * Represents attributes of camel routes used for validation purposes. Modify this model if more
 * attributes are required.
 * <ul>
 *     <li>List of Route IDs</li>
 *     <li>List of 'from' uris</li>
 *     <li>List of 'to' uris</li>
 * </ul>
 */
public interface CamelDataModel {

    List<String> getRouteIds();

    List<String> getInboundUris();

    List<String> getOutboundUris();

    String getName();

    /**
     * @return entity type. If null, then this model represents internal camel routes (bundled with application).
     */
    @Nullable
    EntityType getType();

    /**
     * @return the childPolicy from uri if the rotue id is marked as a child policy
     */
    Optional<String> getChildPolicyUri();
}
