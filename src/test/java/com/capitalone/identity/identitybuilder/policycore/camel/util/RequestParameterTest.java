package com.capitalone.identity.identitybuilder.policycore.camel.util;


import com.capitalone.chassis.engine.model.exception.RequestValidationException;
import com.capitalone.identity.identitybuilder.policycore.camel.util.RequestParameter.ConversionType;
import com.capitalone.identity.identitybuilder.policycore.service.util.CryptoUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class RequestParameterTest {

	@Mock
	static private CryptoUtil cryptoUtil;

	@Test
	public void testValidDescriptors() {
		checkItem(new RequestParameter("foo"),                  "foo", null);
		checkItem(new RequestParameter("foo{type=i}"),          "foo", ConversionType.INTEGER);
		checkItem(new RequestParameter("foo{type=integer}"),    "foo", ConversionType.INTEGER);
		checkItem(new RequestParameter("foo{type=b}"),          "foo", ConversionType.BOOLEAN);
		checkItem(new RequestParameter("foo{type=boolean}"),    "foo", ConversionType.BOOLEAN);
		checkItem(new RequestParameter("foo{type=javaBoolean}"),"foo", ConversionType.JAVA_BOOLEAN);
		checkItem(new RequestParameter("foo{type=i}"),          "foo", ConversionType.INTEGER);
		checkItem(new RequestParameter("foo{type=boolean}"),    "foo", ConversionType.BOOLEAN);
		checkItem(new RequestParameter("foo{type=urlEncode}"),  "foo", ConversionType.URL_ENCODE);
		checkItem(new RequestParameter("foo{type=alphanum}"),   "foo", ConversionType.ALPHA_NUM);
		checkItem(new RequestParameter("foo{type=country3}"),   "foo", ConversionType.COUNTRY_3);
		checkItem(new RequestParameter("foo{type=uppercase}"),  "foo", ConversionType.UPPERCASE);
		checkItem(new RequestParameter("foo{type=encrypt}"),    "foo", ConversionType.ENCRYPT);
		checkItem(new RequestParameter("foo{type=aesencrypt}"), "foo", ConversionType.AES_ENCRYPT);
	}

	@Test
	public void testGetDeclaration() {
		String declaration = "foo{type=integer}";
		assertEquals(declaration, new RequestParameter(declaration).getDeclaration());
	}

	@Test
	public void testMissingFlags() {
		assertThrows(
				IllegalArgumentException.class,
				() -> new RequestParameter("foo{}")
		);
	}

	@Test()
	public void testBinaryMissingValue() {
		assertThrows(
				IllegalArgumentException.class,
				() -> new RequestParameter("foo{type=}")
		);
	}

	@Test
	public void testBinaryMissingKey() {
		assertThrows(
				IllegalArgumentException.class,
				() -> new RequestParameter("foo{=value}")
		);
	}

	@Test
	public void testBinaryInvalidKey() {
		assertThrows(
				IllegalArgumentException.class,
				() -> new RequestParameter("foo{bad=value}")
		);
	}

	@Test
	public void testInvalidType() {
		assertThrows(
				IllegalArgumentException.class,
				() -> new RequestParameter("foo{type=xyz}")
		);
	}

	@Test
	public void testMissingType() {
		assertThrows(
				IllegalArgumentException.class,
				() -> new RequestParameter("foo{type=}")
		);
	}

	@Test
	public void testMultipleType() {
		assertThrows(
				IllegalArgumentException.class,
				() -> new RequestParameter("foo{type=i:type=b}")
		);
	}

	@Test
	public void testMissingRename() {
		assertThrows(
				IllegalArgumentException.class,
				() -> new RequestParameter("foo{rename=}")
		);
	}

	@Test
	public void testMultipleRename() {
		assertThrows(
				IllegalArgumentException.class,
				() -> new RequestParameter("foo{rename=bar1:rename=bar2}")
		);
	}

	@Test
	public void testUnknownFlag() {
		assertThrows(
				IllegalArgumentException.class,
				() -> new RequestParameter("foo{x}")
		);
	}

	@Test
	public void testObsoleteIFlag() {
		assertThrows(
				IllegalArgumentException.class,
				() -> new RequestParameter("foo{i}")
		);
	}

	@Test
	public void testObsoleteBFlag() {
		assertThrows(
				IllegalArgumentException.class,
				() -> new RequestParameter("foo{b}")
		);
	}

	@Test
	public void testMalformed() {
		for (String text : Arrays.asList("{o}foo", "foo{o}b", "[foo", "{type=b}", "foo{type=i}}", "foo{type=i}]")) {
			try {
				new RequestParameter(text);
				fail(String.format("Attribute \"%s\" should have been treated as invalid", text));
			} catch (IllegalArgumentException expected) {
				// Expected result, so ignore.
			}
		}
	}

	@Test
	public void testFromArray() {
		RequestParameter[] array = RequestParameter.fromArray("foo", "bar{type=i}");
		checkItem(array[0], "foo", null);
		checkItem(array[1], "bar", ConversionType.INTEGER);
		assertEquals(2, array.length);
	}

	@Test
	public void testToValueNonString() {
		assertThrows(
				RequestValidationException.class,
				() -> new RequestParameter("foo{type=i}").toValue(42)
		);
	}

	@Test
	public void testToValueString() {
		assertEquals("bar", new RequestParameter("foo").toValue("bar"));
	}

	@Test
	public void testToValueInteger() {
		assertEquals(1234, new RequestParameter("foo{type=i}").toValue("1234"));
	}

	@Test
	public void testToValueBadInteger() {
		assertThrows(
				RequestValidationException.class,
				() -> new RequestParameter("foo{type=i}").toValue("bar")
		);
	}

	@Test
	public void testToValueBoolean() {
		assertEquals(true,  new RequestParameter("foo{type=b}").toValue("true"));
		assertEquals(true,  new RequestParameter("foo{type=b}").toValue("True"));
		assertEquals(false, new RequestParameter("foo{type=b}").toValue("false"));
		assertEquals(false, new RequestParameter("foo{type=b}").toValue("FALSE"));
	}

	@Test
	public void testToValueBadBoolean() {
		assertThrows(
				RequestValidationException.class,
				() -> assertEquals(false, new RequestParameter("foo{type=b}").toValue("not-true"))
		);
	}

	@Test
	public void testToValueJavaBoolean() {
		assertEquals(true,  new RequestParameter("foo{type=javaBoolean}").toValue("true"));
		assertEquals(true,  new RequestParameter("foo{type=javaBoolean}").toValue("True"));
		assertEquals(false, new RequestParameter("foo{type=javaBoolean}").toValue("false"));
		assertEquals(false, new RequestParameter("foo{type=javaBoolean}").toValue("FALSE"));
		assertEquals(false, new RequestParameter("foo{type=javaBoolean}").toValue("yes"));
		assertEquals(false, new RequestParameter("foo{type=javaBoolean}").toValue("f"));
		assertEquals(false, new RequestParameter("foo{type=javaBoolean}").toValue("xyz"));
	}

	@Test
	public void testToValueAlphaNum() {
		assertEquals("abcd",  new RequestParameter("foo{type=alphanum}").toValue(" a.b- c)d\"\t\r "));
	}

	@Test
	public void testToValueCountry3() {
		assertEquals("USA",  new RequestParameter("foo{type=country3}").toValue("us"));
		assertEquals("CAN",  new RequestParameter("foo{type=country3}").toValue("ca"));
	}

	@Test
	public void testToValueCountry3UnknownCountry() {
		assertThrows(
				RequestValidationException.class,
				() -> new RequestParameter("foo{type=country3}").toValue("xq")
		);
	}

	@Test
	public void testToValueCountry3Already3() {
		assertThrows(
				RequestValidationException.class,
				() -> new RequestParameter("foo{type=country3}").toValue("USA")
		);
	}

	@Test
	public void testValueMissing() {
		assertNull(new RequestParameter("foo").toValue(null));
	}

	@Test
	public void testSimpleValues() {
		assertEquals("bar", new RequestParameter("foo").toValue("bar"));
		new RequestParameter("mobilePhoneNumber").toValue("abcd");
		new RequestParameter("taxId").toValue("abcd");
	}

	@Test
	public void testRename() {
		assertNull(new RequestParameter("foo").getNewName());
		assertEquals("bar", new RequestParameter("foo{rename=bar}").getNewName());
	}

	@Test
	public void testRenameMissing() {
		assertThrows(
				IllegalArgumentException.class,
				() -> new RequestParameter("foo{rename=}")
		);
	}

	@Test
	public void testURLEncode() throws UnsupportedEncodingException {
		String unencoded = "43161QDz1PcUJR3K0rYzB8QW68OOyfzdUJ21RE/jHdI=";
		String encoded = URLEncoder.encode(unencoded, "UTF-8");
		assertNotEquals(unencoded, encoded);
		assertEquals(encoded, new RequestParameter("foo{type=urlEncode}").toValue(unencoded));
	}

	@Test
	public void testEncrypt() {
		String cipherText = "fake-encrypted-ciphertext";
		when(cryptoUtil.encrypt(anyString())).thenReturn(cipherText);
		new RequestParameter(cryptoUtil);
		assertEquals(cipherText, new RequestParameter("foo{type=encrypt}").toValue("bar"));
	}
	@Test
	public void testAESEncrypt() {
		String cipherText = "fake-encrypted-ciphertext";
		when(cryptoUtil.encryptRefId(anyString())).thenReturn(cipherText);
		new RequestParameter(cryptoUtil);
		assertEquals(cipherText, new RequestParameter("foo{type=aesencrypt}").toValue("bar"));
	}

	private void checkItem(RequestParameter f, String name, ConversionType type) {
		assertEquals(name, f.getName());
		assertEquals(type, f.getConversionType());
	}

	@Test
	public void testUppercase() {
		assertEquals("VA",  new RequestParameter("foo{type=uppercase}").toValue("Va"));
		assertEquals("VA",  new RequestParameter("foo{type=uppercase}").toValue("va"));
	}

}