package com.capitalone.identity.identitybuilder.policycore.crypto;

import com.capitalone.chassis.engine.model.exception.ChassisBusinessException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWEObject;
import com.nimbusds.jose.Payload;
import com.nimbusds.jose.crypto.ECDHDecrypter;
import com.nimbusds.jose.crypto.bc.BouncyCastleProviderSingleton;
import com.nimbusds.jose.jwk.ECKey;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;

import java.text.ParseException;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;
import java.util.function.Supplier;

@Log4j2
public class DecryptUtil {

    private DecryptUtil() throws IllegalAccessException {
        throw new IllegalAccessException("Util method should not be instantiated");
    }

    public static byte[] getDecode(@NonNull String key) {
        return Base64.getDecoder().decode(key);
    }

    @SneakyThrows
    public static boolean isExpired(@NonNull String decodedKey) {
        try {
            JsonNode jsonParsedKey = new ObjectMapper().readTree(decodedKey);
            Instant expiredAt = Instant.ofEpochSecond(jsonParsedKey.path("exp").asLong());
            return Instant.now().isAfter(expiredAt);
        } catch (JsonProcessingException error) {
            log.error("unable to read decoded key: {}", error.getMessage(), error);
            throw new ChassisBusinessException(error);
        }
    }

    public static String parseData(
            String privateKey, @NonNull JWEObject encryptedData) throws JOSEException, ParseException {
        ECDHDecrypter ecdhDecrypter = new ECDHDecrypter(ECKey.parse(privateKey));
        ecdhDecrypter.getJCAContext().setContentEncryptionProvider(BouncyCastleProviderSingleton.getInstance());
        encryptedData.decrypt(ecdhDecrypter);
        return Optional.ofNullable(encryptedData.getPayload())
                .map(DecryptUtil::convertToString)
                .orElse(null);
    }

    private static String convertToString(Payload payload) {
        return payload.toBytes() != null ? Base64.getEncoder().encodeToString(payload.toBytes()) : payload.toString();
    }

    public static Supplier<ChassisBusinessException> createError(@NonNull String message) {
        return () -> new ChassisBusinessException(message);
    }
}
