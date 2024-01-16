package com.capitalone.identity.identitybuilder.policycore.camel.util;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.lang3.StringUtils;

import com.capitalone.chassis.engine.model.exception.ChassisErrorCode;
import com.capitalone.chassis.engine.model.exception.RequestValidationException;
import com.capitalone.identity.identitybuilder.policycore.service.constants.ApplicationConstants;
import com.capitalone.identity.identitybuilder.policycore.service.exception.AttributeValidationException;
import com.capitalone.identity.identitybuilder.policycore.service.util.CryptoUtil;

/**
 * A request parameter definition that combines a parameter name with options affecting
 * processing behavior.
 * <p>
 * A parameter reference string has the following form:
 * <pre>    name{option:option:...}</pre>
 * <p>
 * By default, parameter names are required.
 * The curly braces may be omitted if there are no options. Valid options are:
 * <p>
 * <table border="1" cellpadding="4">
 * <tr><th align="center">Option</th><th>Meaning</th></tr>
 * <tr><td align="center">rename=<i>name</i></td><td>rename the field to the given name</td></tr>
 * <tr><td align="center">type=<i>code</i></td><td>convert a string value to a different type</td></tr>
 * </table>
 * <p>
 * Supported type conversion codes:
 * <p>
 * <table border="1" cellpadding="4">
 * <tr><th align="center">Type code</th><th>Effect</th></tr>
 * <tr><td align="center">alphanum</td><td>non-alphanumeric characters are removed (including whitespace)
 * 		(e.g.,from "(703) 555-1212" to "7035551212")</td></tr>
 * <tr><td align="center">b | boolean</td><td>value is converted to <code>Boolean</code>
 * 		and must be either "true" or "false" (case insensitive).
 * 		This is a more restrictive conversion than "javaBoolean".</td></tr>
 * <tr><td align="center">country3</td><td>converts an ISO 3166 2-character country code to a 3-character code</br>
 *      (e.g., from "US" to "USA", or "CA" to "CAN")</td></tr>
 * <tr><td align="center">encrypt</td><td>value is encrypted</td></tr>
 * <tr><td align="center">aesencrypt</td><td>value is encrypted using AES</td></tr>
 * <tr><td align="center">i | integer</td><td>value is converted to <code>Integer</code></td></tr>
 * <tr><td align="center">javaBoolean</td><td>value is converted to <code>Boolean</code>
 * 		using <code>Java Boolean.parseBoolean()</code>, so any value other than
 * 		"true" (case insensitive) becomes <i>false</i>.
 * 		This is a more lenient conversion than "boolean"</td></tr>
 * <tr><td align="center">uppercase</td><td>value is converted to uppercase</td></tr>
 * <tr><td align="center">urlEncode</td><td>value is URL-encoded</td></tr>
 * </table>
 * <p>
 * Some examples:
 * <p>
 * <table border="1" cellpadding="4">
 * <tr><th>Parameter Reference</th><th align="left">Interpretation</th></tr>
 * <tr><td>foo</td><td>required value with no other processing</td></tr>
 * <tr><td>foo{type=boolean}</td><td>convert to a <code>Boolean</code></td></tr>
 * <tr><td>foo{type=i:rename=bar}</td><td>convert to an <code>Integer</code> and save as "bar" instead of "foo"</td></tr>
 * </table>
 * 
 * @author oqu271
 */

@Named
public class RequestParameter {
	
	private static CryptoUtil cryptoUtil;
	
	/** Pattern to match a name followed by options as {a:b:c=d:..}. */
	@SuppressWarnings("squid:S4784")
	private static final Pattern pattern = Pattern.compile("([^\\{\\}\\[\\]]+)(?:\\{(.*)\\})?");
	
	/** Valid type annotation values. */
	public enum ConversionType { INTEGER, BOOLEAN, JAVA_BOOLEAN, URL_ENCODE, ALPHA_NUM, COUNTRY_3, UPPERCASE, ENCRYPT,AES_ENCRYPT}
	
	/** Defines valid type conversion values. */
	private static Map<String, ConversionType> typeMap = new HashMap<>();
	static {
		typeMap.put("i",           ConversionType.INTEGER);
		typeMap.put("integer",     ConversionType.INTEGER);
		typeMap.put("b",           ConversionType.BOOLEAN);
		typeMap.put("boolean",     ConversionType.BOOLEAN);
		typeMap.put("javaBoolean", ConversionType.JAVA_BOOLEAN);
		typeMap.put("urlEncode",   ConversionType.URL_ENCODE);
		typeMap.put("alphanum",    ConversionType.ALPHA_NUM);
		typeMap.put("country3",    ConversionType.COUNTRY_3);
		typeMap.put("uppercase",   ConversionType.UPPERCASE);
		typeMap.put("encrypt",     ConversionType.ENCRYPT);
		typeMap.put("aesencrypt",  ConversionType.AES_ENCRYPT);
	}

