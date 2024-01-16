package com.capitalone.identity.identitybuilder.policycore.camel.external.dynamic;

import com.capitalone.identity.identitybuilder.ClientEnvironment;

import java.time.Duration;

public class DynamicUpdateUtil {

    private DynamicUpdateUtil() {
    }

    public static Duration findDurationByEnv(ClientEnvironment clientEnvironment) {
        if(ClientEnvironment.DEV == clientEnvironment) {
            return Duration.ofSeconds(10l);
        } else {
            return Duration.ofSeconds(60l);
        }
    }
}
