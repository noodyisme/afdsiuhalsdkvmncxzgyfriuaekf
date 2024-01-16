package com.capitalone.identity.identitybuilder.policycore.camel.util;

import com.capitalone.chassis.engine.model.exception.ChassisSystemException;
import com.capitalone.chassis.engine.model.exception.RequestValidationException;
import com.capitalone.identity.identitybuilder.policycore.model.ExecutePolicyRequest;
import com.capitalone.identity.identitybuilder.policycore.service.constants.ApplicationConstants;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.newrelic.api.agent.Trace;
import org.apache.camel.Consume;
import org.apache.camel.Exchange;
import org.apache.camel.Header;
import org.apache.camel.component.caffeine.lrucache.CaffeineLRUCache;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.LineNumberReader;
import java.io.StringReader;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A utility route that processes groups of request parameters and sets
 * message headers accordingly. 
 * 
 * @author oqu271
 */
@Component
public class HeaderUtil {

	protected final Logger logger = LogManager.getLogger(getClass());
	
	public static final String ARG1_HEADER = "arg1";
	private static final String COMMENT1 = "//";
	private static final String COMMENT2 = "#";
	private static final String CONTINUATION = "\\";
	
	private static Map<String, List<HeaderEntry>> compiledDirectives = Collections.synchronizedMap(new CaffeineLRUCache<>(100));
	
	@SuppressWarnings("squid:S4784")
	private static final Pattern RIGHT_HAND_PATTERN = Pattern.compile("(\\S*)\\s*(body|hdr)\\s*'([^']*)'");

	public enum Action { MAP, MAPA, VALUE, ATTRPAIRS, JSON }
 
	public enum Source { BODY, HDR }

	private ObjectMapper mapper = new ObjectMapper();
	
