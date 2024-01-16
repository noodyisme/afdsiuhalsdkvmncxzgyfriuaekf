package com.capitalone.identity.identitybuilder.policycore.externalization.events;

import com.capitalone.identity.identitybuilder.model.Entity;
import com.capitalone.identity.identitybuilder.model.EntityInfo;
import com.capitalone.identity.identitybuilder.model.EntityState;
import lombok.NonNull;
import lombok.Value;


public class EntityLoadEvents {

    private EntityLoadEvents() {
    }

    @Value
    public static class Failed implements EntityLoadOperationResult {
        EntityInfo info;

        EntityState.Delta.ChangeType changeType;

        @NonNull
        Throwable error;

        Long startTime;

        Long endTime;

        boolean isBootstrap;

        @Override
        public void visit(EntityLoadEventVisitor visitor) {
            visitor.onEntityLoadFailed(this);
        }
    }

    @Value
    public static class Loaded implements EntityLoadOperationResult {

        Entity entity;

        EntityState.Delta.ChangeType changeType;

        Long startTime;

        Long endTime;

        boolean isBootstrap;

        @Override
        public EntityInfo getInfo() {
            return entity.getInfo();
        }

        @Override
        public void visit(EntityLoadEventVisitor visitor) {
            visitor.onEntityLoaded(this);
        }

    }

    @Value
    public static class Unloaded implements EntityLoadOperationResult {

        EntityInfo info;

        Long startTime;

        Long endTime;

        @Override
        public EntityState.Delta.ChangeType getChangeType() {
            return EntityState.Delta.ChangeType.DELETE;
        }

        @Override
        public void visit(EntityLoadEventVisitor visitor) {
            visitor.onEntityUnloaded(this);
        }
    }

    @Value
    public static class NonLoadingError implements EntityLoadResult {

        Throwable error;

        @Override
        public void visit(EntityLoadEventVisitor visitor) {
            visitor.onNonEntityError(this);
        }
    }

}
