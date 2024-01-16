package com.capitalone.identity.identitybuilder.policycore.camel.external.logging;

import com.capitalone.identity.identitybuilder.model.EntityInfo;
import com.capitalone.identity.identitybuilder.model.EntityType;
import lombok.NonNull;
import lombok.Value;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * see {@link RuntimeUpdateEventLogger}
 */
@Value
public class LoggedEntityInfo {
    @NonNull EntityType type;
    @NonNull String id;
    @NonNull String location;
    @NonNull String version;
    @NonNull Integer versionNumber;
    @NonNull List<LoggedItem> items;
    @NonNull Integer itemsCount;
    @NonNull Set<String> ignoredNonPolicyItems;

    static LoggedEntityInfo newFromInfo(@NonNull EntityInfo info) {
        List<LoggedItem> items = info.getItemInfo().stream().map(LoggedItem::newFromItem).collect(Collectors.toList());
        return new LoggedEntityInfo(info.getType(), info.getId(), info.getLocationPrefix(), info.getVersion(), info.getPatchVersion(),
                items, items.size(), info.getFilteredItemNames());
    }
}
