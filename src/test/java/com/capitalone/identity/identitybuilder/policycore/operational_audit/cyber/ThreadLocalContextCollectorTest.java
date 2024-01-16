package com.capitalone.identity.identitybuilder.policycore.operational_audit.cyber;

import com.capitalone.chassis.engine.model.context.RequestContext;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.model.exchange_schema.OperationalProtocolHeader;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.util.OperationalAuditUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, SpringExtension.class})
public class ThreadLocalContextCollectorTest {
    private final String MOCK_USER_ID = "test_user";
    private final String MOCK_CLIENT_IP = "127.0.0.1";
    private final HashMap<String, List<String>> headersMap = new HashMap<>();
    ThreadLocalContextCollector mockThreadLocalContextCollector = new ThreadLocalContextCollector();
    @Mock
    RequestContext requestContext;

    @BeforeEach
    public void SetupRequestContext() {
        when(requestContext.getUserId()).thenReturn(MOCK_USER_ID);
        when(requestContext.getAllRequestHeaders()).thenReturn(headersMap);
        String MOCK_CORRELATION_ID = "mock_correlation_id";
        when(requestContext.getCorrelationId()).thenReturn(MOCK_CORRELATION_ID);
        String MOCK_MESSAGE_ID = "request_id_test";
        when(requestContext.getMessageId()).thenReturn(MOCK_MESSAGE_ID);
        when(requestContext.getClientIP()).thenReturn(MOCK_CLIENT_IP);

        headersMap.put("X-Forwarded-Proto", Collections.singletonList("test-protocol"));
    }

    @Test
    void getCurrentThreadContextTest() {
        Map<String, Object> contextMap = mockThreadLocalContextCollector
                .getCurrentThreadContext(this.getClass().getName(), requestContext);
        assertEquals(MOCK_USER_ID, contextMap.get("user_id"));
        assertEquals(Collections.singletonList(new OperationalProtocolHeader(
                    "X-Forwarded-Proto",
                    OperationalAuditUtil.headerValueStopGap(headersMap.get("X-Forwarded-Proto").get(0)))),
                contextMap.get("protocol_headers"));
        assertNotNull(contextMap.get("session_id"));
        assertEquals(MOCK_CLIENT_IP, contextMap.get("client_ip_address"));
        assertEquals(this.getClass().getName(), contextMap.get("process_id"));
        assertEquals(headersMap.get("X-Forwarded-Proto").get(0), contextMap.get("protocol_type"));

    }
}
