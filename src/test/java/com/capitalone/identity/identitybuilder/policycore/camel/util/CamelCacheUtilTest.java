package com.capitalone.identity.identitybuilder.policycore.camel.util;

import com.capitalone.chassis.engine.model.exception.ChassisSystemException;
import com.capitalone.identity.identitybuilder.policycore.camel.PolicyConstants;
import com.capitalone.identity.identitybuilder.policycore.model.PolicyState;
import com.capitalone.identity.identitybuilder.policycore.service.PolicyStateCacheService;
import com.capitalone.identity.identitybuilder.policycore.service.exception.PolicyCacheException;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.support.DefaultExchange;
import org.apache.camel.support.DefaultMessage;
import org.apache.camel.test.junit5.CamelTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.Serializable;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CamelCacheUtilTest extends CamelTestSupport {
    public final String CACHE_KEY = "6fa2b707-1886-4e33-bf6e-764ffb35d435";
    public final String CACHE_EXPIRATION = "PT15M";
    public final String POLICY_NAME = "success";
    public final String POLICY_VERSION = "1.0";
    public final String POLICY_CHILD_NAME = "success_b";
    public final String STEP_NAME = "step2";

    public static Boolean isMultistep = true;
    public List<String> stepsCompleted = new ArrayList<>();
    public Set<String> availableNextSteps = new HashSet<>();
    public Map<String, Serializable> cacheValue = new HashMap<>();
    public PolicyState policyState = new PolicyState();
    public Map<String, Object> headers = new HashMap<>();

    @Mock
    private PolicyStateCacheService cacheService;

    private CamelCacheUtil camelCacheUtil;

    @BeforeEach
    public void setup() {
        camelCacheUtil = Mockito.spy(new CamelCacheUtil(cacheService, false));
        cacheValue.put("foo", "bar");
        policyState.setPolicyState(cacheValue);

        policyState.setPolicyName(POLICY_NAME);
        policyState.setPolicyVersion(POLICY_VERSION);

        availableNextSteps.add("step3");
        policyState.setAvailableNextSteps(availableNextSteps);
        policyState.setEffectiveNextStep(null);

        stepsCompleted.add("start");
        stepsCompleted.add("step2");
        policyState.setStepsCompleted(stepsCompleted);
    }

    @Test
    public void testCacheReadMissingArguments() {
        assertThrows(IllegalArgumentException.class, () -> camelCacheUtil.cacheRead(null, headers));
    }

    @Test
    public void testCacheWriteMissingArguments() {
        Message assertionOneTestMessage = getTestMessage(isMultistep);
        assertionOneTestMessage.removeHeader(CamelCacheUtil.CACHE_KEY);
        assertThrows(IllegalArgumentException.class, () -> camelCacheUtil.implicitCacheWrite(assertionOneTestMessage));
        Message assertionTwoTestMessage = getTestMessage(isMultistep);
        assertionTwoTestMessage.removeHeader(CamelCacheUtil.CACHE_EXPIRATION);
        assertThrows(IllegalArgumentException.class, () -> camelCacheUtil.implicitCacheWrite(assertionTwoTestMessage));
    }

    @Test
    public void testCacheRead() {
        Exchange exchange = new DefaultExchange(context());
        when(cacheService.retrieveFromCache(CACHE_KEY)).thenReturn(policyState);
        camelCacheUtil.cacheRead(CACHE_KEY, exchange.getIn().getHeaders());
        verify(cacheService, times(1)).retrieveFromCache(anyString());
        assertEquals(cacheValue, exchange.getIn().getHeader("cacheValue"));
    }

    @Test
    void testCacheReadMiss() {
        Exchange exchange = new DefaultExchange(context());
        PolicyCacheException expectedException = PolicyCacheException.newReadMissException(CACHE_KEY);
        when(cacheService.retrieveFromCache(CACHE_KEY)).thenReturn(null);
        Exception actualException = assertThrows(PolicyCacheException.class, () -> camelCacheUtil.cacheRead(CACHE_KEY, exchange.getIn().getHeaders()));
        //have to use assertThat with recursive comparison since AbstractChassisException uses reflectionEquals()
        assertThat(actualException).usingRecursiveComparison().isEqualTo(expectedException);
    }

    @Test
    public void testCacheWrite() {
        PolicyState inboundPolicyState = new PolicyState();
        inboundPolicyState.setPolicyName(POLICY_NAME);
        inboundPolicyState.setPolicyVersion(POLICY_VERSION);
        List<String> inboundStepsCompleted = new ArrayList<>();
        inboundStepsCompleted.add("start");
        inboundPolicyState.setStepsCompleted(inboundStepsCompleted);
        Message testMessage = getTestMessage(isMultistep);
        testMessage.setHeader(PolicyConstants.HEADER_POLICYSTATE, inboundPolicyState);
        camelCacheUtil.implicitCacheWrite(testMessage);
        verify(cacheService, times(1)).populateCache(eq(CACHE_KEY), eq(policyState), eq(CACHE_EXPIRATION));
    }

    @Test
    void testCacheWriteCacheServiceFailure() {
        doThrow(new ChassisSystemException("test exception")).when(cacheService).populateCache(any(), any(), any());
        assertThrows(ChassisSystemException.class, () -> camelCacheUtil.implicitCacheWrite(getTestMessage(isMultistep)));
    }

    @Test
    public void testCacheWriteWithReadException() {
        Exchange exchange = new DefaultExchange(context());
        when(cacheService.retrieveFromCache(CACHE_KEY)).thenThrow(ChassisSystemException.class);

        policyState.getStepsCompleted().remove("start");
        Message testMessage = getTestMessage(isMultistep);
        testMessage.removeHeader(PolicyConstants.HEADER_POLICYSTATE);
        camelCacheUtil.implicitCacheWrite(testMessage);
        verify(cacheService, times(1)).populateCache(eq(CACHE_KEY), eq(policyState), eq(CACHE_EXPIRATION));
    }

    @Test
    public void testCacheWriteNullPolicyState() {
        policyState.getStepsCompleted().remove("start");
        Message testMessage = getTestMessage(isMultistep);
        testMessage.removeHeader(PolicyConstants.HEADER_POLICYSTATE);
        camelCacheUtil.implicitCacheWrite(testMessage);
        verify(cacheService, times(1)).populateCache(eq(CACHE_KEY), eq(policyState), eq(CACHE_EXPIRATION));
    }

    @Test
    public void testCacheWriteNullCacheValue() {
        policyState.setPolicyState(new HashMap<>());
        policyState.getStepsCompleted().remove("start");
        Message testMessage = getTestMessage(isMultistep);
        testMessage.removeHeader(CamelCacheUtil.CACHE_VALUE);
        testMessage.removeHeader(PolicyConstants.HEADER_POLICYSTATE);
        camelCacheUtil.implicitCacheWrite(testMessage);
        verify(cacheService, times(1)).populateCache(eq(CACHE_KEY), eq(policyState), eq(CACHE_EXPIRATION));
    }

    @Test
    public void testCacheWriteNullNextSteps() {
        policyState.setAvailableNextSteps(new HashSet<>());
        policyState.setPolicyState(new HashMap<>());
        policyState.getStepsCompleted().remove("start");
        Message testMessage = getTestMessage(isMultistep);
        testMessage.removeHeader(CamelCacheUtil.CACHE_VALUE);
        testMessage.removeHeader(PolicyConstants.HEADER_AVAILABLENEXTSTEPS);
        testMessage.removeHeader(PolicyConstants.HEADER_POLICYSTATE);

        camelCacheUtil.implicitCacheWrite(testMessage);
        verify(cacheService, times(1)).populateCache(eq(CACHE_KEY), eq(policyState), eq(CACHE_EXPIRATION));
    }

    @Test
    public void testCacheWriteHelper() {
        Message testMessage = getTestMessage(isMultistep);
        camelCacheUtil.implicitCacheWrite(testMessage);
        verify(camelCacheUtil, times(1)).implicitCacheWrite(eq(testMessage));
        verify(cacheService, times(1)).populateCache(eq(CACHE_KEY), eq(policyState), eq(CACHE_EXPIRATION));
    }

    @Test
    public void testExplicitCacheWrite() {
        policyState.setStepsCompleted(new ArrayList<>());
        Message testMessage = getTestMessage(!isMultistep);
        for (int i = 0; i < 10; ++i) {
            camelCacheUtil.explicitCacheWrite(testMessage);
        }
        verify(camelCacheUtil, times(10)).explicitCacheWrite(eq(testMessage));
        assertTrue(policyState.getStepsCompleted().isEmpty());

        for (int i = 0; i < 10; ++i) {
            camelCacheUtil.implicitCacheWrite(testMessage);
        }
        assertFalse(policyState.getStepsCompleted().isEmpty());
    }

    @Test
    public void testCacheDelete() {
        camelCacheUtil.cacheDelete(CACHE_KEY);
        verify(cacheService, times(1)).deleteFromCache(eq(CACHE_KEY));
    }

    @Test
    public void testEffectiveStepsCompleted() {
        List<String> stepsCompleted = new ArrayList<>();
        stepsCompleted.add("1");
        stepsCompleted.add("2");
        stepsCompleted.add("3");
        stepsCompleted.add("1");
        stepsCompleted.add("2a");
        stepsCompleted.add("2a");
        stepsCompleted.add("3");

        List<String> expected = new ArrayList<>();
        expected.add("1");
        expected.add("2a");
        expected.add("3");
        assertEquals(expected, camelCacheUtil.effectiveStepsCompleted(stepsCompleted));

        assertEquals(new ArrayList<String>(), camelCacheUtil.effectiveStepsCompleted(new ArrayList<>()));
    }

    @Test
    void testMultistepP2PCacheWrite(){
        //simulate cache retrieval containing child policy information
        PolicyState childPolicyState = new PolicyState();
        childPolicyState.setPolicyState(cacheValue);

        childPolicyState.setPolicyName(POLICY_CHILD_NAME);
        childPolicyState.setPolicyVersion(POLICY_VERSION);

        availableNextSteps.add("step3");
        childPolicyState.setAvailableNextSteps(availableNextSteps);
        childPolicyState.setEffectiveNextStep(null);

        stepsCompleted.add("start");
        stepsCompleted.add("step2");
        childPolicyState.setStepsCompleted(stepsCompleted);

        when(cacheService.retrieveFromCache(any())).thenReturn(childPolicyState);

        Message testMessage = getTestMessage(isMultistep);
        testMessage.removeHeader(PolicyConstants.HEADER_POLICYSTATE);

        // act
        camelCacheUtil.implicitCacheWrite(testMessage);

        //verify
        verify(cacheService, times(1)).populateCache(CACHE_KEY, policyState, CACHE_EXPIRATION);
    }

    private Message getTestMessage(boolean isMultiStep) {
        Message message = new DefaultMessage(context);
        message.setHeader(CamelCacheUtil.CACHE_KEY, CACHE_KEY);
        message.setHeader(CamelCacheUtil.CACHE_VALUE, cacheValue);
        message.setHeader(CamelCacheUtil.CACHE_EXPIRATION, CACHE_EXPIRATION);
        message.setHeader(PolicyConstants.HEADER_POLICYNAME, POLICY_NAME);
        message.setHeader(PolicyConstants.HEADER_POLICYVERSION, POLICY_VERSION);
        message.setHeader(PolicyConstants.HEADER_POLICYSTATE, policyState);
        if (isMultiStep) {
            message.setHeader(PolicyConstants.HEADER_STEPNAME, STEP_NAME);
            message.setHeader(PolicyConstants.HEADER_AVAILABLENEXTSTEPS, availableNextSteps);
        }
        return message;
    }
}
