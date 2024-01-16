package com.capitalone.identity.identitybuilder.policycore.service.util;

import com.capitalone.api.security.CryptoSerializerDeserializer;
import com.capitalone.dsd.utilities.crypto.lib.pki.PKIEncryption;
import com.capitalone.dsd.utilities.crypto.lib.pkiaes.PKIAESEncryption;
import com.nimbusds.jose.EncryptionMethod;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWEAlgorithm;
import com.nimbusds.jose.JWEHeader;
import com.nimbusds.jose.JWEObject;
import com.nimbusds.jose.Payload;
import com.nimbusds.jose.crypto.ECDHDecrypter;
import com.nimbusds.jose.crypto.ECDHEncrypter;
import com.nimbusds.jose.crypto.RSADecrypter;
import com.nimbusds.jose.crypto.RSAEncrypter;
import com.nimbusds.jose.crypto.bc.BouncyCastleProviderSingleton;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.util.Base64URL;
import com.nimbusds.jwt.EncryptedJWT;

import kotlin.Pair;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.security.KeyPair;
import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;
import java.text.ParseException;
import java.util.Base64;

/**
 * @author nro567
 * This is the util class utilized for cryptographic operations
 */
@Named
@ConditionalOnProperty(prefix = "identity.identitybuilder.policycore", name = "crypto.util.load", havingValue = "true")
public class CryptoUtil {
    /**
     * Logger.
     */
    private final Logger logger = LogManager.getLogger(getClass());

    /**
     * DSD encryption utilities object.
     */
    @Inject
    private PKIEncryption pkiEncryption;
    
    /**
     * DSD encryption utilities object.
     */
    @Inject
    private PKIAESEncryption pkiAesEncryption;
    
    /**
     * chassis crypto object.
     */
    @Inject
    @Named("defaultCryptoSerializerDeserializer")
    private CryptoSerializerDeserializer crypto;
	
    /**
     * This method is used to encrypt the incoming data string.
     *
     * @param input - plain text string
     * @return encrypted string
     * 
     */
    public String encrypt(String input) {
        String encResponse = null;
        if (StringUtils.isNotBlank(input)) {
            try {
                encResponse = pkiEncryption.encrypt(input);
            } catch (Exception excp) {
                logger.error("Error in encrypt() : Exception {excp}", excp);
            }
        }
        return encResponse;
    }

    /**
     * This method is used to decrypt the incoming data string.
     *
     * @param input - encrypted string
     * @return plain text
     */
    public String decrypt(String input) {
        String decResponse = null;
        if (StringUtils.isNotBlank(input)) {
            try {
                decResponse = pkiEncryption.decrypt(input);
            } catch (Exception excp) {
                logger.error("Error in decrypt() : Exception {excp}", excp);
            }
        }
        return decResponse;
    }

    /**
     * This method is used to encrypt the incoming data string using PKI AES Encryption. 
     *
     * @param input - plain text string
     * @return encrypted string
     * 
     */
    public String encryptPkiAes(String input) {
        String encResponse = null;
        if (StringUtils.isNotBlank(input)) {
            try {
                encResponse = pkiAesEncryption.encrypt(input);
            } catch (Exception excp) {
                logger.error("Error in encryptPkiAes() : Exception {excp}", excp);
            }
        }
        return encResponse;
    }
    
    /**
     * This method is used to decrypt PKI AES string. 
     *
     * @param input - PKI AES-Encrypted string
     * @return decrypted string
     * 
     */
    public String decryptPkiAes(String input) {
        String decryptedResponse = null;
        if (StringUtils.isNotBlank(input)) {
            try {
            	decryptedResponse = pkiAesEncryption.decrypt(input);
            } catch (Exception excp) {
                logger.error("Error in decryptPkiAes() : Exception {excp}", excp);
            }
        }
        return decryptedResponse;
    }
    
    /**
     * This method is used to decrypt the reference id info using chassis crypto utilities.
     * @param encRefIdTxt
     * @return String - decrypted data.
     */
    public String decryptRefId(String encRefIdTxt){
        String unencrypted = StringUtils.EMPTY;
        /**
         * To handle the .NET 3.5 framework issue which replace %2F with the "/" which causes the URL to the
         * resource to break. In order to overcome the issue, .NET client would be double encoding the url and
         * sending it. If double encoded url comes as input, first time decrpyt method would fail. We would decode
         * the value and try decrypting again.
         */
        try {
            /**
             * First call to decrypt. It should fail if the "value" is still encoded as a result of double encoding
             * done by client.
             */
            unencrypted = crypto.decrypt(encRefIdTxt);

        } catch (Exception excp) {
            /**
             * Loop through the exceptions to see if there is IllegalBlockSizeException or BadPaddingException.
             */
            if (ExceptionUtils.indexOfType(excp, IllegalBlockSizeException.class) >= 0
                    || ExceptionUtils.indexOfType(excp, BadPaddingException.class) >= 0) {
                /**
                 * In the catch, try to decode double encoded url, by calling URLDecoder. and then call the decrypt
                 * method again.
                 */
                try{
                    unencrypted = crypto.decrypt(URLDecoder.decode(encRefIdTxt, "UTF-8"));
                }catch(UnsupportedEncodingException usee){
                    logger.error("Error in decrypt() : Exception {usee}", usee);
                }
            } else {
                logger.error("Error in decrypt() : Exception {excp}", excp);
            }
        }
        logger.info("decrypted ref id data : {}", unencrypted);
        return unencrypted;
    }
    public String encryptRefId(String decRefIdTxt){
        String encrypted = StringUtils.EMPTY;
        try {

            encrypted = crypto.encrypt(decRefIdTxt);

        } catch (Exception excp) {

            logger.error("Error in encryption() : Exception {excp}", excp);
        }
        logger.info("encrypted ref id data : {}", encrypted);
        return encrypted;
    }
    
