package com.capitalone.identity.identitybuilder.policycore.service.exception;

import com.capitalone.identity.identitybuilder.policycore.model.ErrorInfo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EmptySource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

public class ForwardedDownstreamExceptionTest {

    private static final String DOWNSTREAM_ERROR_ID = "202020";
    private static final String DOWNSTREAM_ERROR_TEXT = "Downstream API error";

    private void validateErrorInfo(ErrorInfo expectedInfo, CustomPolicyException e) {
        ErrorInfo info = e.getErrorInfo();
        assertEquals(expectedInfo.getId(), info.getId());
        assertEquals(expectedInfo.getText(), info.getText());
        assertEquals(expectedInfo.getDeveloperText(), info.getDeveloperText());

    }

    @Test
    public void testDefaultDownstreamError() {
        String FORWARD_DOWNSTREAM_ERROR_MSG = "The risk domain policy \\\"ecommerce_google_risk_assessment\\\" version \\\"1.1\\\" " +
                "(post method) returned the following error \\\"{errorInfo={id=200002, text=Unexpected error, " +
                "developerText=java.lang.StringIndexOutOfBoundsException: String index out of range: -1}," +
                " policyStatus=FAILURE, results=null}\\\"";
        CustomPolicyException exception = new ForwardedDownstreamException(FORWARD_DOWNSTREAM_ERROR_MSG);
        assertEquals(DOWNSTREAM_ERROR_ID, exception.getId());
        assertEquals(DOWNSTREAM_ERROR_TEXT, exception.getText());
        assertNotNull(exception.getDeveloperText());
        assertEquals(FORWARD_DOWNSTREAM_ERROR_MSG, exception.getDeveloperText());
    }


    @ParameterizedTest
    @ValueSource(strings = {
            "RISK123SYSTEMERROR"
    })
    void testScenariosForVariousExceptionMessages(String errorMessage) {
        CustomPolicyException exception = new ForwardedDownstreamException(errorMessage);
        assertEquals(DOWNSTREAM_ERROR_ID, exception.getId());
        assertEquals(DOWNSTREAM_ERROR_TEXT, exception.getText());
        assertEquals(errorMessage, exception.getDeveloperText());
        assertNotNull(exception.getDeveloperText());
    }


    @ParameterizedTest
    @NullSource
    void testScenariosWithNullMessage(String errorMessage) {
        CustomPolicyException exception = new ForwardedDownstreamException(errorMessage);
        assertEquals(DOWNSTREAM_ERROR_ID, exception.getId());
        assertEquals(DOWNSTREAM_ERROR_TEXT, exception.getText());
        assertNull(exception.getDeveloperText());
    }

    private static Stream<ErrorInfo> provideErrorInfo() {
        return Stream.of(
                new ErrorInfo("0", "this is a test", "the exception was forwarded."),
                new ErrorInfo(null, "no id given", "yep, definitely no id here."),
                new ErrorInfo("808080", null, "that text was null"),
                new ErrorInfo("101010", "no dev text given", null),
                new ErrorInfo(null, null, null)
        );
    }

    @ParameterizedTest
    @MethodSource("provideErrorInfo")
    public void testFowardedDownstreamException_proxyPass(ErrorInfo errorInfo) {
        CustomPolicyException exception = new ForwardedDownstreamException(errorInfo);
        validateErrorInfo(errorInfo, exception);
        assertEquals(errorInfo.getText(), exception.getMessage());
    }

    @ParameterizedTest
    @EmptySource
    void testScenariosWithEmptyMessage(String errorMessage) {
        CustomPolicyException exception = new ForwardedDownstreamException(errorMessage);
        assertEquals(DOWNSTREAM_ERROR_ID, exception.getId());
        assertEquals(DOWNSTREAM_ERROR_TEXT, exception.getText());
        assertEquals("", exception.getDeveloperText());
    }

}

