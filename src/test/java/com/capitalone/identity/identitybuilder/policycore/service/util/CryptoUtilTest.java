package com.capitalone.identity.identitybuilder.policycore.service.util;

import com.capitalone.api.security.CryptoSerializerDeserializer;
import com.capitalone.chassis.engine.model.exception.ChassisBusinessException;
import com.capitalone.dsd.utilities.crypto.lib.pki.PKIEncryption;
import com.capitalone.dsd.utilities.crypto.lib.pkiaes.PKIAESEncryption;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWEObject;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.util.Base64URL;

import kotlin.Pair;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.IllegalBlockSizeException;
import javax.inject.Named;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class CryptoUtilTest {

	private static final String STR_INPUT = "xyz";
	private static final String STR_OUTPUT = "abc";

	private static final String PLAINTEXT = "payload to encrypt";
	private byte[] testArray = new byte[50000];
	private byte[] testArray2 = new byte[10000];
	@InjectMocks @Spy private JWTKey jwtKey;
	
	@InjectMocks
	CryptoUtil cryptoUtil;

	/**
	 * DSD encryption utilities object.
	 */
	@Mock
	private PKIEncryption pkiEncryption;
	
	@Mock
	private PKIAESEncryption pikAesEncryption;
	
	@Mock
	private JWEObject jweObject;

	private String x = "AcA5yijXdEHu04aYoYkTJNgPpey1sctD5fxBNTdosfKKHOi4Jw14vYK3O8bwG2bBV3VL3d98y88Y7BcEy4UOkqG1";
	private String y = "AKDTM7BHYkugTIzJnRkIw1kQoCXU7-XBSKGwAHoWxad9XSz7GtUQZnlxuAusuz_n_llPHnZWLKLhLh5uoyvcWEVj";
	private String d = "AOQxfGRdjz66zngO7OAKkHTpGUOPvra6_QRGBXdiRogco7fX4eTJpgPjxnNeguHlUtdWIqkbDFiPCJDOqfk7ozMO";
	private ECKey capitalOneJWK =  new ECKey.Builder(Curve.P_521, new Base64URL(x), new Base64URL(y))
            .d(new Base64URL(d))
            .build();
	
	/**
	 * chassis crypto object.
	 */
	@Mock
	@Named("defaultCryptoSerializerDeserializer")
	private CryptoSerializerDeserializer crypto;

	@Test
	public void encryptTest() {
		Mockito.doReturn(STR_OUTPUT).when(pkiEncryption).encrypt(Mockito.anyString());
		String encrypt = cryptoUtil.encrypt(STR_INPUT);
		assertEquals(STR_OUTPUT, encrypt);
	}

	@Test
	public void encryptTest_Error() {
		Mockito.doThrow(RuntimeException.class).when(pkiEncryption).encrypt(Mockito.anyString());
		String encrypt = cryptoUtil.encrypt(STR_INPUT);
		assertNull(encrypt);
	}

	@Test
	public void decryptTest() {
		Mockito.doReturn(STR_OUTPUT).when(pkiEncryption).decrypt(Mockito.anyString());
		String decrypt = cryptoUtil.decrypt(STR_INPUT);
		assertEquals(STR_OUTPUT, decrypt);
	}

	@Test
	public void decryptTest_Error() {
		Mockito.doThrow(RuntimeException.class).when(pkiEncryption).decrypt(Mockito.anyString());
		String decrypt = cryptoUtil.decrypt(STR_INPUT);
		assertNull(decrypt);
	}

	@Test
	public void decryptRefIdTest() {
		Mockito.doReturn(STR_OUTPUT).when(crypto).decrypt(Mockito.anyString());
		String decryptRefId = cryptoUtil.decryptRefId(STR_INPUT);
		assertEquals(STR_OUTPUT, decryptRefId);
	}

	@Test
	public void decryptRefIdTestURLEncoded() throws UnsupportedEncodingException {
		String input = "foo+";
		String inputEncoded = URLEncoder.encode(input, "UTF-8");
		Exception cryptoException = new ChassisBusinessException(new IllegalBlockSizeException());
		Mockito.doThrow(cryptoException).when(crypto).decrypt(inputEncoded);
		Mockito.doReturn(input).when(crypto).decrypt(input);
		String decryptRefId = cryptoUtil.decryptRefId(inputEncoded);
		assertEquals(input, decryptRefId);
	}

	@Test
	public void decryptRefIdTest_Error() {
		Mockito.doThrow(RuntimeException.class).when(crypto).decrypt(Mockito.anyString());
		String decryptRefId=cryptoUtil.decryptRefId(STR_INPUT);
		assertNotNull(decryptRefId);
	}

	@Test
	public void encryptRefIdTest() {
		Mockito.doReturn(STR_OUTPUT).when(crypto).encrypt(Mockito.anyString());
		String encryptRefId = cryptoUtil.encryptRefId(STR_INPUT);
		assertEquals(STR_OUTPUT, encryptRefId);
	}

	@Test
	public void encryptRefIdTestNullReturn() {
		String encryptRefId = cryptoUtil.encryptRefId(STR_INPUT);
		assertNull(encryptRefId);
	}
	
	@Test
	public void encryptRefIdTestException() {
		Mockito.doThrow(RuntimeException.class).when(crypto).encrypt(Mockito.anyString());
		String encryptRefId = cryptoUtil.encryptRefId(STR_INPUT);
		assertEquals("", encryptRefId);
	}
	
	@Test
	public void encryptPkiAesTest() {
		Mockito.doReturn(STR_OUTPUT).when(pikAesEncryption).encrypt(Mockito.anyString());
		String encrypt = cryptoUtil.encryptPkiAes(STR_INPUT);
		assertEquals(STR_OUTPUT, encrypt);
	}

	@Test
	public void encryptPkiAesTest_Error() {
		Mockito.doThrow(RuntimeException.class).when(pikAesEncryption).encrypt(Mockito.anyString());
		String encrypt = cryptoUtil.encryptPkiAes(STR_INPUT);
		assertNull(encrypt);
	}
	
	@Test
	public void decryptPkiAesTest() {
		Mockito.doReturn(STR_OUTPUT).when(pikAesEncryption).decrypt(Mockito.anyString());
		String decryptString = cryptoUtil.decryptPkiAes(STR_INPUT);
		assertEquals(STR_OUTPUT, decryptString);
	}
	
	@Test
	public void decryptPkiAesTest_Error() {
		Mockito.doThrow(RuntimeException.class).when(pikAesEncryption).decrypt(Mockito.anyString());
		String decryptString = cryptoUtil.decryptPkiAes(STR_INPUT);
		assertNull(decryptString);
	}
	
	@Test
	public void stringEncryptEcdhTest() throws ParseException, JOSEException {
		jwtKey.setProductId("GovernmentIdApi");
		jwtKey.setKid("b8c991e6-d7e7-4959-9273-4a065b2be2db");
		jwtKey.setX(x);
		jwtKey.setY(y);
		jwtKey.setAlg("ECDH-ES");
		String encodedPayload = CryptoUtil.encryptEcdh(jwtKey, PLAINTEXT);
		System.out.println(encodedPayload);
			
		Pair<byte[], String> decodedPayload = cryptoUtil.decryptEcdh(encodedPayload, capitalOneJWK);
		
		assertEquals(PLAINTEXT, decodedPayload.getSecond());
	}
	
	@Test
	public void stringEncryptEcdhTest_Error() throws JOSEException {
		jwtKey.setProductId("GovernmentIdApi");
		jwtKey.setKid("b8c991e6-d7e7-4959-9273-4a065b2be2db");
		jwtKey.setX(x);
		jwtKey.setY(y);
		jwtKey.setAlg("");
		String encryptString = CryptoUtil.encryptEcdh(jwtKey, PLAINTEXT);
		assertNotNull(encryptString);
		ReflectionTestUtils.setField(jwtKey, "alg", null);
		String encryptString2 = CryptoUtil.encryptEcdh(jwtKey, PLAINTEXT);
		assertNotNull(encryptString2);
	}
	
	@Test
	public void imageEncryptEcdhTest() throws ParseException, JOSEException, IOException {		
	
		String string = "asdf";
		System.arraycopy(string.getBytes(), 0, testArray, 50000 - string.length(), string.length());
		System.arraycopy(string.getBytes(), 0, testArray2, 10000 - string.length(), string.length());
		
		jwtKey.setProductId("GovernmentIdApi");
		jwtKey.setKid("b8c991e6-d7e7-4959-9273-4a065b2be2db");
		jwtKey.setX(x);
		jwtKey.setY(y);
		jwtKey.setAlg("ECDH-ES");
		
		System.out.println("\n byte Array1 Size before encryption");
		System.out.println(testArray.length);
		
		System.out.println("\n byte Array2 Size before encryption");
		System.out.println(testArray2.length);
		
		String encodedPayload = CryptoUtil.encryptEcdh(jwtKey, testArray);
		
		String encodedPayload2 = CryptoUtil.encryptEcdh(jwtKey, testArray2);
		System.out.println("\n Byte Array1 Size After encryption");
		System.out.println(encodedPayload.getBytes().length);
		
		System.out.println("\n Byte Array2 Size After encryption");
		System.out.println(encodedPayload2.getBytes().length);
		byte[] noneBase64 = Base64.getDecoder().decode(encodedPayload);
		String noneBase64Str = new String(noneBase64);
		Pair<byte[], String> decodedPayload = cryptoUtil.decryptEcdh(noneBase64Str, capitalOneJWK);
		assertArrayEquals(testArray, decodedPayload.getFirst());
		
	}

	@Test
	public void imageEncryptEcdhTest_Error() throws JOSEException {		
	
		String string = "asdf";
		System.arraycopy(string.getBytes(), 0, testArray2, 10000 - string.length(), string.length());
		jwtKey.setProductId("GovernmentIdApi");
		jwtKey.setKid("b8c991e6-d7e7-4959-9273-4a065b2be2db");
		jwtKey.setX(x);
		jwtKey.setY(y);
		jwtKey.setAlg("");
		String encryptString = CryptoUtil.encryptEcdh(jwtKey, testArray2);
		assertNotNull(encryptString);
		ReflectionTestUtils.setField(jwtKey, "alg", null);
		String encryptString2 = CryptoUtil.encryptEcdh(jwtKey, testArray2);
		assertNotNull(encryptString2);
		
	}
	
	@Test
	public void jwtKeyTest() {
		
		jwtKey.setExp("1346524199");
		String exp = jwtKey.getExp();
		assertEquals("1346524199", exp);
		
		boolean testVal = true;
		assertEquals(testVal, jwtKey.isExpired());
	}

	@Test
	public void stringEncryptRsaTest() throws JOSEException, NoSuchAlgorithmException, ParseException {
		jwtKey.setProductId("GovernmentIdApi");
		jwtKey.setKid("b8c991e6-d7e7-4959-9273-4a065b2be2db");
		jwtKey.setAlg("RSA-RS256");
		KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
		generator.initialize(2048);
		KeyPair keyPair = generator.generateKeyPair();

		String encodedPayload = CryptoUtil.encryptRsa(keyPair, PLAINTEXT);
		System.out.println(encodedPayload);

		Pair<byte[], String> decodedPayload = cryptoUtil.decryptRsa(encodedPayload, keyPair);

		assertEquals(PLAINTEXT, decodedPayload.getSecond());
	}

	@Test
	public void byteEncryptRsaTest() throws JOSEException, NoSuchAlgorithmException, ParseException {
		jwtKey.setProductId("GovernmentIdApi");
		jwtKey.setKid("b8c991e6-d7e7-4959-9273-4a065b2be2db");
		jwtKey.setAlg("RSA-RS256");
		KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
		generator.initialize(2048);
		KeyPair keyPair = generator.generateKeyPair();

		String encodedPayload = CryptoUtil.encryptRsa(keyPair, PLAINTEXT.getBytes());
		System.out.println(encodedPayload);

		Pair<byte[], String> decodedPayload = cryptoUtil.decryptRsa(encodedPayload, keyPair);

		assertEquals(PLAINTEXT, decodedPayload.getSecond());
	}

	@Test
	public void stringEncryptRsaTest_IllegalArgumentError() throws NoSuchAlgorithmException {
		jwtKey.setProductId("GovernmentIdApi");
		jwtKey.setKid("b8c991e6-d7e7-4959-9273-4a065b2be2db");
		jwtKey.setAlg("RSA-RS256");
		KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
		generator.initialize(2048);
		KeyPair keyPair = generator.generateKeyPair();

		assertThrows(IllegalArgumentException.class, () -> CryptoUtil.encryptRsa(keyPair, 123));
	}

	@Test
	public void invalidPublicKeyRsa_Error() throws NoSuchAlgorithmException {
		jwtKey.setProductId("GovernmentIdApi");
		jwtKey.setKid("b8c991e6-d7e7-4959-9273-4a065b2be2db");
		jwtKey.setAlg("RSA-RS256");
		KeyPair keyPair = new KeyPair(null,null);

		assertThrows(IllegalArgumentException.class, () -> CryptoUtil.encryptRsa(keyPair,PLAINTEXT));
	}

	@Test
	public void invalidPrivateKeyRsa_Error() throws NoSuchAlgorithmException, JOSEException {
		jwtKey.setProductId("GovernmentIdApi");
		jwtKey.setKid("b8c991e6-d7e7-4959-9273-4a065b2be2db");
		jwtKey.setAlg("RSA-RS256");
		KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
		generator.initialize(2048);
		KeyPair validKeyPair = generator.generateKeyPair();
		KeyPair invalidKeyPair = generator.generateKeyPair();

		String encodedPayload = CryptoUtil.encryptRsa(validKeyPair,PLAINTEXT);
		assertThrows(JOSEException.class, () -> cryptoUtil.decryptRsa(encodedPayload, invalidKeyPair));
	}

	@Test
	public void nullPrivateKeyRsa_Error() throws NoSuchAlgorithmException, JOSEException {
		jwtKey.setProductId("GovernmentIdApi");
		jwtKey.setKid("b8c991e6-d7e7-4959-9273-4a065b2be2db");
		jwtKey.setAlg("RSA-RS256");
		KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
		generator.initialize(2048);
		KeyPair validKeyPair = generator.generateKeyPair();
		KeyPair keyPair = new KeyPair(validKeyPair.getPublic(), null);

		String encodedPayload = CryptoUtil.encryptRsa(keyPair,PLAINTEXT);
		assertThrows(NullPointerException.class, () -> cryptoUtil.decryptRsa(encodedPayload, keyPair));
	}
}
