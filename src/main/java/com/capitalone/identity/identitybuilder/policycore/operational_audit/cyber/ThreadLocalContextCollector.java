package com.capitalone.identity.identitybuilder.policycore.operational_audit.cyber;


import com.capitalone.chassis.engine.model.context.RequestContext;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.model.exchange_schema.OperationalProtocolHeader;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.util.OperationalAuditUtil;
import com.capitalone.identity.identitybuilder.policycore.service.constants.ApplicationConstants;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

import static com.capitalone.chassis.engine.model.constants.Constants.UNDEFINED;

public class ThreadLocalContextCollector {

    public Map<String, Object> getCurrentThreadContext(String methodName, RequestContext currentContext) {
        HashMap<String, Object> contextMap = new HashMap<>();
        contextMap.put("protocol_headers", getProtocolHeaders(currentContext));
        contextMap.put("process_id", getProcessId(currentContext, methodName));
        contextMap.put("client_ip_address", getClientIpAddress(currentContext));
        contextMap.put("user_id", getUserId(currentContext));
        contextMap.put("session_id", getSessionId(currentContext));
        contextMap.put("request_id", getMessageId(currentContext));
        contextMap.put("protocol_type", getProtocolType(currentContext));
        return contextMap;
    }


    private List<OperationalProtocolHeader> getProtocolHeaders(RequestContext context) {
        List<OperationalProtocolHeader> headers = new ArrayList<>();

        // note on getAllRequestHeaders: only included if chassis.request.copy.headers property is set to true.
        for (Map.Entry<String, List<String>> header : context.getAllRequestHeaders().entrySet()) {
            if (!CollectionUtils.isEmpty(header.getValue())) {
                for (String value : header.getValue()) {
                    OperationalProtocolHeader protocolHeader = new OperationalProtocolHeader(header.getKey(),
                            OperationalAuditUtil.headerValueStopGap(value));
                    headers.add(protocolHeader);
                }
            }
        }

        return headers;
    }

    private String getUserId(RequestContext context) {
        return context.getUserId();
    }

    private String getClientIpAddress(RequestContext context) {
        return getHeaderCaseInsensitive(context, "X-Forwarded-For").orElse(context.getClientIP());
    }

    private String getProtocolType(RequestContext context) {
        // FROM CHASSIS' CyberLoggingMapEventStreamDataBuilder
        // The X-Forwarded-Proto (XFP) header is a de-facto standard header for identifying the protocol (HTTP or HTTPS) that a client used to connect to your proxy or load balancer.
        // See also: https://docs.aws.amazon.com/elasticloadbalancing/latest/classic/x-forwarded-headers.html
        List<String> headers = getHeadersCaseInsensitive(context, "X-Forwarded-Proto");
        if (headers.isEmpty()) {
            return "";
        }
        return headers.get(0);
    }

    //Used for messageId in Chassis?
    @SuppressWarnings({"squid:S1172", "WeakerAccess", "unused"})
    protected String getCorrelationId(RequestContext context) {
        return UNDEFINED.equals(context.getCorrelationId()) ? getHeaderCaseInsensitive(context,
                ApplicationConstants.CLIENT_CORRELATION_ID).orElse(UNDEFINED) : context.getCorrelationId();
    }

    //Used for requestId in Chassis?
    @SuppressWarnings({"squid:S1172", "WeakerAccess", "unused"})
    protected String getMessageId(RequestContext context) {
        return context.getMessageId();
    }

    private String getProcessId(RequestContext context, String methodCall) {
        return getHeaderCaseInsensitive(context, "x-process-id").orElse(methodCall);
    }

    protected String getSessionId(RequestContext context) {
        return String.format("%s~%s~%s~%s",
                getCorrelationId(context),
                getMessageId(context),
                Thread.currentThread().getId(), Instant.now().toEpochMilli());
    }


    private List<String> getHeadersCaseInsensitive(RequestContext context, String key) {
        return context.getAllRequestHeaders().entrySet().stream()
                .filter(kvp -> StringUtils.equalsIgnoreCase(kvp.getKey(), key))
                .flatMap(kvp -> kvp.getValue().stream())
                .collect(Collectors.toList());
    }

    private Optional<String> getHeaderCaseInsensitive(RequestContext context, String key) {
        return getHeadersCaseInsensitive(context, key).stream().findFirst();
    }

}