	/** The full parameter declaration used to define this parameter (e.g., "foo{o:type=i}"). */
	private String declaration;
	/** The actual parameter name after options have been removed. */
	private String name;
	/** An optional name to rename the field to. */
	private String newName;
	/** An optional type to convert field values to. */
	private ConversionType newType = null;	
	
	/**
	 * This is a constructor injection hack.  During startup, Spring will see this
	 * as a bean due to the @Named annotation and call this method to inject the
	 * value into the bean but instead we'll set a static value that will be used
	 * for future instantiations.
	 */
	@Inject
	@SuppressWarnings("squid:S3010")
	public RequestParameter(CryptoUtil util) {
		cryptoUtil = util;
	}

	/**
	 * Constructs a parameter name definition from a (possibly annotated) string.
	 * 
	 * @param text the parameter name string to parse
	 * @see {@link RequestParameter} for the parameter name annotation syntax
	 */
	public RequestParameter(String text) {
		declaration = text;
		name = text;
		Matcher m = pattern.matcher(text);
		if (m.matches()) {
			name = m.group(1);
			// If an option group is present, set the options accordingly.
			if (m.group(2) != null) {
				if (m.group(2).length() == 0) {
					throw new IllegalArgumentException(String.format("Missing parameter name options in \"%s\"", text));
				}
				for (String option : m.group(2).split(":")) {
					processAnnotation(option);	
				}			
			}
		} else {
			throw new IllegalArgumentException(String.format("Invalid header name \"%s\"", name));
		}
	}
	
	/**
	 * Process an individual parameter annotation string, setting the appropriate instance variables as needed.
	 * 
	 * @param option the parameter annotation to process (e.g., "a" or "a=b")
	 */
	private void processAnnotation(String option) {
		// Check for "key=value" vs. "key" and handle the latter case first. 
		String[] tokens = option.split("=", 2);
		if (tokens.length == 1) {
			// No unary options at this time.
			throw new IllegalArgumentException(String.format("Unknown parameter annotation \"%s\" in \"%s\"", option, declaration));
		}

		// We know we have a key/value parameter.  Neither of them can be empty.
		String key = tokens[0];
		String value = tokens[1];
		if (key.isEmpty()) {
			throw new IllegalArgumentException(String.format("Missing parameter annotation for \"%s\" in \"%s\"", option, declaration));								
		}
		if (value.isEmpty()) {
			throw new IllegalArgumentException(String.format("Missing parameter annotation value for \"%s\" in \"%s\"", option, declaration));								
		}
		
		// Process each key differently.
		switch (key) {
		case "rename": newName = handleBinaryAnnotation(key, value, newName, (Map<String, String>) null); break;
		case "type":   newType = handleBinaryAnnotation(key, value, newType, typeMap); break;
		default:
			throw new IllegalArgumentException(String.format("Unrecognized annotation key \"%s\" in \"%s\"", key, declaration));
		}
	}
	
	/**
	 * Processes a binary option with multiple checks.  If a value map is supplied,
	 * the value is used as a key and the map value is returned as the value.
	 * 
	 * @param key the annotation key
	 * @param value the annotation value
	 * @param previousValue the previous value for this annotation (must be <code>null</code>)
	 * @param valueMap an optional map of allowed values
	 * @return the valid value (extracted from the map if provided)
	 * @throws IllegalArgumentException if the previous value is not <code>null</code> or the value
	 * 		is not in the value map.
	 */
	@SuppressWarnings("unchecked")
	private <T> T handleBinaryAnnotation(String key, String value, Object previousValue, Map<String, T> valueMap) {
		if (previousValue != null) {
			throw new IllegalArgumentException(String.format("Duplicate %s annotations were found for a single parameter: \"%s\"", key, declaration));
		}

		if (valueMap != null && !valueMap.containsKey(value)) {
			throw new IllegalArgumentException(String.format("Invalid annotation value \"%s\" for \"%s\" in \"%s\"", value, key, declaration));
		}
		return valueMap != null ? valueMap.get(value) : (T) value;
	}
	