	/**
	 * A Camel route that processes a set of directives to assign values to headers
	 * in the current Camel message.
	 * <p>
	 * This is used primarily to process request parameters passed to policies
	 * in the message body, but can be used for other header manipulation including
	 * merging values from different headers into one.
	 *
	 * <h2>Header Directives</h2>
	 * On input the <code><b>arg1</b></code> header specifies a list of one or more
	 * header directives. Each directive is of the general form:
	 * <pre>    headerPath action source 'parameter-list' [source 'parameter-list']...</pre>
	 * <p>
	 * Directives are separated by a newline but a trailing backslash character ("\")
	 * will cause the next line to be treated as a continuation of the current line.
	 * This allows for a more readable syntax by allowing source/parameter-list pairs
	 * to appear on their own lines.
	 * <pre>    headerPath action source 'parameter-list' \
	 *                      source 'parameter-list'</pre>
	 *  
	 * <h2>Directive Processing</h2>
	 * When a set of directives is supplied, it is parsed into objects and added to a
	 * bounded LRU cache to avoid having to reparse them frequently.
	 * <p>
	 * The directives are then applied to the current header set using the following
	 * order of processing:
	 * <ol>
	 * <li>The <code>source</code> and <code>parameter-list</code> pairs are processed in (first-to-last) order to build up a single map</li>
	 * <li>The <code>action</code> can return the map, extract the first value, or do other transformations</li>
	 * <li>The <code>headerPath</code> determines which header gets the result</li>
	 * </ol>
	 * 
	 * <h2>Sources and Parameter Lists</h2>
	 * The <code>source</code> keyword defines where the data is extracted from. Different sources require
	 * differently-formatted parameter lists.
	 * <p>
	 * <table border="1" cellpadding="3">
	 * <tr><th>Source</th><th>Where the data comes from</th><th>parameter-list</th></tr>
	 * <tr><td>body</td>
	 *     <td>Values from the <code>Map</code> in the message body</td>
	 *     <td>annotated map key names</td></tr>
	 * <tr><td>hdr</td>
	 *     <td>Current header values for the message</pre></td>
	 *     <td>annotated header names</td></tr>
	 * </table>
	 * </dd>
	 * </li>
	 * <dl>
	 * You can use multiple source and parameter-list pairs in a single directive.
	 * This allows you to merge body values and other header values into a single header.
	 * When combined with the line-continuation character (see above), it also lets you break up long
	 * lists of <code>body</code> or <code>hdr</code> parameters into multiple groups for better readability.
	 * <p>
	 * The values from the source(s) are always built into a <i>Map</i> internally before the <code>action</code> is invoked.
	 * See {@link com.capitalone.identity.identitybuilder.policycore.camel.util.RequestParameter RequestParameter}
	 * for the annotation syntax used in the parameter lists.
	 * 
	 * <h2>Actions</h2>
	 * <p>
	 * <table border="1" cellpadding="3">
	 * <tr><th>Action</th><th>What gets assigned to the headerPath</th></tr>
	 * <tr><td>map</td><td>The map created from the source(s)</td></tr>
	 * <tr><td>mapa</td><td>The contents of the map created from the source(s) are added to the map
	 *          in the target header.
	 * 			If the <code>headerPath</code> is not a map then behaves the same as <code>map</code></td></tr>
	 * <tr><td>value</td><td>The first value in the map (only one is allowed)</td></tr>
	 * <tr><td>attrpairs</td><td>A JSON string declaring an array (list) of attribute key/value
	 *         pairs like this.  All attribute values are converted to strings.<pre>
	 *  [ { attributeName="foo", attributeValue="bar" },
	 *  { attributeName="count", attributeValue="1" }]</pre></td></tr>
	 *  <tr><td>json</td><td>The map is converted into JSON string</td></tr>
	 * </table>
	 * </dd>
	 * 
	 * <h2>Header Path</h2>
	 * The name of the header to assign values to. This may be a simple name like "foo"
	 * or a <i>path</i> describing a nested structure of maps (e.g., "foo.bar.xyz").
	 * <p>
	 * If the header path has multiple parts, any missing maps along the path are created as needed.
	 * If one of the keys in the path resolves to something other than a map than an exception is thrown.
	 * <p>
	 * Some examples:
	 * <table border="1" cellpadding="3">
	 * <tr><th>Header Path</th><th>Where the Action Result is Set</th></tr>
	 * <tr><td>foo</td><td>The "foo" message header</td></tr>
	 * <tr><td>foo.bar.xyz</td><td>The "foo" message header is a map with a "bar" key,
	 *         which is a map with an "xyz" key that maps to the result.
	 *         The "foo" and "bar" maps are created if needed.
	 * <tr><td>policyState.personalDetails.taxId</td><td>The standard "taxId" field in the policy state</td></tr>
	 * </table>
	 * 
	 * <h2>Additional Notes</h2>
	 * <ul>
	 * <li>Blank lines are ignored</li>
	 * <li>Lines that begin with a "#" or "//" are ignored as comments</li>
	 * <li>Lines can be continued using a trailing \ character</li>
	 * <li>Most annotations require that the parameter value is a <code>String</code> and will throw an error if
	 *     used to evaluate anything else (like a <code><i>Map</i></code>)
	 * </ul>
	 * 
	 * <h2>Examples</h2>
	 * <pre>
	 * &lt;setHeader headerName="arg1"&gt;
	 * 	&lt;constant&gt;
	 * 		barcode    value     body  'barcode'
	 * 		device     map       body  'deviceId, deviceType, locationsyntax=stateCode:type=uppercase}' \
	 * 		                     body  'taxId'
	 * 		originator value     body  'originator{syntax=wmtOriginator}'
	 * 		deviceJson attrpairs hdr 'device'
	 * 	&lt;/constant&gt;
	 * &lt;/setHeader&gt;
	 * &lt;to uri="headerUtil"/&gt;
	 * </pre>
	 * This next example saves an input parameter in the policy state header.
	 * <pre>
	 * &lt;setHeader headerName="arg1"&gt;
	 * 	&lt;constant&gt;
	 * 		policyState.personalDetails.taxId value body 'taxId{syntax=taxId}'
	 * 	&lt;/constant&gt;
	 * &lt;/setHeader&gt;
	 * &lt;to uri="direct:headerUtil"/&gt;
	 * </pre>
	 * <p>
	 * See {@link com.capitalone.identity.identitybuilder.policycore.camel.util.RequestParameter RequestParameter}
	 * for the annotation syntax used in the parameter list.
	 * 
	 * @param  exchange an exchange with the {@link ExecutePolicyRequest} message
	 * @param  directives the header value specifying the processing directives
	 * @throws IllegalArgumentException if a required value is missing or a value has an invalid syntax
	 * @see {@link com.capitalone.identity.identitybuilder.policycore.camel.util.RequestParameter RequestParameter}
	 *      for the annotation syntax for <code>names</code>
	 */
	@Consume(uri = "direct:headerUtil")
	@Trace
	public void process(Exchange exchange, @Header(ARG1_HEADER) String directives) {
		// If there is no directive, it's an error.
		if (directives == null) {
			throw new IllegalArgumentException("This route requres the " + ARG1_HEADER + " header be set to define header processing");
		}

		// If we've seen this argument string before we don't have to parse and compile it again.
		List<HeaderEntry> hds = compiledDirectives.get(directives);
		if (hds == null) {
			// There is a race condition that can result in redundant compilation but it is harmless.
			hds = compileDirectives(directives);
			compiledDirectives.put(directives, hds);
		}
		
		// Process each set of header directives.
		for (HeaderEntry hd: hds) {
			for (Directive d : hd.getDirectives()) {
				processDirective(exchange, hd, d);
			}
		}
	}
	
