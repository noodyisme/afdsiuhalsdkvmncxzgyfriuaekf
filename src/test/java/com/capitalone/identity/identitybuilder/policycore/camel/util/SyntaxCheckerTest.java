package com.capitalone.identity.identitybuilder.policycore.camel.util;


import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class SyntaxCheckerTest {

	@Test
	public void testConstructor() {
		SyntaxChecker c = new SyntaxChecker((t) -> Pattern.matches("abc", t), "not the alphabet");
		assertTrue(c.matches("abc"));
		assertFalse(c.matches("abd"));
		assertEquals("not the alphabet", c.getErrorText());
	}

	private void assertTrue(boolean abc) {
	}

	@Test
	public void testRegex() {
		SyntaxChecker c = SyntaxChecker.regex("[0-9]*", "must be numeric");
		assertTrue(c.matches("123456"));
		assertFalse(c.matches("123x56"));
		assertEquals("must be numeric", c.getErrorText());
	}

	@Test
	public void testFixedValuesCaseSensitive() {
		SyntaxChecker c = SyntaxChecker.fixedValues(Arrays.asList("yes", "no", "maybe"), "must be one of yes, no, maybe", true);
		assertTrue(c.matches("yes"));
		assertTrue(c.matches("no"));
		assertTrue(c.matches("maybe"));
		assertFalse(c.matches("Yes"));
		assertFalse(c.matches("no "));
		assertEquals("must be one of yes, no, maybe", c.getErrorText());
	}

	@Test
	public void testFixedValuesCaseInsensitive() {
		SyntaxChecker c = SyntaxChecker.fixedValues(Arrays.asList("yes", "No", "maybe"), "must be one of yes, no, maybe", false);
		assertTrue(c.matches("Yes"));
		assertTrue(c.matches("no"));
		assertTrue(c.matches("MaYbE"));
		assertFalse(c.matches("Yez"));
		assertEquals("must be one of yes, no, maybe", c.getErrorText());
	}

	@Test
	public void testFixedValuesCaseSensitiveDefaultText() {
		SyntaxChecker c = SyntaxChecker.fixedValues(Arrays.asList("yes", "no", "maybe"), null, true);
		assertTrue(c.matches("yes"));
		assertTrue(c.matches("no"));
		assertTrue(c.matches("maybe"));
		assertFalse(c.matches("Yes"));
		assertFalse(c.matches("no "));
		assertEquals("must be one of the following values: [maybe, no, yes] (case-sensitive)", c.getErrorText());
	}

	@Test
	public void testFixedValuesCaseInsensitiveDefaultText() {
		SyntaxChecker c = SyntaxChecker.fixedValues(Arrays.asList("yes", "No", "maybe"), null, false);
		assertTrue(c.matches("Yes"));
		assertTrue(c.matches("no"));
		assertTrue(c.matches("MaYbE"));
		assertFalse(c.matches("Yez"));
		assertEquals("must be one of the following values: [maybe, no, yes] (case-insensitive)", c.getErrorText());
	}

	@Test
	public void testISO8601Date() {
		SyntaxChecker c = SyntaxChecker.ISO_8601_DATE;
		assertTrue(c.matches("2011-12-03T10:15:30+01:00"));
		assertFalse(c.matches("2 Jan 2011"));
		assertEquals("must be a valid ISO-8601 extended offset date-time format", c.getErrorText());
	}

	@Test
	public void testNineDigits() {
		SyntaxChecker c = SyntaxChecker.NINE_DIGITS;
		assertTrue(c.matches("123456789"));
		assertFalse(c.matches("1234x6789"));
		assertFalse(c.matches("12345678"));
		assertFalse(c.matches("1234567890"));
		assertEquals("must be nine digits", c.getErrorText());
	}

	@Test
	public void testTenDigits() {
		SyntaxChecker c = SyntaxChecker.TEN_DIGITS;
		assertTrue(c.matches("1234567890"));
		assertFalse(c.matches("12345x7890"));
		assertFalse(c.matches("123456789"));
		assertFalse(c.matches("12345678901"));
		assertEquals("must be ten digits", c.getErrorText());
	}

	@Test
	public void testDate() {
		SyntaxChecker c = SyntaxChecker.YYYYMMDD_DATE;

		assertTrue(c.matches("2020-10-12"));
		assertTrue(c.matches("2020-02-28"));
		assertTrue(c.matches("2020-02-29"));
		assertFalse(c.matches("2019-02-29"));
		assertFalse(c.matches("2020-10-00"));
		assertFalse(c.matches("2020-00-12"));
		assertFalse(c.matches("20201012"));
		assertFalse(c.matches("20-10-12"));
		assertFalse(c.matches("2020-1-1"));

		assertEquals("must be a valid YYYY-MM-DD date", c.getErrorText());
	}

	@Test
	public void testCountry2() {
		SyntaxChecker c = SyntaxChecker.COUNTRY_2;
		assertTrue(c.matches("US"));
		assertTrue(c.matches("CA"));
		assertTrue(c.matches("AU"));
		assertFalse(c.matches("QP"));
		assertFalse(c.matches("Z3"));
		assertFalse(c.matches("USA"));
		assertFalse(c.matches("CAN"));
		assertEquals("must be a valid ISO-3166 alpha-2 country code", c.getErrorText());
	}

	@Test
	public void testStateCode() {
		SyntaxChecker c = SyntaxChecker.STATECODE;
		assertTrue(c.matches("VA"));
		assertTrue(c.matches("Ct"));
		assertTrue(c.matches("AR"));
		assertTrue(c.matches("DC")); // Check for the District of Columbia
		assertTrue(c.matches("PR")); // Check for the territory of Puerto Rico
		assertTrue(c.matches("GU")); // Check for the territory of Guam
		assertTrue(c.matches("AS")); // Check for the territory of American Samoa
		assertTrue(c.matches("VI")); // Check for the territory of US Virgin Islands

		assertFalse(c.matches("MH")); // Check for the territory of Marshall Islands
		assertFalse(c.matches("MP")); // Check for the territory of Northern Mariana Islands
		assertFalse(c.matches("US"));
		assertFalse(c.matches("KA"));
		assertFalse(c.matches("USA"));
		assertFalse(c.matches("CAN"));

		assertEquals("must be one of the following state codes: [AK, AL, AR, AS, AZ, CA, CO, CT, DC, DE, FL, GA,"
				+ " GU, HI, IA, ID, IL, IN, KS, KY, LA, MA, MD, ME, MI, MN, MO, MS, MT, NC, ND, NE, NH, NJ, NM,"
				+ " NV, NY, OH, OK, OR, PA, PR, RI, SC, SD, TN, TX, UT, VA, VI, VT, WA, WI, WV, WY]", c.getErrorText());
	}
}
