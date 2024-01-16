package com.capitalone.identity.identitybuilder.policycore.operational_audit.cyber;

import com.capitalone.chassis.engine.dfs.emitter.core.config.HostContext;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CyberLoggingHostContextTest {

    @Test
    void testGetters() {
        // Arrange
        final CyberLoggingHostContext cyberLoggingHostContext = new CyberLoggingHostContext(new HostContextMock());

        // Act & Assert
        assertEquals("mac", cyberLoggingHostContext.getMacAddress());
        assertEquals("arn", cyberLoggingHostContext.getArn());
        assertEquals("region", cyberLoggingHostContext.getRegion());
        assertEquals("name", cyberLoggingHostContext.getName());
        assertEquals("address", cyberLoggingHostContext.getIpAddress());
    }

    private static class HostContextMock implements HostContext {

        @Override public String getMacAddress() {
            return "mac";
        }

        @Override public String getArn() {
            return "arn";
        }

        @Override public String getRegion() {
            return "region";
        }

        @Override public String getName() {
            return "name";
        }

        @Override public String getIpAddress() {
            return "address";
        }
    }

}