	/**
	 * Process a single header directive (implemented as a separate method to please SonarQube).
	 * 
	 * @param exchange the exchange to modify
	 * @param hd the header entry
	 * @param d the directive to process
	 */
	@SuppressWarnings("unchecked")
	private void processDirective(Exchange exchange, HeaderEntry hd, Directive d) {
		// Invoke the proper processor according to the source type.
		Map<String, Object> map;
		switch (d.getSource()) {
		case BODY:
			map = extract(new PathMap(exchange.getIn().getBody(Map.class)),
					((ParameterDirective) d).getParams());
			break;
		case HDR:
			map = extract(new PathMap(exchange.getIn().getHeaders()),
					((ParameterDirective) d).getParams());
			break;
		default:
			map = Collections.emptyMap(); // Shouldn't get here, as all enums should be covered
		}

		// Convert the created map based on the action.
		Object value;
		switch (hd.getAction()) {
		case MAP:
			value = map;
			break;
		case JSON:
			try {
				value = mapper.writeValueAsString(map);
			} catch (JsonProcessingException jpe) {
				throw new ChassisSystemException(jpe.getMessage(), jpe);
			}
			break;
		case VALUE:
			value = map.isEmpty() ? null : map.values().iterator().next();
			break;
		case ATTRPAIRS:
			value = convertToNameValue(map);
			break;
		case MAPA:
			// MAPA (map add) is a special case because if the header path points to an existing map
			// we want to add the new map elements to it, not replace it.
			Object targetMap = new PathMap(exchange.getIn().getHeaders()).get(hd.getHeaderPath());
			if (targetMap instanceof Map) {
				((Map<String, Object>) targetMap).putAll(map);
				return;
			}
			value = map;
			break;
		default:
			value = null; // shouldn't get here, as all enums should be covered
		}

		// Set the value in the designated header, creating parent maps as needed.
		setHeaderByPath(exchange.getIn().getHeaders(), hd.getHeaderPath(), hd.getHeaderPath(), value);
	}
	
	/**
	 * Converts a first-level map into an JSON array where each
	 * key and value are grouped into their own maps as separate entries.
	 * <p>
	 * So an input map of
	 * <pre>{ foo="bar", count=1 }</pre>
	 * will result in an output list of
	 * <pre>[ { attributeName="foo", attributeValue="bar" },
	 *  { attributeName="count", attributeValue="1" } ]</pre>, expressed as
	 * a string.
	 * 
	 * @param  input the input map to convert
	 * @return a JSON String containing an array of attributeNames and attributeValues pairs
	 */
	private String convertToNameValue(Map<String, Object> input) {
		// Build the list of maps, one for each name/value pair.
		List<Map<String, Object>> list = new ArrayList<>(input.size());
	    for (Map.Entry<String, Object> entry : input.entrySet()) {
	        Map<String, Object> map = new TreeMap<>();
	        map.put(ApplicationConstants.ATTRIBUTE_NAME, entry.getKey());
	        map.put(ApplicationConstants.ATTRIBUTE_VALUE, entry.getValue().toString());
	        list.add(map);
	    }
	    
	    // Convert it to a JSON string before returning.
		try {
			return mapper.writeValueAsString(list);
		} catch (JsonProcessingException e) {
			// This shouldn't happen but we have to cover all the bases.
			throw new ChassisSystemException(e.getMessage(), e);
		}
	}
	
