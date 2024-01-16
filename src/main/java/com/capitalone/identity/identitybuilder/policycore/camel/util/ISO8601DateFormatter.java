package com.capitalone.identity.identitybuilder.policycore.camel.util;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.camel.Consume;
import org.apache.camel.Exchange;
import org.apache.camel.Header;
import org.springframework.stereotype.Component;

import com.newrelic.api.agent.Trace;

/**
 * A bean for generating ISO 8601 date-time formats. 
 * 
 * @author oqu271
 */
@Component
public class ISO8601DateFormatter {
	
	/**
	 * Return the current time in ISO 8601 format with a delta adjustment (in milliseconds).
	 * 
	 * @param delta the number of milliseconds to add or subtract from the current time
	 * @return the current time (adjusted by adding <code>delta</code>) in ISO 8601 format
	 */
	public String deltaMilliseconds(long delta) {
		return Instant.now().truncatedTo(ChronoUnit.MILLIS).plus(delta, ChronoUnit.MILLIS).toString();
	}
	
	@SuppressWarnings("squid:S4784")
	private static final Pattern pattern = Pattern.compile("\\s*(\\S+)\\s+([+-]?P\\S+)\\s*");
	
	/**
	 * Return the current time in ISO 8601 format with a delta adjustment (in milliseconds).
	 * <p>
	 * The list of directives is passed in the <code>arg1</code> message header and is
	 * in the format:
	 * <pre>headerName value [, headerName value]</pre>
	 * where headerName is the message header the date should be stored in, and the value is
	 * the desired delta from the current time using
	 * {@link java.time.Duration#parse(CharSequence) ISO-8601 duration format}.
	 * <p>
	 * The following values are equivalent:
	 * <ul>
	 * <li>P1D (days)</li>
	 * <li>PT24H (hours)</li>
	 * <li>PT1440M (minutes)</li>
	 * <li>PT86400S (seconds)</li>
	 * </ul>
	 * <table border="1" cellpadding="5">
	 * <caption>Examples</caption>
	 * <tr><th>Value</th><th>Meaning</th></tr>
	 * <tr><td><b>PT-2H</B></td><td>2 hours in the past</td></th>
	 * <tr><td><b>P1DT12H</B></td><td>36 hours in the future</td></th>
	 * <tr><td><b>PT-6H+3M</B></td><td>5 hours and 57 minutes in the past</td></th>
	 * <tr><td><b>PT0S</B></td><td>now</td></th>
	 * </table>
	 * <p>
	 * 
	 * @param exchange the exchange to set the headers on
	 * @param arg1 the header value containing the list of directives
	 * @return the current date and time (adjusted by adding <code>delta</code>) in ISO 8601 date format
	 * @see   java.time.Duration#parse(CharSequence)
	 */
	@Consume(uri = "direct:ISO8601DatesWithDelta")
	@Trace
	public void dateWithDelta(Exchange exchange, @Header("arg1") String arg1) {
		String[] pairs = arg1.split(",");
		for (String pair : pairs) {
			
			// Validate the syntax and generate groups.
			Matcher m = pattern.matcher(pair);
			if (!m.matches()) {
				throw new IllegalArgumentException(String.format("Invalid date directive \"%s\"", pair));
			}
			
			long delta = Duration.parse(m.group(2)).toMillis();
			String date = Instant.now().truncatedTo(ChronoUnit.MILLIS).plusMillis(delta).toString();
			exchange.getIn().setHeader(m.group(1), date);
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
