package com.capitalone.identity.identitybuilder.policycore.camel;

/**
 * Constants used by Camel routes.
 *
 * @author oqu271
 */
public final class PolicyConstants {


    // Reserved Camel exchange policy headers.
    public static final String HEADER_AUDIT_CONTEXT = "auditContext";
    public static final String HEADER_BUSINESS_EVENT = "businessEvent";
    public static final String HEADER_CLIENTID = "clientId";
    public static final String HEADER_DXHEADERS = "dxHeaders";
    public static final String HEADER_DXRESPONSE_HEADERS = "dxResponseHeaders";
    public static final String HEADER_HTTPSTATUS = "httpStatus";
    public static final String HEADER_AVAILABLENEXTSTEPS = "availableNextSteps";
    public static final String HEADER_EFFECTIVENEXTSTEP = "effectiveNextStep";
    public static final String HEADER_POLICYID = "policyId";
    public static final String HEADER_POLICYNAME = "policyName";
    public static final String HEADER_POLICYVERSION = "policyVersion";
    public static final String HEADER_POLICYVERSION_PATCH = "policyVersionPatch";
    public static final String HEADER_POLICYROUTENAME = "policyRouteName";
    public static final String HEADER_POLICYSTATE = "policyState";
    public static final String HEADER_STEPNAME = "stepName";
    public static final String HEADER_POLICYAUTHOR_SUPPRESS_DEVTEXT = "suppressPolicyErrorDeveloperText";
    // Reserved headers used by Dev Exchange utility methods.
    public static final String HEADER_CUSTOMHEADERS = "customHeaders";
    public static final String HEADER_DXRESULTNAME = "dxResultName";
    public static final String HEADER_MOCKMODE = "policy-core.mockMode";
    // Reserved policy state map keys
    public static final String STATE_APPLIEDRULES = "appliedRules";
    public static final String STATE_DXRESULTS = "dxResults";
    //Aggregated rule results from the DMN Engine
    public static final String POLICY_RULE_RESULTS = "policyRuleResults";
    // Aggregated operational audit information
    public static final String OPERATIONAL_AUDIT_EXECUTION_DATA = "operationalAuditExecutionData";
    //JWT token passed by WebGateway to identify customer
    public static final String CUSTOMER_ID_TOKEN = "customerIDToken";
    public static final String JWT_TOKEN_VALIDATION_RESULT = "jwtTokenValidationResult";
    public static final String JWT_EXTRACTED_FIELDS = "jwtExtractedFields";
    public static final String JWT_SIGNED_TOKEN = "jwtSignedToken";

    private PolicyConstants() {
        // Private constructor
    }
}

/*
 * Copyright 2018 Capital One Financial Corporation All Rights Reserved.
 *
 * This software contains valuable trade secrets and proprietary information of
 * Capital One and is protected by law. It may not be copied or distributed in
 * any form or medium, disclosed to third parties, reverse engineered or used in
 * any manner without prior written authorization from Capital One.
 */