	/**
	 * Recursively search a tree of header <code>Map</code>s to store a value in the map.
	 * 
	 * @param map the current parent map (starts with the message header map)
	 * @param originalPath the original header name path (for error messages)
	 * @param path the remaining header name path (in dotted notation)
	 * @param value the value to store in the target header named by <code>path</code>
	 */
	@SuppressWarnings("unchecked")
	private void setHeaderByPath(Map<String, Object> map, String originalPath, String path, Object value) {
		String[] names = path.split("\\.", 2);
		if (names.length == 1) {
			// We've navigated all the parent nodes now so store the value in the current map.
			Object oldValue = map.get(names[0]);
			if (oldValue instanceof Map && value instanceof Map) {
				((Map<String, Object>) oldValue).putAll((Map<String, Object>) value);
			} else {
				map.put(names[0], value);
			}
		} else {
			Object node = map.get(names[0]);
			if (node == null) {
				node = new TreeMap<String, Object>();
				map.put(names[0], node);				
			}
			if (!(node instanceof Map)) {
				throw new IllegalArgumentException(String.format("Cannot set policy header %s due to conflict with path component %s", originalPath, names[0]));
			}
			setHeaderByPath((Map<String, Object>) node, originalPath, names[1], value);
		}
	}
	
	/**
	 * Extract specific <i>body</i> parameters to a map, allowing
	 * type conversion and enforcement of required parameters and syntax.
	 * <p>
	 * See {@link com.capitalone.identity.identitybuilder.policycore.camel.util.RequestParameter RequestParameter}
	 * for the annotation syntax.
	 * 
	 * @param from the map containing the parameters to process
	 * @param params the parameters to extract (may be annotated)
	 * @throws RequestValidationException if a required value is missing or a value has an invalid syntax
	 * @see {@link com.capitalone.identity.identitybuilder.policycore.camel.util.RequestParameter RequestParameter}
	 *      for the annotation syntax for <code>names</code>
	 */
	@Trace
	@SuppressWarnings("unchecked")
	private Map<String, Object> extract(PathMap from, RequestParameter[] params) {
		Map<String, Object> to = new PathMap(new TreeMap<>()).filter(PathMap.SPARSE_FILTER);
		for (RequestParameter param : params) {
			// Values are extracted from the header using path resolution (e.g., a.b.c)
			// We first need to see if we are flattening a map.
			String name = param.getName();
			boolean flattenMap = name.endsWith(".*");
			if (flattenMap) {
				name = name.substring(0, name.length() - 2);
			}
			
			// Get the value (String, int, float, boolean, or Map<String, Object>.
			Object value = from.get(name);
			
			// Figure out what the new entry will be named (rename support0.
			String lastPathName = PathMap.pathToList(name).getLast();
			String computedName = (param.getNewName() != null) ? param.getNewName() : lastPathName;
			
			// Flattened maps cannot be renamed because they are expanded and may have multiple keys.
			if (value instanceof Map && flattenMap && param.getNewName() != null) {
				throw new IllegalArgumentException(String.format("Value for param %s is a Map which cannot be renamed as it is flattened", param.getDeclaration()));						
			}
			
			// Use the parameter directive to possibly modify the value.
			value = param.toValue(value);
			
			// Accumulate values into the target map, expanding map values.
			if (value instanceof Map && flattenMap) {
				to.putAll((Map<String, Object>) value);
			} else {
				to.put(computedName, value);
			}
		}
		return to;
	}
	
