package com.capitalone.identity.identitybuilder.policycore.camel.util;

import java.util.Arrays;

import org.apache.camel.Converter;

/**
 * Implements our own Camel type converters.
 * 
 * @author oqu271
 */
@Converter
public final class TestTypeConverters {
	
	/**
	 * Declare a private constructor to hide the implict one.
	 */
	private TestTypeConverters() {
		throw new IllegalStateException("Utility class");
	}
	
	/** 
	 * Converts a string of comma-delimited values into an array of those values.
	 * <p>
	 * <ul>
	 * <li>The values are trimmed of leading and trailing whitespace</li>
	 * <li>Whitespace values are ignored</li>
	 * </ul>
	 * 
	 * @param value the comma-delimited values to convert
	 * @return an array of the individual (trimmed) values, will be of zero-length
	 *         if there are no non-whitespace values
	 */
    @Converter
    public static String[] toStringArray(String value)
    {
        return Arrays.stream(value.split(",")).map(String::trim).toArray(String[]::new);
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
