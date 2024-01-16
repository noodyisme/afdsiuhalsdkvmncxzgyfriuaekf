package com.capitalone.identity.identitybuilder.policycore.crypto;

import com.capitalone.chassis.engine.model.exception.ChassisBusinessException;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.support.DefaultExchange;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;

@ExtendWith(MockitoExtension.class)
class DeCryptProcessorTest {

    @MockBean
    DeCryptProcessor deCryptProcessor;

    Exchange exchange;

    @Mock
    DeCryptService deCryptService;

    AutoCloseable mockitoMocks;

    @BeforeEach
    public void setup() {
        this.mockitoMocks = MockitoAnnotations.openMocks(this);
        CamelContext context = new DefaultCamelContext();
        exchange = new DefaultExchange(context);
        deCryptProcessor = new DeCryptProcessor(deCryptService);
    }

    @AfterEach
    public void tearDown() throws Exception {
        this.mockitoMocks.close();
    }

    @Test
    void testProcessor() throws Exception {
        // prepare
        exchange.getIn().setBody(new HashMap<>());
        HashMap<String, Object> headers = new HashMap<>();

        HashMap<String, String> map = new HashMap<>();
        map.put("lockBoxId", "lockBoxId");
        map.put("productId", "productId");
        map.put("vaultRole", "vaultRole");
        map.put("vaultAddress", "vaultAddress");
        headers.put("decrypt.cosMap", map);
        exchange.getIn().setHeaders(headers);

        Mockito.when(deCryptService.decrypt(anyString(), any())).thenReturn("test");

        // execute
        deCryptProcessor.process(exchange);

        // validate
        assertEquals("test", exchange.getIn().getBody());
    }

    @Test
    void testProcessorThrowsErrorWhenNoLockBoxIdSet() {
        // prepare
        exchange.getIn().setBody(new HashMap<>());
        HashMap<String, Object> headers = new HashMap<>();

        HashMap<String, String> map = new HashMap<>();
        map.put("productId", "productId");
        map.put("vaultRole", "vaultRole");
        map.put("vaultAddress", "vaultAddress");
        headers.put("decrypt.cosMap", map);
        exchange.getIn().setHeaders(headers);

        // execute && validate
        assertThrows(
                ChassisBusinessException.class,
                () -> deCryptProcessor.process(exchange),
                "lockBoxId is required, but [null] found");
    }

    @Test
    void testProcessorThrowsErrorWhenNoProductIdSet() {
        // prepare
        exchange.getIn().setBody(new HashMap<>());
        HashMap<String, Object> headers = new HashMap<>();

        HashMap<String, String> map = new HashMap<>();
        map.put("lockBoxId", "lockBoxId");
        map.put("vaultRole", "vaultRole");
        map.put("vaultAddress", "vaultAddress");
        headers.put("decrypt.cosMap", map);
        exchange.getIn().setHeaders(headers);

        // execute && validate
        assertThrows(
                ChassisBusinessException.class,
                () -> deCryptProcessor.process(exchange),
                "lockBoxId is required, but [null] found");
    }

    @Test
    void testProcessorThrowsErrorWhenNoVaultRoleSet() {
        // prepare
        exchange.getIn().setBody(new HashMap<>());
        HashMap<String, Object> headers = new HashMap<>();

        HashMap<String, String> map = new HashMap<>();
        map.put("lockBoxId", "lockBoxId");
        map.put("productId", "productId");
        map.put("vaultAddress", "vaultAddress");
        headers.put("decrypt.cosMap", map);
        exchange.getIn().setHeaders(headers);

        // execute && validate
        assertThrows(
                ChassisBusinessException.class,
                () -> deCryptProcessor.process(exchange),
                "lockBoxId is required, but [null] found");
    }

    @Test
    void testProcessorThrowsErrorWhenNovaultAddressSet() {
        // prepare
        exchange.getIn().setBody(new HashMap<>());
        HashMap<String, Object> headers = new HashMap<>();

        HashMap<String, String> map = new HashMap<>();
        map.put("lockBoxId", "lockBoxId");
        map.put("productId", "productId");
        map.put("vaultRole", "vaultRole");
        headers.put("decrypt.cosMap", map);
        exchange.getIn().setHeaders(headers);

        // execute && validate
        assertThrows(
                ChassisBusinessException.class,
                () -> deCryptProcessor.process(exchange),
                "lockBoxId is required, but [null] found");
    }

    @Test
    void testProcessorThrowsErrorWhenNoCosMapSet() {
        // prepare
        exchange.getIn().setBody(new HashMap<>());
        HashMap<String, Object> headers = new HashMap<>();
        exchange.getIn().setHeaders(headers);

        // execute && validate
        assertThrows(
                ChassisBusinessException.class,
                () -> deCryptProcessor.process(exchange),
                "headers must contains decrypt.cosMap, but [null] found");
    }


}