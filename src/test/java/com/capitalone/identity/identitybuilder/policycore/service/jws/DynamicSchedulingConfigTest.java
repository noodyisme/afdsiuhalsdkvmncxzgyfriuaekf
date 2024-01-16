package com.capitalone.identity.identitybuilder.policycore.service.jws;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

import java.util.concurrent.Executor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DynamicSchedulingConfigTest {

    private final DynamicJWTKeyPairManager keyPairManager = mock(DynamicJWTKeyPairManager.class);
    private DynamicSchedulingConfig config = spy(new DynamicSchedulingConfig(keyPairManager));

    @Test
    void taskExecutor() {
        Executor executor = config.taskExecutor();
        assertNotNull(executor);
    }

    @Test
    void configureTasks(){
        ScheduledTaskRegistrar taskRegistrar = new ScheduledTaskRegistrar();
        assertDoesNotThrow(()->config.configureTasks(taskRegistrar));
    }

}

