package com.capitalone.identity.identitybuilder.policycore.camel.external.dynamic;

import com.capitalone.identity.identitybuilder.ClientEnvironment;
import com.capitalone.identity.identitybuilder.model.ConfigStoreItem;
import com.capitalone.identity.identitybuilder.model.ConfigStoreItemInfo;
import com.capitalone.identity.identitybuilder.model.EntityInfo;
import com.capitalone.identity.identitybuilder.model.IllegalProdPolicyUpdateException;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * This implementation allows policy entities to be updated as long as the content is
 * the same. Conveniently, this allows policies to be disabled via status flag.
 */
@Component
public class EntityUpdateRegulator {

    private final boolean isProd;

    @Inject
    EntityUpdateRegulator(ClientEnvironment environment) {
        isProd = environment == ClientEnvironment.PROD;
    }

    private static final Set<ConfigStoreItem.Type> allowedProdChangeTypes = new HashSet<>(Arrays.asList(
            ConfigStoreItem.Type.POLICY_STATUS,
            ConfigStoreItem.Type.POLICY_STATUS_SPARSE,
            ConfigStoreItem.Type.CONFIG_USECASE,
            ConfigStoreItem.Type.CONFIG_FEATURES
    ));
    private static String normalizedItemTag(EntityInfo info, ConfigStoreItemInfo itemInfo) {
        return itemInfo.getName().replaceFirst(info.getLocationPrefix(), "") + itemInfo.getTag();
    }

    public void checkUpdateAllowedOrThrow(@NonNull EntityInfo existing, @NonNull EntityInfo update) {
        if (isProd) {
            if (update instanceof EntityInfo.Policy && existing.getPatchVersion() < update.getPatchVersion()) {
                Set<String> currentInfo = existing.getItemInfo().stream()
                        .filter(item -> !allowedProdChangeTypes.contains(ConfigStoreItem.getTypeFromPath(item.getName())))
                        .map(item -> normalizedItemTag(existing, item))
                        .collect(Collectors.toSet());
                Set<String> updatedInfo = update.getItemInfo().stream()
                        .filter(item -> !allowedProdChangeTypes.contains(ConfigStoreItem.getTypeFromPath(item.getName())))
                        .map(item -> normalizedItemTag(update, item))
                        .collect(Collectors.toSet());
                if (!currentInfo.equals(updatedInfo)) {
                    throw new IllegalProdPolicyUpdateException("Illegal Production Policy/PIP Update Exception: " +
                            "UPDATE is not allowed in Production: Policy/PIP == " + update.getId());
                }
            } else {
                throw new IllegalProdPolicyUpdateException("Illegal Production Policy/PIP Update Exception: " +
                        "UPDATE is not allowed in Production: Policy/PIP == " + update.getId());
            }


        }
    }

    public void checkDeleteAllowedOrThrow(@NonNull EntityInfo toDelete) {
        if (isProd) {
            throw new IllegalProdPolicyUpdateException("Illegal Production Policy/PIP Delete Exception: " +
                    "DELETE is not allowed in Production: Policy/PIP == " + toDelete.getId());
        }
    }

}
