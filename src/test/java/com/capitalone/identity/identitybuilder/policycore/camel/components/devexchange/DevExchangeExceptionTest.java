package com.capitalone.identity.identitybuilder.policycore.camel.components.devexchange;

import com.capitalone.identity.identitybuilder.policycore.model.DevExchangeRequest;
import com.capitalone.identity.identitybuilder.policycore.model.DevExchangeResponse;
import com.capitalone.identity.identitybuilder.policycore.service.exception.DownstreamException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


import static org.junit.jupiter.api.Assertions.*;

public class DevExchangeExceptionTest {

    private static final String SERVICE_NAME = "serviceName";
    private static final int HTTP_STATUS = 201;
    private static final String DX_RESPONSE = "{ \"foo\":\"bar\" }";

    private DevExchangeRequest request;
    private DevExchangeResponse response;

    @BeforeEach
    public void setup() {
        request = new DevExchangeRequest();
        request.setServiceName(SERVICE_NAME);

        response = new DevExchangeResponse();
        response.setHttpStatus(HTTP_STATUS);
        response.setDxResponse(DX_RESPONSE);
    }

    private void checkReturnBodyAgainstToStringFormat(String serviceName, int status, String responseBody, String actualString) {
        Assertions.assertEquals(String.format("Downstream API \"%s\" returned %d: body=\"%s\"",
                serviceName, status, responseBody), actualString);
    }

    @Test
    public void testConstructor() {
        DevExchangeException e = new DevExchangeException(request, response);
        assertSame(request, e.getRequest());
        assertSame(response, e.getResponse());
        checkReturnBodyAgainstToStringFormat(SERVICE_NAME, HTTP_STATUS, DX_RESPONSE, e.toString());
    }

    @Test
    public void testConstructorEmptyObjects() {
        DevExchangeException e = new DevExchangeException(new DevExchangeRequest(), new DevExchangeResponse());
        checkReturnBodyAgainstToStringFormat(null, 0, null, e.toString());
    }

    @Test
    public void testDevExExceptionWitNullBody() {
        DevExchangeException e = new DevExchangeException(new DevExchangeRequest(), new DevExchangeResponse());
        assertNull(e.getBody());
    }

    @Test
    public void testDownstreamException() {
        DownstreamException e = new DevExchangeException(request, response);
        assertNull(e.getBody());
        assertNotNull(e.toString());
        checkReturnBodyAgainstToStringFormat(SERVICE_NAME, HTTP_STATUS, DX_RESPONSE, e.toString());
    }

    @Test
    public void testDevExExceptionWithNullReq() {
        DownstreamException e = new DevExchangeException(null, response);
        assertNull(e.getBody());
        assertNotNull(e);
    }

    @Test
    public void testDevExExceptionWithNullReqResp() {
        DevExchangeException e = new DevExchangeException(null, null);
        assertNull(e.getBody());
        assertNotNull(e);
    }


}