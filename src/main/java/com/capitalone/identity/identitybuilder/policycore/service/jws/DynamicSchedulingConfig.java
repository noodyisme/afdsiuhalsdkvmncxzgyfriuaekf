package com.capitalone.identity.identitybuilder.policycore.service.jws;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@Configuration
@ComponentScan(value = {"com.capitalone.identity.identitybuilder"})
@EnableScheduling
class DynamicSchedulingConfig implements SchedulingConfigurer {


    private final DynamicJWTKeyPairManager dynamicJWTKeyPairManager;
    public DynamicSchedulingConfig(DynamicJWTKeyPairManager dynamicJWTKeyPairManager){
        this.dynamicJWTKeyPairManager = dynamicJWTKeyPairManager;
    }

    @Bean
    public Executor taskExecutor() {
        return Executors.newSingleThreadScheduledExecutor();
    }

    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        taskRegistrar.setScheduler(taskExecutor());
        taskRegistrar.addTriggerTask(
                dynamicJWTKeyPairManager::dynamicKeyRotation,
                context -> {
                    Optional<Date> lastCompletionTime =
                            Optional.ofNullable(context.lastCompletionTime());
                    Instant nextExecutionTime =
                            lastCompletionTime.orElseGet(Date::new).toInstant()
                                    .plusMillis(dynamicJWTKeyPairManager.getDelay());
                    return Date.from(nextExecutionTime);
                }
        );
    }
}