    /**
     * Invokes the encryptEcdh method when a string payload is requested
     * 
     * @param jwtkey	key containing x, y, kid values
     * @param payload	The payload that will be encrypted. Must be String or byte[]
     * @return	Returns an encrypted ciphertext for string payloads and an encrypted Base64 image for image Payloads
     * @throws JOSEException	Error has occurred in the encryption process
     */
    static public String encryptEcdh(JWTKey jwtkey, Object payload) throws JOSEException, IllegalArgumentException {
    	if (payload instanceof String) {
    	    return encryptEcdh(jwtkey, new Payload((String) payload));
    	} else if (payload instanceof byte[]) {
    		String output = encryptEcdh(jwtkey, new Payload((byte[]) payload));
    	    String base64Image = Base64.getEncoder().encodeToString(output.getBytes());
    	    return base64Image;
    	} else {
    	    throw new IllegalArgumentException("Payload must be String or byte[]");
    	}
    }

    /**
	 * This method is responsible for encrypting a String payload
	 * 
	 * @param jwtKey    key containing x, y, kid values
	 * @param payload   A field containing a Payload object which contains either a string or byte[]
	 * @return A ciphertext of the payload
     * @throws JOSEException	Error has occurred in the encryption process
	 */
	static private String encryptEcdh(JWTKey jwtKey, Payload payload) throws JOSEException {
    	
		Base64URL xUrl = new Base64URL(jwtKey.getX());
		Base64URL yUrl = new Base64URL(jwtKey.getY());
		String alg = (jwtKey.getAlg() == null || jwtKey.getAlg().isEmpty()) ? "ECDH-ES" : jwtKey.getAlg();

		ECKey eckey = new ECKey.Builder(Curve.P_521, xUrl, yUrl).algorithm(JWEAlgorithm.parse(alg)).keyID(jwtKey.getKid())
				.build();
		JWEHeader header = new JWEHeader.Builder(JWEAlgorithm.ECDH_ES, EncryptionMethod.A256GCM).keyID(jwtKey.getKid())
				.build();

		JWEObject jweObject = new JWEObject(header, payload);
		
		String ciphertext = null;
		
		ECDHEncrypter encrypter = new ECDHEncrypter(eckey.toECPublicKey());
		encrypter.getJCAContext().setContentEncryptionProvider(BouncyCastleProviderSingleton.getInstance());

		jweObject.encrypt(encrypter);
		ciphertext = jweObject.serialize();
		

		return ciphertext;
	}

    /**
     * Decrypts the encrypted payload to return the original string and byte[] payload
     * 
     * @param vcnRequestJWE The encrypted payload represented as a ciphertext
     * @param ecPrivateKeyJWK ECKey used for decryption purposes
     * @return Returns the decrypted payload
     * @throws ParseException if it's unable to read the ciphertext passed in
     * @throws JOSEException if there is an issue with the decrypter
     */
    public Pair<byte[], String> decryptEcdh(String vcnRequestJWE, ECKey ecPrivateKeyJWK) throws ParseException, JOSEException {
        // Parse JWE & validate headers
        JWEObject jweObjectCapitalOne = EncryptedJWT.parse(vcnRequestJWE);
        ECDHDecrypter decrypter = new ECDHDecrypter(ecPrivateKeyJWK.toECPrivateKey());
        decrypter.getJCAContext().setContentEncryptionProvider(BouncyCastleProviderSingleton.getInstance());
        jweObjectCapitalOne.decrypt(decrypter);
        byte[] decryptedByteArray = jweObjectCapitalOne.getPayload().toBytes();
        String decryptedString = jweObjectCapitalOne.getPayload().toString();
        return new Pair<byte[], String>(decryptedByteArray, decryptedString);
    }

    public static String encryptRsa(KeyPair keyPair, Object payload) throws JOSEException {
        PublicKey publicKey = keyPair.getPublic();
        if (payload instanceof String) {
            return encryptRsa(publicKey, new Payload((String) payload));
        } else if (payload instanceof byte[]) {
            return encryptRsa(publicKey, new Payload((byte[]) payload));
        } else {
            throw new IllegalArgumentException("Payload must be String or byte[]");
        }
    }

    public static String encryptRsa(PublicKey publicKey, Payload payload) throws JOSEException {
        JWEHeader jweHeader = new JWEHeader.Builder(JWEAlgorithm.RSA_OAEP_256, EncryptionMethod.A128GCM)
                .build();
        JWEObject jweObject = new JWEObject(jweHeader, payload);

        RSAEncrypter encrypter = new RSAEncrypter((RSAPublicKey) publicKey);

        jweObject.encrypt(encrypter);

        return jweObject.serialize();
    }

    public Pair<byte[], String> decryptRsa(String encryptedMessage, KeyPair keyPair) throws ParseException, JOSEException {
        JWEObject jweObject = EncryptedJWT.parse(encryptedMessage);
        RSADecrypter rsaDecrypter = new RSADecrypter(keyPair.getPrivate());

        jweObject.decrypt(rsaDecrypter);
        byte[] decryptedMessageBytes = jweObject.getPayload().toBytes();
        String decryptedMessage = jweObject.getPayload().toString();
        return new Pair<>(decryptedMessageBytes, decryptedMessage);
    }
}
