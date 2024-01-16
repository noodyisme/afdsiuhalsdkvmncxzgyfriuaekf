package com.capitalone.identity.identitybuilder.policycore.service.exception;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EmptySource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

public class OverridePolicyErrorInfoExceptionTest {

    private static final String DOWNSTREAM_ERROR_ID = "202020";
    private static final String DOWNSTREAM_ERROR_TEXT = "Downstream API error";

    @Test
    public void testOverrideException() {
        String OVERRIDE_ERROR_MSG = "RISK123~SYSTEM ERROR~INVALID DATA";
        CustomPolicyException exception = new OverridePolicyErrorInfoException(OVERRIDE_ERROR_MSG);
        assertEquals("RISK123", exception.getId());
        assertEquals("SYSTEM ERROR", exception.getText());
        assertEquals("INVALID DATA", exception.getDeveloperText());
        assertNull(exception.getAdditionalDetails());
    }

    @Test
    public void testOverrideExceptionWithAdditionalDetails(){
        String OVERRIDE_ERROR_MSG = "RISK123~SYSTEM ERROR~INVALID DATA~KEY1:VALUE1";
        CustomPolicyException exception = new OverridePolicyErrorInfoException(OVERRIDE_ERROR_MSG);
        assertEquals("RISK123", exception.getId());
        assertEquals("SYSTEM ERROR", exception.getText());
        assertEquals("INVALID DATA", exception.getDeveloperText());
        assertEquals("VALUE1", exception.getAdditionalDetails().get("KEY1"));
    }

    @Test
    public void testOverrideExceptionWithTwoAdditionalDetails(){
        String OVERRIDE_ERROR_MSG = "RISK123~SYSTEM ERROR~INVALID DATA~KEY1:VALUE1~KEY2:VALUE2";
        CustomPolicyException exception = new OverridePolicyErrorInfoException(OVERRIDE_ERROR_MSG);
        assertEquals("RISK123", exception.getId());
        assertEquals("SYSTEM ERROR", exception.getText());
        assertEquals("INVALID DATA", exception.getDeveloperText());
        assertEquals("VALUE1", exception.getAdditionalDetails().get("KEY1"));
        assertEquals("VALUE2", exception.getAdditionalDetails().get("KEY2"));
    }

    @Test
    public void testOverrideExceptionWithDetailsHangingTelda(){
        String OVERRIDE_ERROR_MSG = "RISK123~SYSTEM ERROR~INVALID DATA~KEY1:VALUE1~";
        CustomPolicyException exception = new OverridePolicyErrorInfoException(OVERRIDE_ERROR_MSG);
        assertEquals("RISK123", exception.getId());
        assertEquals("SYSTEM ERROR", exception.getText());
        assertEquals("INVALID DATA", exception.getDeveloperText());
        assertEquals("VALUE1", exception.getAdditionalDetails().get("KEY1"));
    }

    @Test
    public void testOverrideExceptionWithDetailsHangingColon(){
        String OVERRIDE_ERROR_MSG = "RISK123~SYSTEM ERROR~INVALID DATA~KEY1:";
        CustomPolicyException exception = new OverridePolicyErrorInfoException(OVERRIDE_ERROR_MSG);
        assertEquals("RISK123", exception.getId());
        assertEquals("SYSTEM ERROR", exception.getText());
        assertEquals("INVALID DATA", exception.getDeveloperText());
        assertEquals(null, exception.getAdditionalDetails().get("KEY1"));
    }

    @Test
    public void testOverrideExceptionWithDetailsNoColon(){
        String OVERRIDE_ERROR_MSG = "RISK123~SYSTEM ERROR~INVALID DATA~KEY1";
        CustomPolicyException exception = new OverridePolicyErrorInfoException(OVERRIDE_ERROR_MSG);
        assertEquals("RISK123", exception.getId());
        assertEquals("SYSTEM ERROR", exception.getText());
        assertEquals("INVALID DATA", exception.getDeveloperText());
        assertEquals(null, exception.getAdditionalDetails().get("KEY1"));
    }

    @Test
    public void testOverrideExceptionWithSupressDevText() {
        String OVERRIDE_ERROR_MSG_SUPPRESS_DEVTEXT = "RISK123~SYSTEM ERROR";
        CustomPolicyException exception = new OverridePolicyErrorInfoException(OVERRIDE_ERROR_MSG_SUPPRESS_DEVTEXT);
        assertEquals("RISK123", exception.getId());
        assertEquals("SYSTEM ERROR", exception.getText());
        assertNull(exception.getDeveloperText());
    }

    @Test
    public void testOverrideExceptionWithNoDevText() {
        String OVERRIDE_ERROR_MSG_NO_DEVTEXT = "RISK123~SYSTEM ERROR";
        CustomPolicyException exception = new OverridePolicyErrorInfoException(OVERRIDE_ERROR_MSG_NO_DEVTEXT);
        assertEquals("RISK123", exception.getId());
        assertEquals("SYSTEM ERROR", exception.getText());
        assertNull(exception.getDeveloperText());

    }

    @Test
    public void testOverrideExceptionWithMoreThanThreeDelimiters() {
        String OVERRIDE_ERROR_MSG_WITH_MORE_DELIMITER = "RISK123~SYSTEM ERROR~devText~unknown";
        CustomPolicyException exception = new OverridePolicyErrorInfoException(OVERRIDE_ERROR_MSG_WITH_MORE_DELIMITER);
        assertEquals("RISK123", exception.getId());
        assertEquals("SYSTEM ERROR", exception.getText());
        assertNotNull(exception.getDeveloperText());
        assertEquals("devText", exception.getDeveloperText());

    }

    @ParameterizedTest
    @ValueSource(strings = {
            "~"
    })
    void testScenarioWithMessageHasonlyTilda(String errorMessage) {
        CustomPolicyException exception = new OverridePolicyErrorInfoException(errorMessage);
        assertNull(exception.getId());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "123~"
    })
    void testScenarioWithMessageHasIdAndTilda(String errorMessage) {
        CustomPolicyException exception = new OverridePolicyErrorInfoException(errorMessage);
        assertNotNull(exception.getId());
        assertNull(exception.getText());
    }

    @ParameterizedTest
    @NullSource
    void testScenariosWithNullMessage(String errorMessage) {
        CustomPolicyException exception = new OverridePolicyErrorInfoException(errorMessage);
        assertEquals(DOWNSTREAM_ERROR_ID, exception.getId());
        assertEquals(DOWNSTREAM_ERROR_TEXT, exception.getText());
        assertNull(exception.getDeveloperText());
    }

    @ParameterizedTest
    @EmptySource
    void testScenariosWithEmptyMessage(String errorMessage) {
        CustomPolicyException exception = new OverridePolicyErrorInfoException(errorMessage);
        assertEquals(DOWNSTREAM_ERROR_ID, exception.getId());
        assertEquals(DOWNSTREAM_ERROR_TEXT, exception.getText());
        assertEquals("", exception.getDeveloperText());
    }
}

