package com.capitalone.identity.identitybuilder.policycore.camel.external.dynamic;

import com.capitalone.identity.identitybuilder.model.Entity;
import com.capitalone.identity.identitybuilder.model.EntityInfo;

/**
 * Load components into Masterbuilder that relate to "policy" definitions (including
 * route files for Camel and dmn files for decision engine).
 * <div> </div>
 * <b>Note: success/failure operations returned via {@link UpdateTransactionResult}. Unsuccessful
 * operations indicate the loader may be in an inconsistent state.</b>
 */
public interface DynamicPolicyLoader {

    /**
     * @param entity config store entity that represents the full policy
     * @return result indicating if operation was successful, rolled back, or failed, indicating if internal state
     * of the {@link DynamicPolicyLoader} is inconsistent.
     * @throws IllegalArgumentException if the entity with that ID has already been added (caller must
     *                                  keep track of success/failure/rollback responses)
     */
    UpdateTransactionResult add(Entity entity);

    /**
     * @param info about the policy to delete
     * @return result indicating if operation was successful, rolled back, or failed, indicating if internal state
     * of the {@link DynamicPolicyLoader} is inconsistent.
     * @throws NullPointerException if the entity with that ID wasn't found to delete (caller must
     *                              keep track of success/failure/rollback responses)
     */
    UpdateTransactionResult delete(EntityInfo info);
}
