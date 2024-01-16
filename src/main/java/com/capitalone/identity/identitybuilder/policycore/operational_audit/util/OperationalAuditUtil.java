package com.capitalone.identity.identitybuilder.policycore.operational_audit.util;

import com.capitalone.chassis.engine.model.context.RequestContext;
import com.capitalone.chassis.engine.model.context.RequestContextHolder;
import com.capitalone.chassis.engine.model.error.ErrorResponse;
import com.capitalone.chassis.engine.model.exception.AbstractChassisException;
import com.capitalone.chassis.engine.model.exception.ChassisErrorCode;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.model.OperationalError;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.model.OperationalRequestContext;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.model.policy.OperationalExecutionContext;
import com.capitalone.identity.identitybuilder.policycore.service.constants.ApplicationConstants;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.http.HttpHeaders;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Log4j2
public class OperationalAuditUtil {
    public static final String DEFAULT_POLICY_ERROR_TEXT = "An error occurred during policy evaluation.";

    /**
     * Extracts the common request header params from the chassis context.
     * WARNING: MAKE SURE TO CALL THIS ONLY FROM THE MAIN REQUEST THREAD!
     *
     * @return the header params for auditing
     */
    public static OperationalRequestContext getChassisRequestContext() {
        final RequestContext chassisRequestContext = RequestContextHolder.getRequestContextOrDefault();
        return OperationalRequestContext.builder()
                .correlationId(chassisRequestContext.getCorrelationId())
                .messageId(chassisRequestContext.getMessageId())
                .build();
    }

    /**
     * Merges the message and stack trace from the throwable into the provided operational error
     *
     * @param operationalError the operational error
     * @param throwable        the throwable
     * @return the operational error with throwable data merged
     */
    public static OperationalError mergeThrowableInfo(
            final OperationalError operationalError,
            final Optional<Throwable> throwable) {
        if (!throwable.isPresent()) {
            return operationalError;
        }
        if (throwable.get().getCause() instanceof StackOverflowError) { // to avoid auditing circular references
            return operationalError.toBuilder()
                    .developerText("StackOverflowError detected.")
                    .stackTrace(null)
                    .build();
        }
        return operationalError.toBuilder()
                .developerText(ExceptionUtils.getMessage(throwable.get()))
                .stackTrace(ExceptionUtils.getStackTrace(throwable.get()))
                .build();
    }

    /**
     * Populates error info from chassis error response if present and exception
     *
     * @param chassisErrorResponseOptional the chassis error response which may be null
     * @param chassisException             the chassis exception being audited
     * @return the error info
     */
    public static OperationalError toChassisOperationalError(final Optional<ErrorResponse> chassisErrorResponseOptional,
                                                             final AbstractChassisException chassisException) {
         // TODO Add List<ErrorDetail> as a separate, optional, OperationalError field.
         // Must also be added to schema. Should ErrorDetails be added as supplemental data instead?
        List<ChassisErrorCode> errorDetails = Optional.ofNullable(chassisException.getApiError())
                .map(ChassisErrorCode::getErrorDetails)
                .orElse(new ArrayList<>());
        return OperationalError.builder()
                .text(chassisErrorResponseOptional.map(ErrorResponse::getText).orElse(DEFAULT_POLICY_ERROR_TEXT))
                .developerText(chassisErrorResponseOptional.map(ErrorResponse::getDeveloperText)
                        .orElse(chassisException.getMessage() != null ? chassisException.getMessage() : errorDetails.toString()))
                .errorId(chassisErrorResponseOptional.map(ErrorResponse::getId).orElse(null))
                .stackTrace(ExceptionUtils.getStackTrace(chassisException))
                .build();
    }

    /**
     * This is a temporary stopgap measure due to a mistake made during initial schema registration for mb 2.0
     * TODO remove this method once header_value is changed from type 'array' to type 'string' in platform schema
     *
     * @param headerValue the header value
     * @return the headerValue in the type matching the schema
     */
    public static Object headerValueStopGap(final String headerValue) {
        return Collections.singletonList(headerValue);
    }

    /**
     * Iterates the provided header entries, appending any non-customer related
     * entries to the supplemental atrributes list provided. Appends to execution
     * context if matching one of its fields.
     *
     * @param httpHeaders             the http headers
     * @param executionContextBuilder the operational execution context builder
     */
    public static void appendNonCuREHeaders(final HttpHeaders httpHeaders,
                                            final OperationalExecutionContext.OperationalExecutionContextBuilder executionContextBuilder) {

        // Populate any execution context attributes if present
        final InternalHeaderAuditHelper headerAuditHelper = new InternalHeaderAuditHelper(httpHeaders);
        executionContextBuilder.apiKey(headerAuditHelper.getValueOrNull(ApplicationConstants.API_KEY));
        executionContextBuilder.contentType(headerAuditHelper.getValueOrNull(HttpHeaders.CONTENT_TYPE));
        executionContextBuilder.channelType(headerAuditHelper.getValueOrNull(ApplicationConstants.CHANNEL_TYPE));
        executionContextBuilder.countryCode(headerAuditHelper.getValueOrNull(ApplicationConstants.COUNTRY_CODE));
        executionContextBuilder.acceptLanguage(headerAuditHelper.getValueOrNull(HttpHeaders.ACCEPT_LANGUAGE));
        executionContextBuilder.subDomain(headerAuditHelper.getValueOrNull("Sub-Domain"));
        executionContextBuilder.userAgent(headerAuditHelper.getValueOrNull(HttpHeaders.USER_AGENT));
        executionContextBuilder.accept(headerAuditHelper.getValueOrNull(HttpHeaders.ACCEPT));
        executionContextBuilder.businessEvent(headerAuditHelper.getValueOrNull("Business-Event"));
        executionContextBuilder.xJourneyPolicy(headerAuditHelper.getValueOrNull(ApplicationConstants.X_JOURNEY_POLICY));
        executionContextBuilder.domain(headerAuditHelper.getValueOrNull("Domain"));
    }

    @AllArgsConstructor
    private static final class InternalHeaderAuditHelper {

        static String fromStringListToString(final List<String> stringList) {
            // Null / empty list check
            if (stringList == null || stringList.isEmpty()) {
                return null;
            }

            // Convert list to string
            final String valueString;
            if (stringList.size() == 1) {
                valueString = stringList.get(0);
            } else {
                valueString = stringList.toString();
            }
            if (StringUtils.isEmpty(valueString)) {
                return null;
            }
            return valueString;
        }

        private final HttpHeaders httpHeaders;

        public String getValueOrNull(final String key) {
            return fromStringListToString(httpHeaders.get(key));
        }

    }

    private OperationalAuditUtil() {
    }

}
