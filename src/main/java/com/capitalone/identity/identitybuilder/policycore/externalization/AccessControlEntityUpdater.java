package com.capitalone.identity.identitybuilder.policycore.externalization;

import com.capitalone.identity.identitybuilder.ClientEnvironment;
import com.capitalone.identity.identitybuilder.model.Entity;
import com.capitalone.identity.identitybuilder.model.EntityInfo;
import com.capitalone.identity.identitybuilder.model.IllegalProdPolicyUpdateException;
import com.capitalone.identity.identitybuilder.policycore.abac.config.PolicyAccessRegistry;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.Objects;

@Component
public class AccessControlEntityUpdater {
    private final PolicyAccessRegistry policyAccessRegistry;
    private final boolean doEnforceVersionNumberUpdatedOnChange;
    private final boolean doAllowDeleteOperations;

    @Inject
    public AccessControlEntityUpdater(PolicyAccessRegistry policyAccessRegistry, ClientEnvironment clientEnvironment) {
        this.policyAccessRegistry = Objects.requireNonNull(policyAccessRegistry);
        this.doEnforceVersionNumberUpdatedOnChange = clientEnvironment != ClientEnvironment.DEV;
        this.doAllowDeleteOperations = clientEnvironment != ClientEnvironment.PROD;
    }

    public void add(Entity.Access entity) {
        policyAccessRegistry.setPolicyAccess(entity.getPolicyAccess());
    }

    public void update(Entity.Access entity) {
        policyAccessRegistry.setPolicyAccess(entity.getPolicyAccess());
    }

    public void delete(EntityInfo.Access entityInfo) {
        if (doAllowDeleteOperations) {
            policyAccessRegistry.removePolicyAccess(entityInfo.getPolicyShortName(), entityInfo.getPolicyMajorVersion());
        }
    }

    public void checkUpdateAllowedOrThrow(@Nullable EntityInfo.Access prev, @NonNull EntityInfo.Access next) {
        if (prev != null && doEnforceVersionNumberUpdatedOnChange
                && !prev.getVersion().equals(next.getVersion())
                && prev.getPatchVersion() >= next.getPatchVersion())
            throw new IllegalProdAccessUpdateException("Illegal update of access control entity. Entity may not " +
                    "be updated through an overwrite operation. Please revert overwritten entity to it's previous" +
                    "version");
    }

    public static class IllegalProdAccessUpdateException extends IllegalProdPolicyUpdateException {

        public IllegalProdAccessUpdateException(String errorMessage) {
            super(errorMessage);
        }
    }
}
