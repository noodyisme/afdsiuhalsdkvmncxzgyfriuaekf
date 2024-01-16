package com.capitalone.identity.identitybuilder.policycore.externalization.events;

public interface EntityLoadEventVisitor {
    EntityLoadEventVisitor EMPTY = new EntityLoadEventVisitor() {};
    default void onEntityLoaded(EntityLoadEvents.Loaded event) {}

    default void onEntityUnloaded(EntityLoadEvents.Unloaded event) {}

    default void onEntityLoadFailed(EntityLoadEvents.Failed event) {}

    /**
     * Loading context failure
     * @param event
     */
    default void onNonEntityError(EntityLoadEvents.NonLoadingError event) {}

}
