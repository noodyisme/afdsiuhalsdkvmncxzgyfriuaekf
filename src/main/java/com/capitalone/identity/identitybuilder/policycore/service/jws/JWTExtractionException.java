package com.capitalone.identity.identitybuilder.policycore.service.jws;

import com.capitalone.identity.identitybuilder.policycore.service.exception.CustomPolicyException;

public class JWTExtractionException extends CustomPolicyException {

    public static final String INVALID_TOKEN_ID = "501001";
    public static final String MALFORMED_TOKEN_ID = "501002";
    public static final String INVALID_KEY_ID = "501003";
    public static final String MISSING_CERTIFICATE_ID = "501004";
    public static final String PUBLIC_KEY_EXPIRED_ID = "501005";
    public static final String INVALID_CLAIMS_ID = "501401";
    public static final String INVALID_SIGNATURE_ID = "501402";

    public static final String JWT_UNAUTHORIZED_ERROR_TEXT = "JWT Unauthorized Error";
    public static final String JWT_FORBIDDEN_ERROR_ERROR_TEXT = "JWT Forbidden";

    public static final String MISSING_OR_ABSENT_TOKEN_UNABLE_TO_VALIDATE_DEV_TEXT = "Missing or absent token, unable to validate";
    public static final String JWT_MALFORMED_NOT_IN_EXPECTED_FORMAT_DEV_TEXT = "JWT malformed, not in expected format";
    public static final String JWT_KEY_ID_MISSING_OR_INVALID_DEV_TEXT = "JWT key id missing or invalid";
    public static final String JWT_CLAIMS_WERE_NOT_APPROVED_DEV_TEXT = "JWT claims were not approved";
    public static final String JWT_PUBLIC_CERTIFICATE_ASSOCIATED_WITH_KEY_ID_MISSING_OR_INVALID_DEV_TEXT = "JWT public certificate associated with key id missing or invalid";
    public static final String JWT_PUBLIC_CERTIFICATE_ASSOCIATED_WITH_KEY_ID_EXPIRED_TEXT = "JWT public certificate associated with key id expired";
    public static final String JWT_SIGNATURE_NOT_VALID_DEV_TEXT = "JWT Signature not valid";
    public static final String JWT_PRODUCTID_MISMATCH_TEXT = "JWT expected productId does not match with productId from public key";


    public JWTExtractionException(JwsExceptionType jwsExceptionType) {
        super(jwsExceptionType.toString());
    }

    public enum JwsExceptionType{

        JWT_NULL_TOKEN(INVALID_TOKEN_ID, JWT_UNAUTHORIZED_ERROR_TEXT, MISSING_OR_ABSENT_TOKEN_UNABLE_TO_VALIDATE_DEV_TEXT),
        JWT_MALFORMED_TOKEN(MALFORMED_TOKEN_ID, JWT_UNAUTHORIZED_ERROR_TEXT, JWT_MALFORMED_NOT_IN_EXPECTED_FORMAT_DEV_TEXT),
        JWT_INVALID_KEY(INVALID_KEY_ID, JWT_UNAUTHORIZED_ERROR_TEXT, JWT_KEY_ID_MISSING_OR_INVALID_DEV_TEXT),
        JWT_MISSING_CERTIFICATE(MISSING_CERTIFICATE_ID, JWT_UNAUTHORIZED_ERROR_TEXT, JWT_PUBLIC_CERTIFICATE_ASSOCIATED_WITH_KEY_ID_MISSING_OR_INVALID_DEV_TEXT),
        JWS_INVALID_CLAIMS(INVALID_CLAIMS_ID, JWT_FORBIDDEN_ERROR_ERROR_TEXT, JWT_CLAIMS_WERE_NOT_APPROVED_DEV_TEXT),
        JWT_INVALID_SIGNATURE(INVALID_SIGNATURE_ID, JWT_FORBIDDEN_ERROR_ERROR_TEXT, JWT_SIGNATURE_NOT_VALID_DEV_TEXT),
        JWT_PRODUCTID_MISMATCH(MALFORMED_TOKEN_ID, JWT_FORBIDDEN_ERROR_ERROR_TEXT, JWT_PRODUCTID_MISMATCH_TEXT),
        JWT_PUBLIC_KEY_EXPIRED(PUBLIC_KEY_EXPIRED_ID, JWT_UNAUTHORIZED_ERROR_TEXT, JWT_PUBLIC_CERTIFICATE_ASSOCIATED_WITH_KEY_ID_EXPIRED_TEXT);

        private final String id;
        private final String text;
        private final String developerText;
        JwsExceptionType(String id, String text, String developerText) {
            this.id = id;
            this.text = text;
            this.developerText = developerText;
        }

        @Override
        public String toString() {
            return String.join("~", id, text, developerText);
        }
    }
}