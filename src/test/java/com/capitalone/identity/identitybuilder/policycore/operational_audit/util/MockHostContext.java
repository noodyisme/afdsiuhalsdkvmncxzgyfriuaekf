package com.capitalone.identity.identitybuilder.policycore.operational_audit.util;

import com.capitalone.chassis.engine.dfs.emitter.core.config.HostContext;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.cyber.CyberLoggingHostContext;
import org.mockito.Mockito;

public final class MockHostContext extends CyberLoggingHostContext {

    public MockHostContext() {
        super(Mockito.mock(HostContext.class));
    }

    @Override
    public String getMacAddress() {
        return "empty-mac";
    }

    @Override
    public String getArn() {
        return "arn:aws:ecs:us-west-2:123456789012:service/cluster-name/service-name";
    }

    @Override
    public String getRegion() {
        return "us-west-2";
    }

    @Override
    public String getName() {
        return "test-hostname";
    }

    @Override
    public String getIpAddress() {
        return "192.168.0.1";
    }

}
