package com.capitalone.identity.identitybuilder.policycore.policytopolicy.camel;

import com.capitalone.chassis.engine.model.exception.AbstractChassisException;
import com.capitalone.identity.identitybuilder.model.*;
import com.capitalone.identity.identitybuilder.model.parsing.PolicyDefinition;
import com.capitalone.identity.identitybuilder.policycore.camel.PolicyConstants;
import com.capitalone.identity.identitybuilder.policycore.camel.external.dynamic.PolicyValidator;
import com.capitalone.identity.identitybuilder.policycore.model.ErrorInfo;
import com.capitalone.identity.identitybuilder.policycore.model.PolicyResponse;
import com.capitalone.identity.identitybuilder.policycore.model.PolicyStatus;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.PolicyEvaluatedAuditor;
import com.capitalone.identity.identitybuilder.policycore.service.PolicyService;
import com.capitalone.identity.identitybuilder.policycore.service.logging.AuditLogger;
import com.capitalone.identity.identitybuilder.policycore.service.logging.PolicyServiceExecutor;
import com.capitalone.identity.identitybuilder.policycore.service.versions.PolicyVersionService;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.support.DefaultExchange;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.convert.ConversionService;

import java.util.*;

import static com.capitalone.identity.identitybuilder.policycore.camel.util.CamelCacheUtil.CACHE_KEY;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PolicyToPolicyProducerTest {

    @Mock
    PolicyService.PolicyExecutionHelper policyExecutionHelper;
    @Mock
    PolicyToPolicyEndpoint policyToPolicyEndpoint;
    @Mock
    PolicyValidator policyValidator;
    @Mock
    PolicyVersionService policyVersionService;
    @Mock
    Message message;
    @Mock
    PolicyService.ExecutedPolicyResponse executedPolicyResponse;
    PolicyServiceExecutor policyServiceExecutor;

    @BeforeEach
    void setup() {
        policyServiceExecutor = new PolicyServiceExecutor(
                Mockito.mock(AuditLogger.class), Mockito.mock(PolicyEvaluatedAuditor.class),
                Mockito.mock(ConversionService.class), policyExecutionHelper);
    }

    @Test
    void onCreateNullPointerExceptions() {
        assertThrows(NullPointerException.class, () -> new PolicyToPolicyProducer(null, policyServiceExecutor, policyValidator, policyVersionService, "test", "test", "test"));
        assertThrows(NullPointerException.class, () -> new PolicyToPolicyProducer(policyToPolicyEndpoint,null, policyValidator, policyVersionService, "test", "test", "test"));
        assertThrows(NullPointerException.class, () -> new PolicyToPolicyProducer(policyToPolicyEndpoint, policyServiceExecutor, null, policyVersionService, "test", "test", "test"));
        assertThrows(NullPointerException.class, () -> new PolicyToPolicyProducer(policyToPolicyEndpoint, policyServiceExecutor, policyValidator, null, "test", "test", "test"));
        assertThrows(NullPointerException.class, () -> new PolicyToPolicyProducer(policyToPolicyEndpoint, policyServiceExecutor, policyValidator, policyVersionService, null, "test", "test"));
        assertThrows(NullPointerException.class, () -> new PolicyToPolicyProducer(policyToPolicyEndpoint, policyServiceExecutor, policyValidator, policyVersionService, "test", null, "test"));
        assertThrows(NullPointerException.class, () -> new PolicyToPolicyProducer(policyToPolicyEndpoint, policyServiceExecutor, policyValidator, policyVersionService, "test", "test", null));
    }

    @Test
    void process() {
        Exchange exchange = new DefaultExchange(new DefaultCamelContext());
        exchange.setProperty("policyVersion", "1.0");
        exchange.setProperty("policyName", "test");
        exchange.setIn(message);
        when(message.getHeader(PolicyConstants.HEADER_BUSINESS_EVENT)).thenReturn("testing");
        PolicyToPolicyProducer policyToPolicyProducer = new PolicyToPolicyProducer(policyToPolicyEndpoint,
                policyServiceExecutor, policyValidator, policyVersionService, "test", "test", "test");

        when(policyToPolicyEndpoint.getPolicyVersion()).thenReturn("1");

        when(message.getHeader(PolicyConstants.HEADER_AVAILABLENEXTSTEPS)).thenReturn("testing");
        Throwable throwable = assertThrows(PolicyToPolicyException.class, () -> policyToPolicyProducer.process(exchange));
        assertTrue(throwable.getMessage().contains("The camel header \"" + PolicyConstants.HEADER_AVAILABLENEXTSTEPS));

        when(message.getHeader(PolicyConstants.HEADER_AVAILABLENEXTSTEPS)).thenReturn(null);

        throwable = assertThrows(PolicyToPolicyException.class, () -> policyToPolicyProducer.process(exchange));
        assertTrue(throwable.getMessage().contains("least one minor version is ACTIVE and that version forwarder is enabled"));

        when(policyToPolicyEndpoint.getPolicyVersion()).thenReturn("1.0");
        throwable = assertThrows(PolicyToPolicyException.class, () -> policyToPolicyProducer.process(exchange));
        assertTrue(throwable.getMessage().contains("P2P unknown child policy: \"" + policyToPolicyEndpoint.getEndpointUri() + "\""));

        when(policyToPolicyEndpoint.getPolicyName()).thenReturn("a");
        PolicyInfo.Patch minor = getMockPolicyInfo("name", new ConfigStoreItemInfo("O/b/c/1.0/process/a.xml", "a"));
        when(policyVersionService.getPolicyVersion(anyString(), anyString())).thenReturn(minor);
        String name = "policy:name_1.0.0";

        throwable = assertThrows(PolicyToPolicyException.class, () -> policyToPolicyProducer.process(exchange));
        assertTrue(throwable.getMessage().contains("being invoked by P2P must be marked as child policies"));

        Map<String, String> stringMap = new HashMap<>();
        stringMap.put("x-journey-policy", "test");
        Set<String> childPolicySet = new HashSet<>();
        childPolicySet.add(name);
        when(policyValidator.getChildPolicies()).thenReturn(childPolicySet);
        when(message.getHeader(PolicyConstants.HEADER_DXHEADERS)).thenReturn(stringMap);
        when(message.getBody()).thenReturn("testing");
        throwable = assertThrows(PolicyToPolicyException.class, () -> policyToPolicyProducer.process(exchange));
        assertTrue(throwable.getMessage().contains("Error converting the parent policy body to a Map when"));

        when(message.getBody()).thenReturn(stringMap);
        when(message.getHeader(PolicyConstants.HEADER_CLIENTID)).thenReturn("test");
        when(message.getHeader(CACHE_KEY)).thenReturn("test");
        when(policyExecutionHelper.executePolicy(any())).thenReturn(executedPolicyResponse);
        when(executedPolicyResponse.getResult()).thenReturn(new PolicyService.Result.SystemError(new AbstractChassisException() {
            @Override
            public int getHttpStatusCode() {
                return 0;
            }

            @Override
            public String getMessage() {
                return "testing message";
            }
        }));
        throwable = assertThrows(PolicyToPolicyException.class, () -> policyToPolicyProducer.process(exchange));
        assertTrue(throwable.getMessage().contains("returned the following error: testing message"));
        when(executedPolicyResponse.getResult()).thenReturn(new PolicyService.Result.PolicySuccess(PolicyResponse.builder().policyStatus(PolicyStatus.FAILURE).results(null).errorInfo(new ErrorInfo("1", "test", "test")).build()));
        PolicyFailureException policyFailureException = assertThrows(PolicyFailureException.class, () -> policyToPolicyProducer.process(exchange));
        assertTrue(policyFailureException.id.contains("1"));
        assertTrue(policyFailureException.text.contains("test"));
        assertTrue(policyFailureException.developerText.contains("test"));
        when(executedPolicyResponse.getResult()).thenReturn(new PolicyService.Result.PolicySuccess(PolicyResponse.builder().policyStatus(PolicyStatus.SUCCESS).results(stringMap).build()));
        assertDoesNotThrow(() -> policyToPolicyProducer.process(exchange));
    }

    @Test
    void getEndpoint() {
        PolicyToPolicyProducer policyToPolicyProducer = new PolicyToPolicyProducer(policyToPolicyEndpoint,
                policyServiceExecutor, policyValidator, policyVersionService, "test", "test", "test");
        assertEquals(policyToPolicyEndpoint, policyToPolicyProducer.getEndpoint());
    }

    private EntityInfo.Policy getMockPolicyInfo(String policyPath, ConfigStoreItemInfo... items) {
        PolicyDefinition definition = new PolicyDefinition(policyPath, "1.0");
        return new EntityInfo.Policy(definition, new HashSet<>(Arrays.asList(items)));
    }
}