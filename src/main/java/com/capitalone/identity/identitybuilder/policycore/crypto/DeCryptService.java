package com.capitalone.identity.identitybuilder.policycore.crypto;

import com.capitalone.chassis.engine.model.exception.ChassisBusinessException;
import com.capitalone.fs.secretprovider.SecretProvider;
import com.capitalone.fs.secretprovider.builder.IAMSecretProviderBuilder;
import com.capitalone.fs.secretprovider.builder.PassThruSecretProviderBuilder;
import com.capitalone.fs.secretprovider.config.SecretProviderConfig;
import com.google.gson.JsonSyntaxException;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWEHeader;
import com.nimbusds.jose.JWEObject;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static com.capitalone.identity.identitybuilder.policycore.crypto.DecryptUtil.createError;
import static java.text.MessageFormat.format;

/**
 * 1. Format of encryptedData is JWE String and this encoded parts are separated by '.', can be transformed using {@link com.nimbusds.jose.JWEObject#parse(String)}
 *
 * <p>
 * <b> headers  : (required)</b></b>
 *   <ul>
 *       <li> refer how to get kid value {@link #parseStringToJwt(String)}
 *       <li> contains public key Id for encrypted payload in <decodedJEWObject>.header.kid
 *       <li>  keyId is public key
 *       <li>  ignores other keys ECHD decryption used by default
 *   </ul>
 * </p>
 *
 * <p>
 * <b> base64 - encrypted key  : (optional)</b> </b>
 * <ul>
 *     <li>  ignored (not processed by decrypt component)
 * </ul>
 * </p>
 * <p>
 * <b> base64 IV  : (required)</b> </b>
 * </p>
 * <p>
 * <b> base64 encoded cypherText : (required) </b>
 * <ul>
 *     <li> actual file / image  to decrypt
 * </ul>
 * </p>
 * <p>
 * <b> base64 authTag : (required)</b>
 * </p>
 * <p>
 * <br/> 2. Private key is determined from combination of kid value, parsed from from JWE object headers, and COS request properties
 * <br/> 3. Decryption is performed using ECHD
 * <br/>
 * </p>
 */
@Service
@Log4j2
public class DeCryptService {

    protected final Logger logger = LogManager.getLogger(this.getClass());
    private static final int INVALID_PUBLIC_KEY_ID_ERROR_CODE = 211007;
    private final Environment environment;
    private final boolean isLocalEnvTesting;

    private final String keyRotationSecretVersion;


    private final Map<String, String> cache;

    /**
     * keyRotationSecretVersion: property used to set the API version for {@link SecretProvider}, see: {@link #getIAMSecretProvider(String, String, String)}
     */
    public DeCryptService(Environment environment,
                          @Value("${identitybuilder.policycore.crypto-cos.keyRotationSecretVersion:v1}") String keyRotationSecretVersion,
                          @Qualifier("decryptionPrivateKeyMap") Map<String, String> cache) {
        this.environment = environment;
        this.keyRotationSecretVersion = keyRotationSecretVersion;
        this.cache = cache;
        isLocalEnvTesting = Arrays.stream(environment.getActiveProfiles())
                .map(String::toLowerCase)
                .anyMatch(o -> o.contains("local") || o.contains("test"));
    }

    @SneakyThrows
    private static JWEObject parseStringToJwt(@NonNull String encryptedData) {
        return JWEObject.parse(encryptedData);
    }

    // kid value present in stringToDecrypt
    public String decrypt(@NonNull String encryptedData, @NonNull COSRequestProperties config) {


        JWEObject parsedEncryptedData = parseStringToJwt(encryptedData);

        // get extract Kid from encrypted payload
        final String kid = Optional.of(parsedEncryptedData)
                .map(JWEObject::getHeader)
                .map(JWEHeader::getKeyID)
                .orElseThrow(createError("Unable to get Kid from encrypted payload"));

        String privateKey = null;
        // logic to fetch local cache data using kid. privateKeyMap
        if (cache.containsKey(kid)) {
            privateKey = cache.get(kid);
            if (StringUtils.isBlank(privateKey)) {
                log.error("Unable to find Private Key in Kid: {}", kid);
            }
            if (DecryptUtil.isExpired(privateKey)) {
                log.error("Private Key in cache expired for Kid: {}", kid);
                privateKey = null;
            }
        }

        if (StringUtils.isBlank(privateKey)) {
            // get private key
            privateKey = Optional.ofNullable(getDecryptionSecretProvider(kid, config))
                    .map(provider -> provider.getSecret(kid))
                    .map(DecryptUtil::getDecode)
                    .map(String::new)
                    .orElseThrow(createError("unable to get private key from secret provider"));
            // check if private key expired
            if (DecryptUtil.isExpired(privateKey)) {
                log.error("Private Key expired for Kid: {}, productId: {} ", kid, config.getProductId());
                throw new ChassisBusinessException("Private Key Expired");
            }
            // add to cache
            cache.putIfAbsent(kid, privateKey);
        }

        try {
            return DecryptUtil.parseData(privateKey, parsedEncryptedData);
        } catch (JOSEException | JsonSyntaxException | ParseException error) {
            String message = format(
                    "Error decrypting JWE from client: {0}, key: {1}, message: {2}",
                    config.getProductId(),
                    kid,
                    error.getMessage());
            throw new ChassisBusinessException(message, error, INVALID_PUBLIC_KEY_ID_ERROR_CODE);
        }
    }

    private SecretProvider getDecryptionSecretProvider(String kid, COSRequestProperties config) {
        String applicationName = format("{0}/{1}", config.getLockBoxId(), config.getProductId());
        SecretProvider secretprovider;
        if (isLocalEnvTesting) {
            secretprovider = getLocalSecretProvider(applicationName, kid);
        } else {
            secretprovider = getIAMSecretProvider(applicationName, config.getVaultAddress(), config.getVaultRole());
        }
        return secretprovider;
    }

    private SecretProvider getLocalSecretProvider(String applicationName, String kid) {
        String key = format("{0}/{1}", applicationName, kid);
        String localKeyVal = environment.getProperty(key);
        Map<String, Object> localKeys = new HashMap<>();
        localKeys.put(key, localKeyVal);
        return new PassThruSecretProviderBuilder().withApplicationName(applicationName).withSecrets(localKeys).build();
    }

    private SecretProvider getIAMSecretProvider(String applicationName, String secretUrl, String vaultRole) {
        SecretProviderConfig config = new SecretProviderConfig() {
            @Override
            public String getSecretBackend() {
                return keyRotationSecretVersion;
            }
        };

        return new IAMSecretProviderBuilder(config)
                .withApplicationName(applicationName)
                .withApplicationRoleName(vaultRole)
                .withUrl(secretUrl)
                .build();
    }


}
