package com.capitalone.identity.identitybuilder.policycore.camel.util;

import org.apache.camel.Exchange;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.support.DefaultExchange;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ISO8601DateFormatterTest {

	private ISO8601DateFormatter formatter = new ISO8601DateFormatter();
	private Exchange exchange;
	private String now;
	private Instant nowInstant;
	
	@BeforeEach
	public void setup() {
		exchange = new DefaultExchange(new DefaultCamelContext());
		now = formatter.deltaMilliseconds(0);
		nowInstant = Instant.parse(now).truncatedTo(ChronoUnit.MILLIS);
	}
	
	@Test
	public void testDeltaMilliseconds() {
		String deltaDate = formatter.deltaMilliseconds(10*60*1000);
		Instant deltaInstant = Instant.parse(deltaDate);
		Duration diff = Duration.between(nowInstant, deltaInstant);
		// This will fail if the timestamps are more than a second apart.
		assertEquals(10*60, diff.getSeconds());
	}
	
	@Test
	public void testRouteDeltaMissingHeaderName() {
		assertThrows(IllegalArgumentException.class, () -> formatter.dateWithDelta(exchange, "PT20M"));
	}
	
	@Test
	public void testRouteDeltaMissingValue() {
		assertThrows(IllegalArgumentException.class, () -> formatter.dateWithDelta(exchange, "foo"));
	}
		
	@Test
	public void testRouteDeltaMissingPrefix() {
		assertThrows(IllegalArgumentException.class, () -> formatter.dateWithDelta(exchange, "foo "));
	}
		
	@Test
	public void testRouteDeltaInvalidSuffix() {
		assertThrows(DateTimeParseException.class, () -> formatter.dateWithDelta(exchange, "foo P20x"));
	}
	
	@Test
	public void testRouteDeltaSimple() {
		formatter.dateWithDelta(exchange, "foo P2D");
		testRouteResult("foo", 2*24*60*60);
	}

	@Test
	public void testRouteDeltaComplex() {
		formatter.dateWithDelta(exchange, "foo PT3H-5M+6S");
		testRouteResult("foo", 3*60*60 - 5*60 + 6);
	}
		
	@Test
	public void testRouteDeltaLeadingSign() {
		formatter.dateWithDelta(exchange, "foo +PT40S");
		testRouteResult("foo", 40);
		formatter.dateWithDelta(exchange, "foo -PT40S");
		testRouteResult("foo", -40);
	}
		
	@Test
	public void testRouteMultipleArguments() {
		formatter.dateWithDelta(exchange, "foo PT3M, bar PT2H");
		testRouteResult("foo", 3*60);
		testRouteResult("bar", 2*60*60);
	}
	
	@Test
	public void testRouteZero() {
		formatter.dateWithDelta(exchange, "foo PT0M");
		testRouteResult("foo", 0);
		formatter.dateWithDelta(exchange, "foo PT0S");
		testRouteResult("foo", 0);
	}
	
	/**
	 * A common method that invokes the route method that extracts a date from
	 * an exchange message header and ensures it is offset by the correct number
	 * of seconds.
	 * <p>
	 * The comparison is intentionally fuzzy at the seconds level to account
	 * for processing time and to not fail if stepping through the code quickly.
	 * 
	 * @param headerName the name of the header containing the ISO8601 date to check
	 * @param expectedSeconds the expected time delta in seconds
	 */
	private void testRouteResult(String headerName, long expectedSeconds) {
		String deltaDate = exchange.getIn().getHeader(headerName, String.class);
		Instant deltaDateInstant = Instant.parse(deltaDate);
		Duration diff = Duration.between(nowInstant, deltaDateInstant);
		// This will fail if the timestamps are more than a second apart.
		assertEquals(expectedSeconds, diff.getSeconds());
	}

}
