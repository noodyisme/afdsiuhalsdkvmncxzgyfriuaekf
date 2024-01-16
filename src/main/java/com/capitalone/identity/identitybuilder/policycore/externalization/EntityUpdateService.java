package com.capitalone.identity.identitybuilder.policycore.externalization;

import com.capitalone.identity.identitybuilder.client.ConfigStoreClient;
import com.capitalone.identity.identitybuilder.model.Entity;
import com.capitalone.identity.identitybuilder.model.EntityInfo;
import com.capitalone.identity.identitybuilder.model.EntityState;
import com.capitalone.identity.identitybuilder.policycore.camel.external.dynamic.UpdateTransactionResult;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
@AllArgsConstructor
@Log4j2
public class EntityUpdateService {

    private final ConfigStoreClient client;
    private final AccessControlEntityUpdater accessControlEntityUpdater;

    private final Map<String, EntityInfo> loadedEntities = new HashMap<>();

    /**
     * Updates the provided entity in policy core based on the provided request. This method is intended
     * to be general purpose and leveraged for all entity updates.
     *
     * @param info the entity info
     * @param changeType the change type
     * @return the result status
     */
    public UpdateTransactionResult applyEntityUpdate(final EntityInfo info, final EntityState.Delta.ChangeType changeType) {
        try {
            switch (changeType) {
                case ADD:
                case UPDATE:
                    EntityInfo existingVersion = loadedEntities.get(info.getId());
                    accessControlEntityUpdater.checkUpdateAllowedOrThrow((EntityInfo.Access) existingVersion, (EntityInfo.Access) info);
                    if (existingVersion == null) {
                        accessControlEntityUpdater.add((Entity.Access) client.getEntity(info));
                    } else {
                        accessControlEntityUpdater.update((Entity.Access) client.getEntity(info));
                    }
                    loadedEntities.put(info.getId(), info);
                    return UpdateTransactionResult.success();
                case DELETE:
                    accessControlEntityUpdater.delete((EntityInfo.Access) info);
                    loadedEntities.remove(info.getId());
                    return UpdateTransactionResult.success();
                default:
                    throw new UnsupportedOperationException("unknown change type " + changeType);
            }
        } catch (Exception r) {
            return UpdateTransactionResult.error(r);
        }
    }

    public List<EntityInfo> getCurrentLoadedEntities() {
        return loadedEntities.keySet().stream()
                .sorted(Comparator.naturalOrder())
                .map(loadedEntities::get)
                .collect(Collectors.toList());
    }

}
