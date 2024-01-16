package com.capitalone.identity.identitybuilder.policycore.service.logging;

import com.capitalone.chassis.engine.model.context.RequestContext;
import com.capitalone.chassis.engine.model.context.RequestContextHolder;
import lombok.extern.log4j.Log4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;


@ExtendWith(value = {MockitoExtension.class, SpringExtension.class})
@ContextConfiguration(classes = PolicySystemEventLogUpdater.class)
@TestPropertySource(properties = {
        "chassis.app.name=dummy-name",
        "chassis.app.code=dummy-code",
        "HOSTNAME=ABC", // Container Runtime ID always set in images
        "CLUSTER_ID=blue", // Used by Chassis to populate clusterId in audit logs
        "DEPLOYMENT_REGION=EAST", // Used by Chassis to populate region in audit logs
        "ENVIRONMENT_NAME=prod", // Used by Chassis to populate environmentName in audit logs
})
class PolicySystemEventLogUpdaterTest {

    @Autowired
    PolicySystemEventLogUpdater updater;

    @Mock
    ProceedingJoinPoint pointcut;

    /**
     * Some other tests have dependency on existing static RequestContext
     */
    RequestContext saved;

    @BeforeEach
    void saveRequestContext() {
        saved = RequestContextHolder.getRequestContextOrDefault();
    }

    @AfterEach
    void restoreRequestContext() {
        RequestContextHolder.put(saved);
    }

    @Test
    void nullValuesDoesNotThrow() {
        PolicySystemEventLogUpdater updater = new PolicySystemEventLogUpdater(null, null, null, null, null, null, null);
        assertDoesNotThrow(() -> updater.applyRequestContextForSystemEvent(pointcut));
    }

    @Test
    void testInjectProperties() {
        assertNotNull(updater);
        assertEquals("dummy-name", updater.applicationName);
        assertEquals("dummy-code", updater.applicationCode);
        assertEquals("blue", updater.clusterId);
        assertEquals("EAST", updater.region);
        assertEquals("prod", updater.environmentName);
        assertEquals("ABC", updater.containerId);
        assertEquals("dummy-name.prod.EAST.blue-ABC", updater.systemRuntimeId);

        assertDoesNotThrow(() -> updater.applyRequestContextForSystemEvent(pointcut));
    }

    @Test
    void testContainerIdOverrideHostnameWhenSet() {
        String containerId = "XYZ";
        String hostName = "ABC";
        PolicySystemEventLogUpdater instance = new PolicySystemEventLogUpdater(null, null, null, containerId, hostName, null, null);
        assertEquals("XYZ", instance.containerId);
        assertTrue(instance.systemRuntimeId.endsWith("XYZ"));
        assertDoesNotThrow(() -> instance.applyRequestContextForSystemEvent(pointcut));
    }

    @Test
    void logWithNonDefault() {
        RequestContext requestContext = new RequestContext();
        requestContext.setApplicationCode("applicationCode");
        requestContext.setApplicationName("applicationName");
        requestContext.setContainerId("containerId");
        requestContext.setClusterId("clusterId");
        requestContext.setEventStartTime(Instant.now());
        RequestContextHolder.put(requestContext);
        assertDoesNotThrow(() -> updater.applyRequestContextForSystemEvent(pointcut));
    }

}