	/**
	 * Returns an array of parameter name definitions created from a list of
	 * (possibly annotated) parameter names. This is a convenience method that
	 * calls the constructor repeatedly.
	 * 
	 * @param names one or more (possibly annotated) parameter names
	 * @see {@link RequestParameter} for the parameter name annotation syntax
	 */
	public static RequestParameter[] fromArray(String... names) {
		RequestParameter[] result = new RequestParameter[names.length];
		for (int i = 0; i < names.length; i++) {
			result[i] = new RequestParameter(names[i]);
		}
		return result;
	}

	/**
	 * Process a string value for the parameter into an appropriate value object.
	 * <p>
	 * <ul>
	 * <li>Required values that are <code>null</code> result in an exception</code></li>
	 * <li>Optional values that are <code>null</code> return <code>null</code></li>
	 * <li>A syntax check is performed, if desired</li>
	 * <li>A type transformation is performed, if desired</li>
	 * </ul>
	 * 
	 * @param  value the parameter value to be processed (may be <code>null</code>)
	 * @return an appropriate value object for the parameter
	 * @throws RequestValidationException if the parameter value is invalid
	 */
	public Object toValue(Object value) {
		// Handle missing values.  Enforcing required values is done using the schema.
		if (value == null || (value instanceof String && StringUtils.isBlank((String) value))) {
			return null;
		}
		
		if (value instanceof String) {
			// Do any necessary type conversions.
			value = (newType != null && value instanceof String) ? resolveType((String) value) : value; 
		} else {
			// The type conversion code only runs against string values.
			// The schema checker should catch this but just in case, we'll check here too.
			if (newType != null) {
				throw makeException(name, String.format("Attribute \"%s\" must be a string for syntax checks or type conversions", name));
			}
		}
		return value;

	}
	
	/**
	 * Returns the value after applying an optional type conversion. 
	 */
	private Object resolveType(String value) {
		switch (newType) {
		case BOOLEAN:
			if("true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value)) {
				return Boolean.parseBoolean(value);
			}
			else {
				throw makeException(name, String.format("Attribute \"%s\" requires a boolean value", name));
			}
		case JAVA_BOOLEAN:
			return Boolean.parseBoolean(value);
		case INTEGER:
			try {
				return Integer.parseInt(value);
			} catch (NumberFormatException e) {
				throw makeException(name, String.format("Attribute \"%s\" requires an integer value", name));
			}
		case URL_ENCODE:
			try {
				return URLEncoder.encode(value, "UTF-8");
			} catch (UnsupportedEncodingException e) {
				throw new IllegalStateException("UTF-8 should always be supported");
			}
		case ALPHA_NUM:
			return value.replaceAll("[^\\da-zA-Z]", "");
		case UPPERCASE:
			return value.toUpperCase();
		case COUNTRY_3:
			try {
				return new Locale("", value).getISO3Country();
			} catch (MissingResourceException e) {
				throw makeException(name, String.format("Attribute \"%s\" is not a valid ISO 3166 2-character country code", name));
			}
		case ENCRYPT:
			if(cryptoUtil != null )
				return cryptoUtil.encrypt(value);
			else
				throw makeException(name, String.format("CryptoUtil bean is \"%s\". It is not set to load ", cryptoUtil));
		case AES_ENCRYPT:
			if(cryptoUtil != null )
				return cryptoUtil.encryptRefId(value);
			else
				throw makeException(name, String.format("CryptoUtil bean is \"%s\". It is not set to load ", cryptoUtil));
		default:
			throw new IllegalStateException();
		}
	}
	
	/**
	 * Returns an exception for an invalid attribute condition.
	 * <p>
	 * Throwing the exception here would be cleaner but SonarQube demands that
	 * we explicitly throw the exception directly from in the case statement.
	 * 
	 * @param  name the name of the invalid attribute
	 * @param  message the developer text
	 * @return the exception to throw
	 */
	private RequestValidationException makeException(String name, String message) {
		ChassisErrorCode error = new ChassisErrorCode(ApplicationConstants.INVALID_ATTRIBUTE_ID, ApplicationConstants.INVALID_ATTRIBUTE_TEXT, ApplicationConstants.INVALID_ATTRIBUTE_DEV_TEXT);
		error.getDeveloperTextMessageParms().add(name);
		return new RequestValidationException(error, new AttributeValidationException(message));					
	}

	public String getDeclaration() {
		return declaration;
	}

	public String getName() {
		return name;
	}

	public String getNewName() {
		return newName;
	}

	public ConversionType getConversionType() {
		return newType;
	}
	
	@Override
	public String toString() {
		return declaration;
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
