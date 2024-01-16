package com.capitalone.identity.identitybuilder.policycore.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Partner token response returned by POST method.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PartnerTokenResponse implements Serializable {
    private static final long serialVersionUID = -1158204410809749457L;

    /**
     * Type of token, typically just the string “bearer”.
     */
    @JsonProperty("token_type")
    private String tokenType;

    /**
     * The access token string as issued by the authorization server.
     */
    @JsonProperty("access_token")
    private String accessToken;

    /**
     * The duration of time the access token is granted for
     */
    @JsonProperty("expires_in")
    private int expiresIn;

    /**
     * Refresh token which applications can use to obtain another access token when the current access token expires
     */
    @JsonProperty("refresh_token")
    private String refreshToken;

    /**
     * The duration of time the refresh token is granted for
     */
    @JsonProperty("refresh_expires_in")
    private int refreshExpiresIn;

    /**
     * The duration before which the access token must not be accepted for processing
     */
    @JsonProperty("not-before-policy")
    private int notBeforePolicy;

    /**
     * The identity information about the user that is encoded into a token
     */
    @JsonProperty("id_token")
    private String idToken;

    /**
     * An internal identifier used to identify the session
     */
    @JsonProperty("session_state")
    private String sessionState;
}
