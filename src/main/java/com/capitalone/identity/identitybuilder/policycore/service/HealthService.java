package com.capitalone.identity.identitybuilder.policycore.service;


import com.capitalone.chassis.cal1_5.builder.Cal1_5EventStreamDataBuilder;
import com.capitalone.chassis.engine.annotations.stream.EventStream;
import com.capitalone.identity.identitybuilder.policycore.camel.CamelLoadCompleteMonitor;
import com.capitalone.identity.identitybuilder.policycore.camel.external.dynamic.DynamicPolicyManager;
import com.capitalone.identity.identitybuilder.policycore.externalization.DynamicEntityManager;
import com.capitalone.identity.identitybuilder.policycore.service.jws.DynamicJWTKeyPairManager;
import com.capitalone.identity.identitybuilder.policycore.service.logging.PolicySystemEventLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class HealthService {

    final CamelLoadCompleteMonitor loadCompleteMonitor;

    final DynamicPolicyManager externalPolicyManager;

    final DynamicEntityManager externalEntityManager;

    final DynamicJWTKeyPairManager dynamicJWTKeyPairManager;

    @Inject
    public HealthService(CamelLoadCompleteMonitor loadCompleteMonitor,
                         DynamicPolicyManager externalPolicyManager,
                         @Autowired(required = false) DynamicEntityManager externalEntityManager,
                         DynamicJWTKeyPairManager dynamicJWTKeyPairManager) {
        this.loadCompleteMonitor = loadCompleteMonitor;
        this.externalPolicyManager = externalPolicyManager;
        this.externalEntityManager = externalEntityManager;
        this.dynamicJWTKeyPairManager = dynamicJWTKeyPairManager;

    }

    private final AtomicBoolean healthServiceIsReady = new AtomicBoolean();

    /**
     * Indicates whether the policy core has finished startup operations needed for receiving policy requests.
     * This will validate that external policies have been loaded into the Camel Context.
     *
     * @return boolean indicating if the policy core is "ready" - done with startup processes
     */

    @PolicySystemEventLog
    @EventStream(
            eventName = "idb-policy-core.health-check",
            customEventStreams = @EventStream.CustomEventStream(
                    type = EventStream.EventStreamType.AUDIT,
                    emitters = "sdpv3",
                    eventStreamDataBuilder = Cal1_5EventStreamDataBuilder.BUILDER_TYPE)
    ) //TODO: Remove this annot. when Kafka connector reaches EOL after 5/31/23
    public boolean isReady() {
        if (!healthServiceIsReady.get()) {
            healthServiceIsReady.compareAndSet(false, loadCompleteMonitor.isDoneLoading()
                    && externalPolicyManager.isExternalLoadComplete()
                    && (externalEntityManager == null || externalEntityManager.isAccessControlLoadComplete())
                    && dynamicJWTKeyPairManager.isKeyPairReady());
        }
        return healthServiceIsReady.get() && dynamicJWTKeyPairManager.isKeyPairReady();
    }
}