	/**
	 * Parses and compiles a set of header directives for repeated use.
	 * 
	 * @param  text the header directives
	 * @return the compiled directives
	 * @throws IllegalArgumentException if the text is of an invalid format
	 */
	private List<HeaderEntry> compileDirectives(String text) {
		List<HeaderEntry> list = new ArrayList<>();

		try (LineNumberReader reader = new LineNumberReader(new StringReader(text))) {
			String line;
			while ((line = reader.readLine()) != null) {
				line = line.trim();
				if (line.isEmpty() || line.startsWith(COMMENT1) || line.startsWith(COMMENT2)) {
					continue;
				}

				int startingLine = reader.getLineNumber();
				// Continued lines are merged together before parsing.
				while (line.endsWith(CONTINUATION)) {
					// Get the next line (or EOF) and append it.
					String nextLine = reader.readLine();
					if (nextLine == null) {
						break;
					}
					line = line.substring(0, line.length() - 2) + " " + nextLine.trim();
				}

				// Get the header, operator, and any directives.
				String[] parts = line.split("\\s+", 3);
				if (parts.length != 3) {
					throw new IllegalArgumentException(String.format("Invalid directive format at line %d", startingLine));
				}
				String headerName = parts[0];
				Action action = Action.valueOf(parts[1].toUpperCase());

				// Scan for directives and build a list of them.
				List<Directive> dList = buildDirectiveList(action, startingLine, parts[2]);
				
				list.add(new HeaderEntry(headerName, action, dList));
			}
			return list;
		} catch (IOException e) {
			// Should never get here because we can't get an IO error reading a string.
			throw new IllegalArgumentException("Invalid directive", e);
		}
	}
	
	/**
	 * Parses a directive list string into a list of directive objects.
	 * 
	 * @param action the action being taken
	 * @param startingLine the line the directive starts at
	 * @param text the directive list string
	 * @return a list of directive objects parsed from <code>text</code> (may be empty)
	 */
	private List<Directive> buildDirectiveList(Action action, int startingLine, String text) {
		List<Directive> list = new ArrayList<>();
		Matcher m = RIGHT_HAND_PATTERN.matcher(text);
		boolean found = false;
		while (m.find()) {
			// The first capture group makes sure there isn't an extra token
			// before the source.  This could be due to multiple header directives
			// are accidentally placed on the same line.
			if (!m.group(1).isEmpty()) {
				throw new IllegalArgumentException(String.format("Invalid directive source \"%s\" at line %d", m.group(1), startingLine));				
			}
			found = true;
			Source source = Source.valueOf(m.group(2).toUpperCase());
			
			String[] fieldList = TestTypeConverters.toStringArray(m.group(3));
			if (fieldList.length > 1 && action == Action.VALUE) {
				throw new IllegalArgumentException(String.format("Invalid directive field names at line %d: multiple field names for single value operator", startingLine));
			}
			
			list.add(new ParameterDirective(source, RequestParameter.fromArray(fieldList)));
		}
		if (!found) {
			throw new IllegalArgumentException(String.format("Invalid directive source \"%s\" at line %d", text, startingLine));
		}
		return list;
	}
			 
	/**
	 * One or more parameter directives for a single header assignment.
	 * 
	 * @author oqu271
	 */
	private static class HeaderEntry {
		String headerPath;
		Action action;
		List<Directive> directives;
		
		public HeaderEntry(String headerPath, Action action, List<Directive> directives) {
			this.headerPath = headerPath;
			this.action = action;
			this.directives = directives;
		}
		
		public String getHeaderPath() {
			return headerPath;
		}

		public Action getAction() {
			return action;
		}

		public List<Directive> getDirectives() {
			return directives;
		}
	}

	/**
	 * A tagging interface to handle the different types of directives.
	 *
	 * @author oqu271
	 */
	private static interface Directive {
		public Source getSource();
	}
	
	/**
	 * Represents a single compiled parameter directive.
	 * 
	 * @author oqu271
	 */
	private static class ParameterDirective implements Directive {
		Source source;
		RequestParameter[] params;
		
		public ParameterDirective(Source source, RequestParameter[] params) {
			this.source = source;
			this.params = params;
		}
		
		public Source getSource() {
			return source;
		}

		public RequestParameter[] getParams() {
			return params;
		}
		
		@Override
		public String toString() {
			return source.toString().toLowerCase() + " " + Arrays.toString(params);
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