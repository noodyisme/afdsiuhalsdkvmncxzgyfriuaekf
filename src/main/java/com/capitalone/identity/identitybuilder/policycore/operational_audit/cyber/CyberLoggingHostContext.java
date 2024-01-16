package com.capitalone.identity.identitybuilder.policycore.operational_audit.cyber;

import com.capitalone.chassis.engine.dfs.emitter.core.config.HostContext;
import lombok.AllArgsConstructor;

/**
 * CyberLoggingHostContext holds data intended for use with {@link com.capitalone.chassis.engine.dfs.emitter.sdp.stream.CyberLoggingMapEventStreamDataBuilder}
 */
@AllArgsConstructor
public class CyberLoggingHostContext implements HostContext {

    private final HostContext hostContext;

    /**
     * Gets the MAC Address of the hosting environment.
     *
     * @return mac address or empty string
     */
    @Override
    public String getMacAddress() {
        return hostContext.getMacAddress();
    }

    /**
     * Gets the Amazon Resource Name (ARN) of the hosting environment, if available.
     *
     * @return arn or empty string
     */
    @Override
    public String getArn() {
        return hostContext.getArn();
    }

    /**
     * Gets the region of the hosting environment, if available.
     *
     * @return region or empty string
     */
    @Override
    public String getRegion() {
        return hostContext.getRegion();
    }

    /**
     * Gets the hostname of the hosting environment, if available.
     *
     * @return hostname or empty string
     */
    @Override
    public String getName() {
        return hostContext.getName();
    }

    /**
     * Gets the IPv4 address of the hosting environment, if available.
     *
     * @return ip or empty string
     */
    @Override
    public String getIpAddress() {
        return hostContext.getIpAddress();
    }

}
