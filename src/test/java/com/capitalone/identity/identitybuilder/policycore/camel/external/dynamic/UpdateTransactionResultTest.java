package com.capitalone.identity.identitybuilder.policycore.camel.external.dynamic;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class UpdateTransactionResultTest {

    @Test
    void constructor() {
        assertThrows(NullPointerException.class, () -> UpdateTransactionResult.error(null));
        assertDoesNotThrow(UpdateTransactionResult::success);
    }

    @Test
    void successStatus() {
        UpdateTransactionResult result = UpdateTransactionResult.success();
        assertTrue(result.isSuccess());
        assertFalse(result.isError());
        assertNull(result.getError());
        assertEquals(UpdateTransactionResult.Status.SUCCESS, result.getStatus());
    }

    @Test
    void rolledBackStatus() {
        IllegalArgumentException test = new IllegalArgumentException("test");
        UpdateTransactionResult result = UpdateTransactionResult.error(test);
        assertFalse(result.isSuccess());
        assertTrue(result.isError());
        assertNotNull(result.getError());
        assertEquals(UpdateTransactionResult.Status.ERROR, result.getStatus());
    }

    @Test
    void serializeErrorTest() throws JsonProcessingException {
        final ObjectMapper objectMapper = new ObjectMapper();
        IllegalArgumentException test = new IllegalArgumentException("test");
        UpdateTransactionResult object = UpdateTransactionResult.error(test);
        String serialized = objectMapper.writeValueAsString(object);

        Map<String,Object> result = (Map<String,Object>)objectMapper.readValue(serialized, HashMap.class);

        assertEquals("ERROR", result.get("status"));
        assertEquals(false, result.get("success"));
        assertNull(result.get("error"));
    }
}
