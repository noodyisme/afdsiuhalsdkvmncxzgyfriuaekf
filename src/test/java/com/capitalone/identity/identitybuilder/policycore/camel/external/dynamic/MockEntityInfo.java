package com.capitalone.identity.identitybuilder.policycore.camel.external.dynamic;

import com.capitalone.identity.identitybuilder.model.ConfigStoreItemInfo;
import com.capitalone.identity.identitybuilder.model.EntityInfo;
import com.capitalone.identity.identitybuilder.model.EntityType;

import java.util.Arrays;
import java.util.HashSet;

public class MockEntityInfo extends EntityInfo {

    public MockEntityInfo(String entityId, EntityType entityType, ConfigStoreItemInfo... items) {
        super(entityId, entityId, entityType,
                new HashSet<>(Arrays.asList(items)), EntityInfo.DEFAULT_VERSION_NUMBER, null);
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public int getMajorVersion() {
        return 0;
    }

    @Override
    public int getMinorVersion() {
        return 0;
    }
}
