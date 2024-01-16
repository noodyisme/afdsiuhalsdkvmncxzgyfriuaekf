package com.capitalone.identity.identitybuilder.policycore.crypto;

import org.apache.commons.collections4.map.PassiveExpiringMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Configuration
@EnableAspectJAutoProxy(proxyTargetClass = true)
public class CryptoSpringConfig {
    @Value("${identitybuilder.policycore.crypto.decryption.cos.private-key-cache-ttl-seconds:900}")
    private Long decryptCos;

    @Bean(name = "decryptionPrivateKeyMap")
    public Map<String, String> privateKeyMap() {
        return Collections.synchronizedMap(new PassiveExpiringMap<>(decryptCos, TimeUnit.SECONDS));
    }
}
