package com.capitalone.identity.identitybuilder.policycore.camel.util;

import com.capitalone.identity.identitybuilder.policycore.camel.components.devexchange.DevExchangeException;
import com.capitalone.identity.identitybuilder.policycore.model.DevExchangeRequest;
import com.capitalone.identity.identitybuilder.policycore.model.DevExchangeResponse;
import com.capitalone.identity.identitybuilder.policycore.service.exception.DownstreamException;
import com.capitalone.identity.identitybuilder.policycore.service.exception.ForwardedDownstreamException;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class PolicyErrorUtilTest {

    private PolicyErrorUtil errorUtil = null;

    @BeforeEach
    public void setup() {
        errorUtil = new PolicyErrorUtil();
    }

    /**
     * These tests are just to exercise the code to make sure we have
     * code coverage.  They cannot fail and do not throw an exception so
     * there is nothing to test.
     */
    @Test
    public void testProcessError() {
        String DX_RESPONSE = "    \"errorInfo\": {\n" +
                "        \"id\": \"999999\",\n" +
                "        \"text\": \"Invalid Business Event\",\n" +
                "        \"developerText\": \"Business event must not be empty if provided\"\n" +
                "    }";
        assertThrows(Exception.class, () -> errorUtil.forwardDownstreamErrors(new Exception(DX_RESPONSE)));
    }

    @Test
    public void isDevExchangeException() {
        DevExchangeRequest request = new DevExchangeRequest();
        DevExchangeResponse response = new DevExchangeResponse();
        DevExchangeException e = new DevExchangeException(request, response);
        assertNull(e.getBody());
        DevExchangeException devExchangeException = new DevExchangeException(request, response);
        assertThrows(DevExchangeException.class, () -> errorUtil.forwardDownstreamErrors(devExchangeException));
    }

    @Test
    void doNotWrapNonDownstreamError() {
        RuntimeException test = new RuntimeException("test");
        Exception error = assertThrows(RuntimeException.class, () -> errorUtil.forwardDownstreamErrors(test));
        assertEquals(test, error);
    }

    @Test
    public void testProxyPassError() {

        String domainName = "secondaryauth";
        String policyName = "testDownStream";
        String policyVersion = "3.2";
        String method = "post";

        Map<String, Object> errorInfoMap = new LinkedHashMap<>();
        errorInfoMap.put("id", "200002");
        errorInfoMap.put("text", "Unexpected error");
        errorInfoMap.put("developerText", "ava.lang.StringIndexOutOfBoundsException: String index out of range: -1");
        Map<String, Object> errorBody = Collections.singletonMap("errorInfo", errorInfoMap);

        DownstreamException downstreamException = new TestDomainException(domainName, policyName, policyVersion, method, errorBody);

        ForwardedDownstreamException exception = assertThrows(ForwardedDownstreamException.class, () -> errorUtil.forwardDownstreamErrors(downstreamException));
        assertEquals(errorInfoMap.get("id"), exception.getId());
        assertEquals(errorInfoMap.get("developerText"), exception.getDeveloperText());
        assertEquals(errorInfoMap.get("text"), exception.getText());


    }

    // testing that null pointer exception is not encountered when values are null.
    @Test
    void testProxyPassErrorWithValidKeysAndNullValues() {
        String domainName = "secondaryauth";
        String policyName = "testDownStream";
        String policyVersion = "3.2";
        String method = "post";

        Map<String, Object> errorInfoMap = new LinkedHashMap<>();
        errorInfoMap.put("id", null);
        errorInfoMap.put("text", null);
        errorInfoMap.put("developerText", null);

        Map<String, Object> errorBody = Collections.singletonMap("errorInfo", errorInfoMap);

        DownstreamException downstreamException = new TestDomainException(domainName, policyName, policyVersion, method, errorBody);

        ForwardedDownstreamException exception = assertThrows(ForwardedDownstreamException.class, () -> errorUtil.forwardDownstreamErrors(downstreamException));

        assertEquals("null", exception.getId());
        assertEquals("null", exception.getDeveloperText());
        assertEquals("null", exception.getText());
    }

    @Test
    public void testProxyPassErrorWithDefaultValues() {

        String domainName = "secondaryauth";
        String policyName = "testDownStream";
        String policyVersion = "3.2";
        String method = "post";

        //noinspection MismatchedQueryAndUpdateOfCollection - we are expecting an empty map.
        Map<String, Object> errorInfoMap = new LinkedHashMap<>();
        Map<String, Object> errorBody = Collections.singletonMap("errorInfo", errorInfoMap);

        DownstreamException downstreamException = new TestDomainException(domainName, policyName, policyVersion, method, errorBody);

        ForwardedDownstreamException exception = assertThrows(ForwardedDownstreamException.class, () -> errorUtil.forwardDownstreamErrors(downstreamException));
        assertEquals(errorInfoMap.get("id"), exception.getId());
        assertEquals(errorInfoMap.get("developerText"), exception.getDeveloperText());
        assertEquals(errorInfoMap.get("text"), exception.getText());

    }

    @Test
    public void testNullSourceException() {
        IllegalArgumentException illegalArgumentException = assertThrows(IllegalArgumentException.class, () -> errorUtil.forwardDownstreamErrors(null));
        assertEquals("Error: Null exception was passed into policy-error:forwardDownstreamErrors.", illegalArgumentException.getMessage());
    }


    @Getter
    @AllArgsConstructor
    private static final class TestDomainException extends DownstreamException {

        private final transient String domain;
        private final transient String policyName;
        private final transient String policyVersion;
        private final transient String method;
        private final transient Map<String, Object> body;

        @Override
        public Map<String, Object> getBody() {
            return body;
        }
    }


}