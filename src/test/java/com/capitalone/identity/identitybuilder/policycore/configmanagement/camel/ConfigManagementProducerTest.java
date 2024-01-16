package com.capitalone.identity.identitybuilder.policycore.configmanagement.camel;

import com.capitalone.identity.identitybuilder.configmanagement.ConfigManagementModel;
import com.capitalone.identity.identitybuilder.policycore.camel.PolicyConstants;
import com.capitalone.identity.identitybuilder.policycore.configmanagement.UnsupportedBusinessEventException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.apache.camel.Exchange;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.support.DefaultExchange;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class ConfigManagementProducerTest {

    private static final ObjectMapper mapper = new ObjectMapper();

    @Mock
    ConfigManagementEndpoint endpoint;

    @Test
    void onCreateNullPointerExceptions() {
        ConfigManagementModel config = ConfigManagementModel.newInstance(new HashMap<>(), new HashMap<>());
        assertThrows(NullPointerException.class, () -> new ConfigManagementProducer(null, null, "test", "outputHeader"));
        assertThrows(NullPointerException.class, () -> new ConfigManagementProducer(null, config, null, "outputHeader"));
        assertThrows(NullPointerException.class, () -> new ConfigManagementProducer(null, config, "test", null));
    }

    @SneakyThrows
    @ParameterizedTest
    @ValueSource(strings = {
            "config.hello",
            "configArray",
            "isOkay",
    })
    void doStartOkay(String property) {

        Map<String, Serializable> defaults = new HashMap<>();
        defaults.put("config.hello", "123");
        defaults.put("configArray", new Object[0]);
        defaults.put("isOkay", true);
        ConfigManagementModel config = ConfigManagementModel.newInstance(defaults, new HashMap<>());

        ConfigManagementProducer producer = new ConfigManagementProducer(endpoint,
                config, property, "outputHeader");

        assertDoesNotThrow(producer::doStart);
    }

    @SneakyThrows
    @ParameterizedTest
    @ValueSource(strings = {
            "abc",
            "def",
    })
    void invalidPropertyThrows(String property) {

        Map<String, Serializable> defaults = new HashMap<>();
        defaults.put("config.hello", "123");
        defaults.put("configArray", new Object[0]);
        defaults.put("isOkay", true);
        ConfigManagementModel config = ConfigManagementModel.newInstance(defaults, new HashMap<>());

        ConfigManagementProducer producer = new ConfigManagementProducer(endpoint,
                config, property, "outputHeader");

        // throw when doStart called
        assertThrows(UnsupportedOperationException.class, producer::doStart);

        // throw when process is called
        Exchange ex = new DefaultExchange(new DefaultCamelContext());
        ex.getMessage().setHeader(PolicyConstants.HEADER_BUSINESS_EVENT, null);
        assertThrows(RuntimeException.class, () -> producer.process(ex));
    }

    @SneakyThrows
    @ParameterizedTest
    @ValueSource(strings = {
            "isString",
            "isStringArray",
            "isBoolean",
            "isBooleanArray",
            "isDouble",
            "isDoubleArray",
            "isInteger",
            "isIntegerArray",
    })
    void process(String property) {
        Map<String, Serializable> defaults = new HashMap<>();
        defaults.put("isString", "123");
        defaults.put("isStringArray", new String[]{"test", "test2"});
        defaults.put("isBoolean", true);
        defaults.put("isBooleanArray", new boolean[]{true, false});
        defaults.put("isDouble", 1.2);
        defaults.put("isDoubleArray", new Double[]{1.2, 5.6});
        defaults.put("isInteger", 1);
        defaults.put("isIntegerArray", new int[]{1, 2});

        ConfigManagementModel config = ConfigManagementModel.newInstance(defaults, new HashMap<>());
        ConfigManagementProducer producer = new ConfigManagementProducer(endpoint,
                config, property, "outputHeader");

        Exchange ex = new DefaultExchange(new DefaultCamelContext());
        ex.getMessage().setHeader(PolicyConstants.HEADER_BUSINESS_EVENT, null);

        producer.process(ex);

        Serializable result = (Serializable) ex.getMessage().getHeader("outputHeader");
        String serializedResult = mapper.writeValueAsString(result);
        String serializedExpected = mapper.writeValueAsString(defaults.get(property));
        assertEquals(serializedExpected, serializedResult);
    }

    @Test
    void missingUseCaseThrows() {
        String PROPERTY = "isString";

        Map<String, Serializable> defaults = new HashMap<>();
        defaults.put(PROPERTY, "123");

        Map<String, Map<String, Serializable>> useCases = new HashMap<>();
        Map<String, Serializable> useCaseA = new HashMap<>();
        useCaseA.put(PROPERTY, "ABC");
        useCases.put("useCaseA", useCaseA);
        useCases.put("LOB.DIV.CHANNEL.APP", useCaseA);


        ConfigManagementModel config = ConfigManagementModel.newInstance(defaults, useCases);
        ConfigManagementProducer producer = new ConfigManagementProducer(endpoint,
                config, PROPERTY, "outputHeader");

        // start should work
        assertDoesNotThrow(producer::doStart);

        // normal usecase should work ok
        Exchange ex = new DefaultExchange(new DefaultCamelContext());
        ex.getMessage().setHeader(PolicyConstants.HEADER_BUSINESS_EVENT, "useCaseA");
        assertDoesNotThrow(() -> producer.process(ex));

        // missing usecase exception
        String MISSING_USECASE = "useCaseX";
        Exchange exs = new DefaultExchange(new DefaultCamelContext());
        exs.getMessage().setHeader(PolicyConstants.HEADER_BUSINESS_EVENT, MISSING_USECASE);
        RuntimeException exception = assertThrows(UnsupportedBusinessEventException.class, () -> producer.process(exs));
        assertTrue(exception.getMessage().contains(MISSING_USECASE));

        // missing event level, but valid app level use case works ok
        String appLevelNoEventLevel = "LOB.DIV.CHANNEL.APP.missing-usecase";
        Exchange message = new DefaultExchange(new DefaultCamelContext());
        message.getMessage().setHeader(PolicyConstants.HEADER_BUSINESS_EVENT, appLevelNoEventLevel);
        assertDoesNotThrow(() -> producer.process(ex));

    }

}
