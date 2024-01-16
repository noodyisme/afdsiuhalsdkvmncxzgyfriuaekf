package com.capitalone.identity.identitybuilder.policycore.service.util;

import com.capitalone.chassis.engine.annotations.logging.Profile;
import com.capitalone.chassis.engine.model.exception.ChassisSystemException;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.jayway.jsonpath.JsonPath;
import com.newrelic.api.agent.Trace;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.*;

/**
 * 
 * @author nro567
 *
 *         Utility to perform operations on JSON.
 */
@Profile
public final class JsonUtil {

	private static List<String> fieldsToRemove;
	private static final ObjectMapper objectMapper = new ObjectMapper();

	static {
		objectMapper.disable(SerializationFeature.INDENT_OUTPUT);
		objectMapper.registerModule(new JavaTimeModule());
		objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
	}

	private static final Logger logger = LogManager.getLogger(JsonUtil.class);

	private JsonUtil() {
	}

	/**
	 * @return the objectMapper
	 */

	public static <T> T jsonToObject(String json, TypeReference<T> typeRef) {
		try {
			return objectMapper.readValue(json, typeRef);
		} catch (Exception e) {
			throw new ChassisSystemException(e.getMessage(), e);
		}
	}

	/**
	 * Returns the ObjectWriter of the given Type.
	 * 
	 * @param type
	 *            Class
	 * @return ObjectWriter
	 */
	public static ObjectWriter getObjectWriter(Class<?> type) {
		return objectMapper.writerFor(type);
	}
	
	/**
	 * Receives JSON string and returns the same after stripping of the elements
	 * mentioned as fieldsToFilter parameter
	 * 
	 * @param jsonData
	 * @param fieldsToFilter
	 * @return
	 */
	@Trace
	public static String getFilteredResponse(String jsonData, String fieldsToFilter, String serviceName) {
		if (StringUtils.isBlank(jsonData)) {
			return jsonData;
		} else {
			JsonNode rootNode = null;
			try {
				rootNode = objectMapper.readTree(jsonData);
			} catch (IOException e) {
				logger.debug("Masterbuilder Backend API : {} response {} is not json", serviceName, jsonData);
				// Suppress the exception no impact because of error in
				// filtering
				return jsonData;
			}
			return convertJsonNodeToMaskedString(fieldsToFilter, rootNode);
		}
	}

	private static String convertJsonNodeToMaskedString(String fieldsToFilter, JsonNode rootNode) {
		if (rootNode != null && StringUtils.isNotBlank(fieldsToFilter)) {
			fieldsToRemove = Arrays.asList(fieldsToFilter.toLowerCase().split("\\s*,\\s*"));
			maskPIIData(rootNode);
			return rootNode.toString();
		} else {
			return null != rootNode ? rootNode.toString() : StringUtils.EMPTY;
		}
	}

	public static String getFilteredResponse(Object jsonData, String fieldsToFilter) {
		if (jsonData == null)
			return null;
		JsonNode rootNode = objectMapper.valueToTree(jsonData);
		return convertJsonNodeToMaskedString(fieldsToFilter, rootNode);
	}

	private static JsonNode maskPIIData(JsonNode node) {

		if (node instanceof ObjectNode) {
			ObjectNode object = (ObjectNode) node;
			Iterator<Map.Entry<String, JsonNode>> fieldsIterator = object.fields();
			while (fieldsIterator.hasNext()) {
				Map.Entry<String, JsonNode> innerField = fieldsIterator.next();
				if(fieldsToRemove.contains(innerField.getKey().toLowerCase())) {
					try {
						innerField.setValue(objectMapper.readTree("\"XXXXXX\""));
					} catch (Exception ex) {
						logger.error("Exception occurred while masking the attributes from Downstream response:{}", ex);
					}
				} else if (innerField.getValue() instanceof ObjectNode || innerField.getValue() instanceof ArrayNode) {
					maskPIIData(innerField.getValue());
				}
			}
		} else if (node instanceof ArrayNode) {
			// Do masking for each element of arraynode too
			ArrayNode arrayNode = (ArrayNode) node;
			for (JsonNode node2 : arrayNode) {
				maskPIIData(node2);
			}
		}
		return node;
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
