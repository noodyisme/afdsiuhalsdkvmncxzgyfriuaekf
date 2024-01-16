package com.capitalone.identity.identitybuilder.policycore.camel.external.dynamic;

import com.capitalone.identity.identitybuilder.model.*;
import com.capitalone.identity.identitybuilder.policycore.service.versions.PolicyVersionService;
import lombok.extern.log4j.Log4j;
import org.apache.logging.log4j.CloseableThreadContext;
import org.jetbrains.annotations.NotNull;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Responsible for managing updates for camel-dsl-based entity objects (Policies and Pips).
 * <p>
 * Instructions for use:
 * <ol>
 *     <li>Use the {@link #set(Entity)} method for add/update operations</li>
 *     <li>Use the {@link #delete(EntityInfo)} method to remove an entity from service</li>
 * </ol>
 * </p>
 */
@Component
@Log4j
public class DynamicPolicyUpdater {

    private final Map<String, EntityInfo> latestLoadedEntityVersionNumber = new HashMap<>();
    private final DynamicPolicyLoader policyLoader;
    private final EntityUpdateRegulator regulator;

    private final PolicyVersionService policyVersionService;

    @Inject
    public DynamicPolicyUpdater(DynamicPolicyLoader dynamicPolicyLoader,
                                EntityUpdateRegulator regulator,
                                PolicyVersionService policyVersionService) {
        this.policyLoader = new DisabledAwarePolicyLoader(dynamicPolicyLoader, regulator);
        this.regulator = regulator;
        this.policyVersionService = Objects.requireNonNull(policyVersionService);
    }

    /**
     * @return collection of {@link EntityInfo} objects with distinct {@link EntityInfo#getId()} that represent
     * the highest {@link EntityInfo#getPatchVersion()} of the entity that has been loaded.
     */
    public Collection<EntityInfo> getLoadedEntities() {
        return new ArrayList<>(latestLoadedEntityVersionNumber.values());
    }

    /**
     * Add/Update an entity of type {@link EntityType#POLICY} or {@link EntityType#PIP}. An error will be thrown
     * if the add/update operation fails due to validation error, malformed content, or transient error.
     *
     * @param entity of type {@link EntityType#POLICY} or {@link EntityType#PIP}
     */
    public synchronized void set(Entity entity) {
        doWithContext(() -> {
            EntityInfo prevEntityInfo = latestLoadedEntityVersionNumber.get(entity.getId());
            if (prevEntityInfo != null) {
                update(entity, prevEntityInfo);
            } else {
                add(entity);
            }
        });
    }

    /**
     * Delete a loaded policy. An error will be thrown if the delete operation fails for any reason,
     * or if the delete operation is not allowed.
     *
     * @param info of entity to delete
     */
    public synchronized void delete(@NotNull EntityInfo info) {
        UpdateTransactionResult result = policyLoader.delete(info);
        if (result.isSuccess()) {
            if (info instanceof EntityInfo.Policy) {
                policyVersionService.remove((PolicyInfo.Patch) info);
            }
            latestLoadedEntityVersionNumber.remove(info.getId(), info);
        } else {
            throw WrappedPolicyLoadException.newFromError(EntityState.Delta.ChangeType.DELETE, info, result.getError());
        }
    }

    synchronized void add(@NonNull Entity entity) {
        UpdateTransactionResult add = policyLoader.add(entity);
        if (add.isSuccess()) {
            if (entity instanceof Entity.Policy) {
                EntityActivationStatus activationStatus = ((Entity.Policy) entity).getEntityActivationStatus();
                policyVersionService.set((PolicyInfo.Patch) entity.getInfo(), activationStatus);
            }
            latestLoadedEntityVersionNumber.put(entity.getId(), entity.getInfo());
        } else {
            throw WrappedPolicyLoadException.newFromError(
                    EntityState.Delta.ChangeType.ADD, entity.getInfo(), add.getError());
        }
    }

    synchronized void update(@NonNull Entity entity, @NonNull EntityInfo prevEntityInfo) {

        EntityInfo nextEntityInfo = entity.getInfo();
        if (nextEntityInfo.equals(prevEntityInfo)) {
            return;
        } else {
            // Check if update operation is allowed.
            regulator.checkUpdateAllowedOrThrow(prevEntityInfo, nextEntityInfo);
        }

        int prevPatch = prevEntityInfo.getPatchVersion();
        int nextPatch = nextEntityInfo.getPatchVersion();
        if (prevPatch != nextPatch) {
            // it is safe to load the next patch version next to previous version
            add(entity);
            try {
                // clean up previous patch version
                delete(prevEntityInfo);
            } catch (Exception e) {
                // ignore cleanup errors because new patch version already serves all traffic
                log.error("Error cleaning up old patch version", e);
            }

        } else {
            // Attempt an unsafe 'replacement' update of a patch version that has already been loaded.
            try {
                delete(prevEntityInfo);
                add(entity);
            } catch (Exception e) {
                throw WrappedPolicyLoadException.newFromError(
                        EntityState.Delta.ChangeType.UPDATE, nextEntityInfo, e);
            }
        }

    }

    private void doWithContext(final Runnable runnable) {
        try (final CloseableThreadContext.Instance ctc = DynamicPolicyHelper.populateUpdateContext()) {
            runnable.run();
        }
    }

    /**
     * Wraps camel loader in a loader that understands not to load disabled policies into
     * camel.
     */
    private static class DisabledAwarePolicyLoader implements DynamicPolicyLoader {

        private final Set<String> disabledEntityPatchVersionsLoaded = new HashSet<>();
        private final Map<String, String> latestActiveLoadedPatchVersion = new HashMap<>();
        private final DynamicPolicyLoader camelLoader;
        private final EntityUpdateRegulator updateRegulator;

        DisabledAwarePolicyLoader(DynamicPolicyLoader camelLoader, EntityUpdateRegulator updateRegulator) {
            this.camelLoader = Objects.requireNonNull(camelLoader);
            this.updateRegulator = Objects.requireNonNull(updateRegulator);
        }

        private static boolean shouldLoad(Entity policy) {
            if (policy instanceof Entity.Policy) {
                return ((Entity.Policy) policy).getEntityActivationStatus() == EntityActivationStatus.ACTIVE
                        || ((Entity.Policy) policy).getEntityActivationStatus() == EntityActivationStatus.AVAILABLE;
            } else {
                return true;
            }
        }

        @Override
        public UpdateTransactionResult add(Entity entity) {
            String patchIdentifier = DynamicPolicyLoaderImpl.getPatchIdentifier(entity.getInfo());
            if (shouldLoad(entity)) {
                UpdateTransactionResult result = camelLoader.add(entity);
                if (result.isSuccess()) {
                    latestActiveLoadedPatchVersion.put(entity.getId(), patchIdentifier);
                }
                return result;
            } else {
                // perform no load operation, but keep track of the id and return success
                disabledEntityPatchVersionsLoaded.add(patchIdentifier);
                // clear the enabled map b.c. the latest patch version is disabled
                latestActiveLoadedPatchVersion.remove(entity.getId());
                return UpdateTransactionResult.success();
            }
        }

        @Override
        public UpdateTransactionResult delete(EntityInfo entityInfoPatch) {
            String patchIdentifier = DynamicPolicyLoaderImpl.getPatchIdentifier(entityInfoPatch);
            if (disabledEntityPatchVersionsLoaded.remove(patchIdentifier)) {
                // entity was never loaded, remove from list of disabled and return success
                return UpdateTransactionResult.success();
            } else if (patchIdentifier.equals(latestActiveLoadedPatchVersion.get(entityInfoPatch.getId()))) {
                // this patch version is current active. Check if delete is allowed before performing this action.
                updateRegulator.checkDeleteAllowedOrThrow(entityInfoPatch);
                return camelLoader.delete(entityInfoPatch);
            } else {
                // patch is not the latest active version number, so it can be removed without needing a check
                return camelLoader.delete(entityInfoPatch);
            }
        }

    }

    public static class WrappedPolicyLoadException extends RuntimeException {

        static WrappedPolicyLoadException newFromError(EntityState.Delta.ChangeType type, EntityInfo info, Throwable cause) {
            final String items = info.getItemInfo().stream()
                    .map(item -> String.format("{name=%s, tag=%s}", item.getName(), item.getTag()))
                    .collect(Collectors.joining(","));
            final String msg = String.format("Change<EntityInfo>[type=%s, id=%s, version=%s, items=[%s]]",
                    type,
                    info.getId(),
                    info.getVersion(),
                    items);
            return new WrappedPolicyLoadException(msg, cause);
        }

        private WrappedPolicyLoadException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
