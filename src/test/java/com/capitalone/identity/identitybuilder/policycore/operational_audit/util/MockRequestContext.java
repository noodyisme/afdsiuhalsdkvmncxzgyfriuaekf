package com.capitalone.identity.identitybuilder.policycore.operational_audit.util;

import com.capitalone.chassis.engine.model.context.RequestContext;

public class MockRequestContext {
    public static String MOCK_CORRELATION_ID = "mock_correlation_id";
    public static String MOCK_MESSAGE_ID = "request_id_test";
    public static String MOCK_USER_ID = "test_user";

    public static String MOCK_CLIENT_IP = "127.0.0.1";
    private final RequestContext requestContext = new RequestContext();

    public RequestContext MockRequestContext() {
        requestContext.setClientIP(MOCK_CLIENT_IP);
        requestContext.setMessageId(MOCK_MESSAGE_ID);
        requestContext.setCorrelationId(MOCK_CORRELATION_ID);
        requestContext.setUserId(MOCK_USER_ID);
        return requestContext;
    }
}
