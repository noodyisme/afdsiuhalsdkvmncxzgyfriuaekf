package com.capitalone.identity.identitybuilder.policycore.service.jws;

import com.capitalone.identity.identitybuilder.policycore.operational_audit.OperationalAudit;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.mapper.JwtPublicKeyRequestedEventMapper;
import com.capitalone.identity.identitybuilder.policycore.operational_audit.model.type.OperationalEventType;
import com.newrelic.api.agent.Trace;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.lang.invoke.MethodHandles;
import java.time.Duration;

import static com.capitalone.identity.identitybuilder.policycore.service.jws.JwsService.*;

@Service
public class JwtPublicKeyRetrievalService {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final String SUCCESSFUL_CERTIFICATE_RETRIEVAL_RESPONSE_STRING = "Successfully retrieved certificated from KMS";
    private static final String KMS_URL = "/developer-platform/key-management/certificates/keys/{kid}";
    private static final int JWK_RETRY_ATTEMPTS = 1;


    private final WebClient webClient;
    public JwtPublicKeyRetrievalService(WebClient webClient) {
        this.webClient = webClient;
    }

    @OperationalAudit(eventType = OperationalEventType.JWT_PUBLIC_KEY_REQUESTED, mapperFactory = JwtPublicKeyRequestedEventMapper.Factory.class)
    @Trace
    public JwtPublicKeyRetrievalResponse fetchJwtPublicKey(String kid){
        String jwk;
        try{
            jwk = obtainJWKFromExchange(kid);
            return new JwtPublicKeyRetrievalResponse(200, jwk, SUCCESSFUL_CERTIFICATE_RETRIEVAL_RESPONSE_STRING);

        } catch (JwkExchangeException e){
            String errorResponseMessage = e.getMessage();
            return new JwtPublicKeyRetrievalResponse(e.getHttpStatusCode(),null, errorResponseMessage);
        }
    }

    String obtainJWKFromExchange(String kid) {
        return this.webClient.get()
                .uri(KMS_URL, kid)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .onStatus(HttpStatus::is4xxClientError,
                        resp -> Mono.error(() -> new JwkExchangeException(String.format(classify4xxClientErrorMessage(resp), kid, resp.statusCode().getReasonPhrase()) + ":" + resp.rawStatusCode(), resp.rawStatusCode())))
                .onStatus(HttpStatus::is5xxServerError,
                        resp -> Mono.error(() -> new JwkExchangeException(String.format(SERVER_ERROR, kid, resp) + 500, 500)))
                .bodyToMono(String.class)
                .retryWhen(Retry.backoff(JWK_RETRY_ATTEMPTS, Duration.ofSeconds(1)).filter(this::isServer5XXError)
                        .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) -> {
                            throw new JwkExchangeException("Failed to communicate with DevEx after " + JWK_RETRY_ATTEMPTS + " attempts while attempting to retrieve key :" + kid + ":" + ",HttpStatus=5XX", 500);
                        }))
                .onErrorMap(throwable -> !(throwable instanceof JwkExchangeException), throwable -> {
                    logger.error("Error obtaining JWK from exchange:", throwable);
                    return new JwkExchangeException(String.format(NOT_ABLE_TO_RETRIEVE_KEY_FOR_KID, kid) + ",HttpStatus=0", 0);
                })
                .doOnError(th -> logger.error("Error occurred during retrieval of public key from DXGW Key Management Service", th))
                .block();
    }

    @NotNull
    private String classify4xxClientErrorMessage(ClientResponse resp) {
        String exceptionMessage;
        if (resp.rawStatusCode() == 404) {
            exceptionMessage = KID_IS_NOT_AVAILABLE_SERVER_RESPONSE;
        } else if (resp.rawStatusCode() == 400) {
            exceptionMessage = REQUEST_NOT_UNDERSTOOD_SERVER_RESPONSE;
        } else if (resp.rawStatusCode() == 401 || resp.rawStatusCode() == 403) {
            exceptionMessage = AUTHENTICATION_OR_AUTHORIZATION_SERVER_RESPONSE;
        } else {
            exceptionMessage = OTHER_CLIENT_ERROR_SERVER_RESPONSE;
        }
        return exceptionMessage;
    }

    private boolean isServer5XXError(Throwable throwable) {
        return throwable instanceof WebClientResponseException &&
                ((WebClientResponseException) (throwable)).getStatusCode().is5xxServerError();
    }


}