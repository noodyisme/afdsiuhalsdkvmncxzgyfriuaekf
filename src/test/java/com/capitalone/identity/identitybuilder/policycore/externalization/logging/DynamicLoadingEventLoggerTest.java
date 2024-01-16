package com.capitalone.identity.identitybuilder.policycore.externalization.logging;

import com.capitalone.identity.identitybuilder.model.Entity;
import com.capitalone.identity.identitybuilder.model.EntityActivationStatus;
import com.capitalone.identity.identitybuilder.model.EntityInfo;
import com.capitalone.identity.identitybuilder.model.EntityState;
import com.capitalone.identity.identitybuilder.policycore.camel.external.dynamic.UpdateTransactionResult;
import com.capitalone.identity.identitybuilder.policycore.camel.external.logging.RuntimeUpdateEventLogger;
import com.capitalone.identity.identitybuilder.policycore.externalization.events.EntityLoadEvents;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.OperationalAuditor;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.mapper.EntityUpdateEventMapper;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.mapper.ScannerTerminationEventMapper;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.model.configstore.EntityUpdateEventOccurred;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.model.configstore.ScannerTerminationEventOccurred;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.platform.commons.util.ExceptionUtils;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static com.capitalone.identity.identitybuilder.policycore.operational_audit.model.type.OperationalEventType.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DynamicLoadingEventLoggerTest {

    @Mock
    OperationalAuditor auditor;

    @Mock
    RuntimeUpdateEventLogger updateEventLogger;

    DynamicLoadingEventLogger logger;

    @BeforeEach
    void setUpdateEventLogger() {
        logger = new DynamicLoadingEventLogger(updateEventLogger, auditor);
    }

    @Test
    void onEntityLoaded_add() {
        Entity.Pip entity = mock(Entity.Pip.class);
        EntityInfo.Pip entityInfo = mock(EntityInfo.Pip.class);
        when(entity.getInfo()).thenReturn(entityInfo);

        logger.onEntityLoaded(new EntityLoadEvents.Loaded(entity, EntityState.Delta.ChangeType.ADD, 100L, 150L, false));

        verify(updateEventLogger).auditUpdate(EntityState.Delta.ChangeType.ADD, entityInfo, EntityActivationStatus.ACTIVE);
        verify(auditor).audit(EntityUpdateEventOccurred.builder()
                .operationalEventType(ENTITY_ADDED)
                .changeType(EntityState.Delta.ChangeType.ADD)
                .info(entityInfo)
                .result(UpdateTransactionResult.success())
                .startTimestamp(100L)
                .endTimestamp(150L)
                .activationStatus(EntityActivationStatus.ACTIVE)
                .build(), EntityUpdateEventMapper.Factory.class);
    }

    @Test
    void onEntityLoaded_update() {
        Entity.Pip entity = mock(Entity.Pip.class);
        EntityInfo.Pip entityInfo = mock(EntityInfo.Pip.class);
        when(entity.getInfo()).thenReturn(entityInfo);

        logger.onEntityLoaded(new EntityLoadEvents.Loaded(entity, EntityState.Delta.ChangeType.UPDATE, 100L, 150L, false));

        verify(updateEventLogger).auditUpdate(EntityState.Delta.ChangeType.UPDATE, entityInfo, EntityActivationStatus.ACTIVE);
        verify(auditor).audit(EntityUpdateEventOccurred.builder()
                .operationalEventType(ENTITY_UPDATED)
                .changeType(EntityState.Delta.ChangeType.UPDATE)
                .info(entityInfo)
                .result(UpdateTransactionResult.success())
                .startTimestamp(100L)
                .endTimestamp(150L)
                .activationStatus(EntityActivationStatus.ACTIVE)
                .build(), EntityUpdateEventMapper.Factory.class);
    }

    @Test
    void onEntityUnloaded() {
        EntityInfo.Policy entityInfo = mock(EntityInfo.Policy.class);

        logger.onEntityUnloaded(new EntityLoadEvents.Unloaded(entityInfo, 100L, 150L));

        verify(updateEventLogger).auditUnload(entityInfo);
        verify(auditor).audit(EntityUpdateEventOccurred.builder()
                .operationalEventType(ENTITY_DELETED)
                .changeType(EntityState.Delta.ChangeType.DELETE)
                .info(entityInfo)
                .result(UpdateTransactionResult.success())
                .startTimestamp(100L)
                .endTimestamp(150L)
                .build(), EntityUpdateEventMapper.Factory.class);
    }

    @Test
    void onEntityLoadFailed() {
        RuntimeException error = new RuntimeException("test");
        EntityInfo.Policy entityInfo = mock(EntityInfo.Policy.class);

        logger.onEntityLoadFailed(new EntityLoadEvents.Failed(entityInfo, EntityState.Delta.ChangeType.UPDATE, error, 100L, 150L, false));

        verify(updateEventLogger).auditLoadOperationFailed(EntityState.Delta.ChangeType.UPDATE, entityInfo, error);
        verify(auditor).audit(EntityUpdateEventOccurred.builder()
                .operationalEventType(ENTITY_UPDATED)
                .changeType(EntityState.Delta.ChangeType.UPDATE)
                .info(entityInfo)
                .result(UpdateTransactionResult.error(error))
                .startTimestamp(100L)
                .endTimestamp(150L)
                .build(), EntityUpdateEventMapper.Factory.class);

    }

    @Test
    void onNonEntityError() {
        RuntimeException expectedError = new RuntimeException("test");
        logger.onNonEntityError(new EntityLoadEvents.NonLoadingError(expectedError));

        verify(updateEventLogger).auditTermination(ExceptionUtils.readStackTrace(expectedError));
        verify(auditor).audit(ScannerTerminationEventOccurred.builder()
                .operationalEventType(SCANNER_TERMINATED)
                .result(UpdateTransactionResult.error(expectedError))
                .build(), ScannerTerminationEventMapper.Factory.class);
    }

    @Test
    void onNonEntityErrorNullError() {
        logger.onNonEntityError(new EntityLoadEvents.NonLoadingError(null));

        verify(updateEventLogger).auditTermination(null);
    }

}
