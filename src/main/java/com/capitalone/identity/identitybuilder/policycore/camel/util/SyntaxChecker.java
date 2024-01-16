package com.capitalone.identity.identitybuilder.policycore.camel.util;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.util.Arrays;
import java.util.Collection;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Validates the syntax of a string - used to check the syntax of parameters.
 * <p>
 * There are some built-in static checkers for well-known formats.
 * There is also a factory methods to evaluate:
 * <ul>
 * <li>Static constants for well-known syntaxes</li>
 * <li>A constructor that takes a <i>lambda expression</i></li>
 * <li>Factory methods for common types of checking
 * 		<ul>
 * 		<li>Arbitrary regular expressions</li>
 * 		<li>A fixed list of acceptable values</li>
 * 		</ul>
 * </li>
 * </ul> 
 * <h4>Examples</h4>
 * <pre>
 * SyntaxChecker.NINE_DIGITS;
 * SyntaxChecker.regex("\d+", "must be one or more digits");
 * SyntaxChecker.fixedValues(Arrays.asList("yes", "no"), "must be yes or no", true);
 * SyntaxChecker.fixedValues(Arrays.asList("yes", "no"), "must be yes or no (case insensitive)", false);
 * </pre>
 * 
 * @author OQU271
 */

public class SyntaxChecker {	
	/** A regular expression pattern for a YYYY-MM-DD date. */
	@SuppressWarnings("squid:S4784")
	private static final Pattern DATE_PATTERN = Pattern.compile("^\\d{4}-\\d{2}-\\d{2}$");
	
	/** A set of ISO-3166 2-character country codes. */
	private static final Set<String> ISO_COUNTRIES = new HashSet<> (Arrays.asList(Locale.getISOCountries()));
	
	private static final Set<String> STATE_CODES = new TreeSet<> (Arrays.asList(
			// 50 US States
			"AL", "AK", "AR", "AZ", "CA", "CO", "CT", "DE", "FL", "GA",
			"HI", "IA", "ID", "IL", "IN", "KS", "KY", "LA", "MA", "MD",
			"ME", "MI", "MN", "MO", "MS", "MT", "NC", "ND", "NE", "NH",
			"NJ", "NM", "NV", "NY", "OH", "OK", "OR", "PA", "RI", "SC",
			"SD", "TN", "TX", "UT", "VA", "VT", "WA", "WI", "WV", "WY",
			// District of Columbia and supported territories
			"AS", "DC", "GU", "PR", "VI"
			));

	/** A syntax checker for a nine-digit string (e.g., tax id). */
	public static final SyntaxChecker NINE_DIGITS = regex("^\\d{9}$", "must be nine digits");
	
	/** A syntax checker for a ten-digit string (e.g., phone number). */
	public static final SyntaxChecker TEN_DIGITS = regex("^\\d{10}$", "must be ten digits");
	
	/**
	 * A syntax checker for valid date in the format YYYY-MM-DD.
	 * Pattern matching does not validate correct dates, and date parsing is too lenient
	 * (accepting any number of digits for the year), so we must use both. 
	 */
	public static final SyntaxChecker YYYYMMDD_DATE = new SyntaxChecker(SyntaxChecker::dateFormatValidate, "must be a valid YYYY-MM-DD date");

	/** A syntax checker for the ISO-8601 extended offset date-time format. */
	public static final SyntaxChecker ISO_8601_DATE = new SyntaxChecker(
			t -> DateTimeFormatter.ISO_OFFSET_DATE_TIME.withResolverStyle(ResolverStyle.STRICT).parse(t) != null,
			"must be a valid ISO-8601 extended offset date-time format");
	
	/** A syntax checker for an ISO-3166 alpha-2 country code. */
	public static final SyntaxChecker COUNTRY_2 = new SyntaxChecker(t -> ISO_COUNTRIES.contains(t.toUpperCase()),
			"must be a valid ISO-3166 alpha-2 country code");
	
