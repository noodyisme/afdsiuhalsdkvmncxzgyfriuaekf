package com.capitalone.identity.identitybuilder.policycore.abac.service;

import com.capitalone.identity.identitybuilder.policycore.abac.model.AbacResponse;

/**
 * @param <T>  - Subject
 * @param <U>  - Resource
 * @param <V>- Action
 */
public interface AuthorizationEnforcer<T, U, V> {
    AbacResponse authorize(T subject, U resource, V action);
}

