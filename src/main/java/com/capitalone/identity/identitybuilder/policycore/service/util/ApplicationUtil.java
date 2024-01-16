package com.capitalone.identity.identitybuilder.policycore.service.util;

import com.capitalone.chassis.engine.annotations.logging.Log;
import com.capitalone.chassis.engine.model.exception.ChassisSystemException;
import com.capitalone.identity.identitybuilder.policycore.model.PolicyState;
import com.capitalone.api.model.id.ReferenceId;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import javax.inject.Named;
import java.io.Serializable;
import java.util.Map;

@Log
@Named
public class ApplicationUtil {
	
	protected static final Logger logger = LogManager.getLogger(ApplicationUtil.class);
	private final ObjectMapper objectMapper = new ObjectMapper();

	public String convertObjectToString(Object object) {
		try {
			return objectMapper.writeValueAsString(object);
		} catch (Exception e) {
			logger.error("Error converting the object to string..", e);
			throw new ChassisSystemException("Error converting the object to string", e);
		}
	}
	
	public PolicyState convertStringToPolicyState(String s) {
		try {
			return objectMapper.readValue(s, new TypeReference<PolicyState>() {});
		} catch (Exception e) {
			logger.error("Error converting the string to Map object..", e);
			throw new ChassisSystemException("Error converting the string to Map object", e);
		}
	}

	public Map<String, Serializable> convertObjectToMap(Object o) {
		try {
			return objectMapper.convertValue(o, new TypeReference<Map<String, Serializable>>() {});
		} catch (Exception e) {
			logger.error("Error converting the Object to Map..", e);
			throw new ChassisSystemException("Error converting the Object to Map", e);
		}
	}

	public int extractSorId(String decryptedRefId) {
		try {
			String extractedSorId = ReferenceId.valueOf(decryptedRefId).getField("sorId");
			return Integer.parseInt(extractedSorId);
		} catch (Exception e) {
			logger.error("Error extracting sorId..", e);
			throw new ChassisSystemException("Error extracting sorId..", e);
		}
	}

}
