
package com.capitalone.identity.identitybuilder.policycore.schema;

import com.capitalone.chassis.engine.model.exception.ChassisBusinessException;
import com.capitalone.chassis.engine.model.exception.NotFoundException;
import com.capitalone.identity.identitybuilder.policycore.camel.PolicyConstants;
import com.fasterxml.jackson.databind.JsonNode;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.caffeine.lrucache.CaffeineLRUCache;
import org.apache.camel.support.DefaultExchange;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * Manages input schemas for policy routes.
 * <p>
 * For a given policy step route name, this manager will:
 * <ol>
 * <li>Invoke the corresponding schema route to get the JSON Schema</li>
 * <li>Generate a JSON Schema validator</li>
 * <li>Cache these values for future lookups</li>
 * </ol>
 * <p>
 * To validate:
 * 
 * <pre>
 * Set&lt;ValidationMessage&gt; msgs = SchemaManager.validate(name, node)
 * </pre>
 * 
 * The set returned contains error messages or is empty if validation succeeded.
 * <p>
 * This code uses the
 * <a href="https://github.com/networknt/json-schema-validator"> com.networknt
 * JSON Schema Validator</a>
 * 
 * @author oqu271
 */
@Component
public class SchemaManager {
	@Inject
	private CamelContext context;

	/**
	 * A factory for generating JSON Schema verifiers for the latest schema level.
	 */
	private JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V201909);

	/** A size-limited LRU cache of loaded schemas and schema validators. */
	private Map<String, Entry> cacheMap = Collections.synchronizedMap(new CaffeineLRUCache<>(100));

	/**
	 * Required default constructor for bean creation.
	 */
	public SchemaManager() {
	}

	/**
	 * Package level constructor for unit tests without injection.
	 * 
	 * @param context the Camel context for this manager instance
	 */
	SchemaManager(CamelContext context) {
		this.context = context;
	}

	/**
	 * Validate a request against a policy schema.
	 * <p>
	 * This is a convenience method that implements
	 * 
	 * <pre>
	 * SchemaManager.getEntry(name).getSchema().validate(node)
	 * </pre>
	 * 
	 * @param name the name of the route (no component prefix)
	 * @param node the root of the request tree to be validated
	 * @return validation error messages or an empty <i>Set</i> if successful
	 */
	public Set<ValidationMessage> validate(String name, JsonNode node) {
		return getEntry(name).getSchema().validate(node);
	}

	/**
	 * Clears the schema cache; used by CustomFileWatcherReloadStrategy
	 */
	public void purgeCache() {
		cacheMap.clear();
	}

	/**
	 * Return the schema for a specific policy route as a string. This can be used
	 * to make policies self-describing at runtime.
	 * <p>
	 * This method manages a cache of schemas to improve performance.
	 * 
	 * @param name the name of the route (no component prefix)
	 * @return the policy step schema definition, or <code>null</code> if none is
	 *         found
	 * @throws NotFoundException        if there is no schema for the name
	 * @throws ChassisBusinessException if the schema route throws an exception
	 */
	public Entry getEntry(String name) {
		return cacheMap.computeIfAbsent(name, this::makeEntry);
	}

	/**
	 * Make a new cache entry by invoking the schema route and parsing the schema it
	 * returns.
	 * 
	 * @param name The route name to invoke ("-schema" will be appended)
	 * @return the new entry
	 * @throws NotFoundException        if there is no schema for the name
	 * @throws ChassisBusinessException if the schema route throws an exception
	 */
	private Entry makeEntry(String name) {
		// Avoid a long timeout on a route that doesn't exist.
		String endpoint = "direct:" + name + "-schema";
		if (context.hasEndpoint(endpoint) == null) {
			throw new NotFoundException("Missing policy schema for \"" + name + "\"");
		}

		Exchange exchange = new DefaultExchange(context);

		int lastOccuranceOfUnderscore = name.lastIndexOf('_');
		exchange.getIn().setHeader(PolicyConstants.HEADER_POLICYNAME, name.substring(0, lastOccuranceOfUnderscore));
		exchange.getIn().setHeader(PolicyConstants.HEADER_POLICYVERSION, name.substring(lastOccuranceOfUnderscore + 1));

		// Invoke the schema Camel route endpoint.
		try (ProducerTemplate template = context.createProducerTemplate()){
			template.send(endpoint, exchange);
		} catch (IOException e) {
			throw new IllegalStateException("Unable to create ProducerTemplate", e);
		}

		if (exchange.getException() != null) {
			Exception e = exchange.getException();
			if (!(e instanceof ChassisBusinessException)) {
				e = new ChassisBusinessException(e);
			}
			throw (ChassisBusinessException) e;
		}

		String source = exchange.getIn().getBody(String.class);
		JsonSchema schema = factory.getSchema(source);

		return new Entry(name, source, schema);
	}

	/**
	 * Defines a schema entry.
	 * 
	 * @author oqu271
	 */
	public static class Entry {
		private String name;
		private String source;
		private JsonSchema schema;

		public Entry(String name, String source, JsonSchema schema) {
			this.name = name;
			this.source = source;
			this.schema = schema;
		}

		/**
		 * @return the route name
		 */
		public String getName() {
			return name;
		}

		/**
		 * @return the JSON Schema source
		 */
		public String getSource() {
			return source;
		}

		/**
		 * @return the JSON Schema object that can be used for validation
		 */
		public JsonSchema getSchema() {
			return schema;
		}
	}
}

/*
 * Copyright 2020 Capital One Financial Corporation All Rights Reserved.
 *
 * This software contains valuable trade secrets and proprietary information of
 * Capital One and is protected by law. It may not be copied or distributed in
 * any form or medium, disclosed to third parties, reverse engineered or used in
 * any manner without prior written authorization from Capital One.
 */