	/** A syntax checker for a list of supported state codes. */
	public static final SyntaxChecker STATECODE = new SyntaxChecker(t -> STATE_CODES.contains(t.toUpperCase()),
			"must be one of the following state codes: " + STATE_CODES);
	
	/** The syntax validation predicate. */
	private Predicate<String> validator;
	/** An error string describing the expected syntax. */
	private String errorText;
	
	/**
	 * Constructs a syntax checker using an arbitrary regular expression.
	 * 
	 * @param regEx the regular expression to check the syntax
	 * @param errorText a description of the validation error (e.g., "must be nine digits")
	 */
	public static SyntaxChecker regex(String regEx, String errorText) {
		return new SyntaxChecker(t -> Pattern.matches(regEx, t), errorText);
	}
	
	/**
	 * Constructs a syntax checker to match against a collection of strings.
	 * <p>
	 * If the caller does not provide error text, error text will be automatically generated
	 * in the following format:
	 * <pre>
	 *    "must be one of the following values: [value1, value2, ...] (case-sensitive)"
	 * (or)
   	 *    "must be one of the following values: [value1, value2, ...] (case-insensitive)"
   	 * </pre>
   	 * where the list of values is ordered and, if case-insensitive, converted to lowercase.
	 * 
	 * @param values the values to check against
	 * @param errorText an optional description of the validation error (may be <code>null</code>)
	 * @param caseSensitive whether the values are to be compared with case sensitivity
	 */
	public static SyntaxChecker fixedValues(Collection<String> values, String errorText, boolean caseSensitive) {
		Set<String> set = (caseSensitive) ? new TreeSet<>(values) : new TreeSet<>(values.stream().map(String::toLowerCase).collect(Collectors.toSet()));
		if (errorText == null) {
			errorText = "must be one of the following values: " + set + (caseSensitive ? " (case-sensitive)" : " (case-insensitive)");
		}
		if (caseSensitive) {
			return new SyntaxChecker(set::contains, errorText);
		} else {
			return new SyntaxChecker(t -> set.contains(t.toLowerCase()), errorText);
		}
	}
	
	/**
	 * Constructs a syntax checker with a validation predicate
	 * (usually a <i>lambda function</i>) and an error text.
	 * <p>
	 * If the validator throws an exception it is caught and treated
	 * as a <i>false</i> result.
	 *  
	 * @param validator the validation predicate to use
	 * @param errorText a description of the validation error (e.g., "must be nine digits") 
	 */
	public SyntaxChecker(Predicate<String> validator, String errorText) {
		this.validator = validator;
		this.errorText = errorText;
	}

	/**
	 * Tests to see if the supplied text is syntactically valid.
	 * <p>
	 * If the validator throws an exception it is caught and treated
	 * as a <i>false</i> result.
	 * 
	 * @param text the value to check
	 * @return whether it is valid
	 */
	public boolean matches(String text) {
		try {
			return validator.test(text);
		} catch (Exception e) {
			return false;
		}
	}
	
	/**
	 * Returns the error text associated with a failed value match (e.g., "must be nine digits").
	 * 
	 * @return the error text for this syntax checker
	 */
	public String getErrorText() {
		return errorText;
	}
	
	/**
	 * Returns whether the text supplied is a valid date of the format YYYY-MM-DD.
	 * <p>
	 * Using a date format with Gregorian calendar and strict parsing will ensure
	 * that the date is a valid date (i.e., not March 41st or Feb 29th in a
	 * non-leap-year).  Unfortunately, even the strict date format will accept any
	 * number of digits for the year so we'll enforce 4 digits with a regular
	 * expression.
	 * 
	 * @return whether the text is a valid date of the format YYYY-MM-DD
	 */
	private static boolean dateFormatValidate(String text) {
		// Note: Unlike Pattern, DateFormat is not threadsafe so we'll make a new one each time.
		DateFormat format = new SimpleDateFormat("yyyy-MM-dd");
		format.setCalendar(new GregorianCalendar());
		format.setLenient(false);
		
		try {
			return format.parse(text) != null && DATE_PATTERN.matcher(text).matches();
		} catch (ParseException e) {
			return false;
		}
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
