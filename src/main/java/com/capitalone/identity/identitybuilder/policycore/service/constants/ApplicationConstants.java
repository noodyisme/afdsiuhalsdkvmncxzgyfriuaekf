package com.capitalone.identity.identitybuilder.policycore.service.constants;

/**
 * @author xzn789
 */
public final class ApplicationConstants {
    public static final String INVALID_ATTRIBUTE_ID = "201216";
    public static final String INVALID_ATTRIBUTE_TEXT = "INVALID_ATTRIBUTE_TEXT";
    public static final String INVALID_ATTRIBUTE_DEV_TEXT = "INVALID_ATTRIBUTE_DEV_TEXT";
    public static final String INVALID_JSON_ENTITY_ID = "201217";
    public static final String INVALID_JSON_ENTITY_TEXT = "INVALID_JSON_ENTITY_TEXT";
    public static final String INVALID_JSON_ENTITY_DEV_TEXT = "INVALID_JSON_ENTITY_DEV_TEXT";
    public static final String INVALID_BUSINESS_EVENT_ID = "201218";
    public static final String INVALID_BUSINESS_EVENT_TEXT = "INVALID_BUSINESS_EVENT_TEXT";
    public static final String INVALID_BUSINESS_EVENT_DEV_TEXT = "INVALID_BUSINESS_EVENT_DEV_TEXT";
    public static final String MISSING_BUSINESS_EVENT_DEV_TEXT = "MISSING_BUSINESS_EVENT_DEV_TEXT";
    public static final String ABAC_CLIENT_UNAUTHORIZED_TEXT = "ABAC_CLIENT_UNAUTHORIZED_TEXT";
    public static final String ABAC_CLIENT_UNAUTHORIZED_DEV_TEXT = "ABAC_CLIENT_UNAUTHORIZED_DEV_TEXT";
    public static final String ABAC_CLIENT_DENIED_ERROR_CODE = "401003";
    public static final String ABAC_CLIENT_DOES_NOT_EXIST_ERROR_CODE = "401002";
    public static final String ABAC_POLICY_ACCESS_UNAVAILABLE_ERROR_CODE = "401001";
    public static final String ABAC_CLIENT_DENIED_DEV_TEXT = "Client is unauthorized as client effect is DENY";
    public static final String ABAC_CLIENT_DOES_NOT_EXIST_DEV_TEXT = "Client is unauthorized as access is not configured for subject";
    public static final String ABAC_POLICY_ACCESS_UNAVAILABLE_DEV_TEXT = "Client is unauthorized as there are no accesses available";
    public static final String POLICY_TO_POLICY_ERROR_CODE = "203000";
    public static final String POLICY_TO_POLICY_TEXT = "Policy to Policy Component Error";
    public static final String CLIENT_CORRELATION_ID = "Client-Correlation-Id";
    public static final String CUSTOMER_IP_ADDR = "Customer-IP-Address";
    public static final String API_KEY = "Api-key";
    public static final String CLIENT_API_KEY = "Client-Api-Key";
    public static final String COUNTRY_CODE = "Country-Code";
    public static final String CHANNEL_TYPE = "Channel-Type";
    public static final String X_JOURNEY_POLICY = "x-journey-policy";
    public static final String SESSION_CORRELATION_ID = "session-correlation-id";
    public static final String POLICY_CORE_API_REQUEST = "5000653";
    public static final String POLICY_CORE_DOWNSTREAM_API_CALL = "5000654";
    public static final String MASTERBUILDER_RULES_AUDIT_EVENT_NAME = "IdentityBuilder-MasterBuilder.RulesAudit";
    public static final String MASTERBUILDER_EXTERNAL_DYNAMIC_EVENT_ROOT = "idb-policy-core.runtime-reload";
    public static final String EXTERNAL_DYNAMIC_EVENT_NAME_SCAN = MASTERBUILDER_EXTERNAL_DYNAMIC_EVENT_ROOT + ".scan";
    public static final String EXTERNAL_DYNAMIC_EVENT_NAME_TERMINATE = MASTERBUILDER_EXTERNAL_DYNAMIC_EVENT_ROOT + ".terminate";
    public static final String EXTERNAL_DYNAMIC_EVENT_NAME_UPDATE = MASTERBUILDER_EXTERNAL_DYNAMIC_EVENT_ROOT + ".load";
    public static final String EXTERNAL_DYNAMIC_EVENT_NAME_POLL_CONFIGURATION = MASTERBUILDER_EXTERNAL_DYNAMIC_EVENT_ROOT + ".polling-configuration";
    public static final String ENABLE_CACHE_PROPERTY = "cache.primary.endpoint";
    public static final String MISSING_CACHE_PROPERTY = "Missing %s property required for multi-step functionality";
    public static final String RESOURCE_ID_NOT_FOUND = "Process resource id %s missing or expired";
    public static final String NO_AVAILABLE_NEXT_STEPS = "Process resource id %s has no available next steps";
    public static final String MISSING_STEP_PARAMETER = "Missing step query parameter";
    public static final String STEP_NOT_AVAILABLE = "Step %s is not available in %s";
    public static final String DEFAULT_CACHE_EXPIRATION = "PT15M";
    public static final String CACHE_READ_FAILED_ID ="904990";
    public static final String CACHE_WRITE_FAILED_ID ="904991";
    public static final String CACHE_READ_MISS_ID="904992";
    public static final String CACHE_OPERATION_FAILED_DEV_TEXT_ID = "CACHE_OPERATION_FAILED_DEV_TEXT";
    public static final String NEGATIVE_CACHE_EXPIRATION = "Cache expiration is in the past: %s";
    public static final String DYNAMIC_TEXT_MSG = "DYNAMIC_TEXT";
    public static final String ATTRIBUTE_VALUE = "attributeValue";
    public static final String ATTRIBUTE_NAME = "attributeName";
    public static final String CLIENT_PROPS_MASK_PREFIX = "audit.filter.props.";
    // Single Atomic Rule result from the DMN engine, used for auditing/listening mode
    public static final String POLICY_RULE_RESULT = "policyRuleResult";
    public static final String DYNAMIC_UPDATES_FEATURE_FLAG = "masterbuilder.feature.external-items.dynamic-load";
    // Enable CuRE event logging through direct:publishCuRE
    public static final String ONESTREAM_HOST = "onestream.host";
    public static final String ONESTREAM_PORT = "onestream.port";
    public static final String POLICY_NAME = "policyName";
    public static final String POLICY_VERSION = "policyVersion";
    public static final String POLICY_VERSION_PATCH = "policyVersionPatch";
    public static final String CACHE_KEY = "cacheKey";
    public static final String STEP_NAME = "stepName";
    public static final String MOCK_MODE_CONFIG = "mockModeConfig";

    private ApplicationConstants() {
        // Private constructor
    }
}
