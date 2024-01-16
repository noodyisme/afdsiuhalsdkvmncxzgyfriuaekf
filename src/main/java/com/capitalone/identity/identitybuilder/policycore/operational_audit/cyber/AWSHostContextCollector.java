package com.capitalone.identity.identitybuilder.policycore.operational_audit.cyber;

import com.capitalone.chassis.engine.model.context.RequestContext;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.mapper.OperationalEventEnricher;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.model.exchange_schema.OperationalProtocolHeader;

import java.util.List;
import java.util.Map;

public class AWSHostContextCollector {
    private final CyberLoggingHostContext cyberContext;
    ThreadLocalContextCollector contextCollector = new ThreadLocalContextCollector();

    public AWSHostContextCollector(final CyberLoggingHostContext cyberLoggingContext) {
        this.cyberContext = cyberLoggingContext;
    }


    public OperationalEventEnricher.CyberAttributes setCyberAttributes(String methodName, RequestContext currentContext) {
        Map<String, Object> contextMap = contextCollector.getCurrentThreadContext(methodName, currentContext);
        return OperationalEventEnricher.CyberAttributes.builder()
                .arn(cyberContext.getArn())
                .macAddress(cyberContext.getMacAddress())
                .threadId(String.valueOf(Thread.currentThread().getId()))
                .clientIpAddress(String.valueOf(contextMap.get("client_ip_address")))
                .protocolHeaders((List<OperationalProtocolHeader>) contextMap.get("protocol_headers"))
                .processId((String) contextMap.get("process_id"))
                .userId((String) contextMap.get("user_id"))
                .sessionId((String) contextMap.get("session_id"))
                .messageId((String) contextMap.get("request_id"))
                .protocolType((String) contextMap.get("protocol_type"))
                .build();

    }


}
