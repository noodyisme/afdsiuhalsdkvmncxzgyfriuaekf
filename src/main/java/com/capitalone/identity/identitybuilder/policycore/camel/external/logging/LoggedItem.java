package com.capitalone.identity.identitybuilder.policycore.camel.external.logging;

import com.capitalone.identity.identitybuilder.model.ConfigStoreItem;
import com.capitalone.identity.identitybuilder.model.ConfigStoreItemInfo;
import lombok.NonNull;
import lombok.Value;

/**
 * see {@link RuntimeUpdateEventLogger}
 */
@Value
public class LoggedItem {
    static LoggedItem newFromItem(ConfigStoreItemInfo info) {
        return new LoggedItem(info.getName(), info.getTag(), ConfigStoreItem.getTypeFromPath(info.getName()));
    }

    @NonNull String name;
    @NonNull String tag;
    @NonNull ConfigStoreItem.Type type;

}
