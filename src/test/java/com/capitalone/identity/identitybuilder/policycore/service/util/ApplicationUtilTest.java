package com.capitalone.identity.identitybuilder.policycore.service.util;

import com.capitalone.chassis.engine.model.exception.ChassisSystemException;
import com.capitalone.identity.identitybuilder.policycore.model.PolicyState;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;


@ExtendWith(MockitoExtension.class)
public class ApplicationUtilTest {

	@InjectMocks
	ApplicationUtil applicationUtil;
	
	@Test
	public void convertObjectToString() {
		Map<String, Object> testDataMap = new HashMap<>();
		testDataMap.put("key", "value");
		String string = applicationUtil.convertObjectToString(testDataMap);
		assertEquals("{\"key\":\"value\"}", string);
	}

	@Test
	public void convertObjectToStringFailure() {
		assertThrows(ChassisSystemException.class, () -> applicationUtil.convertObjectToString(new Object()));
	}

	@Test
	public void convertStringToPolicyState() {
		PolicyState policyState = new PolicyState();
		String policyName = "redis";
		policyState.setPolicyName(policyName);

		String policyVersion = "1.0";
		policyState.setPolicyVersion(policyVersion);

		Map<String, Serializable> policyStateMap = new HashMap<>();
		policyStateMap.put("firstName", "john");
		policyState.setPolicyState(policyStateMap);

		policyState.getAvailableNextSteps().add("step2");
		policyState.getAvailableNextSteps().add("back");

		policyState.getStepsCompleted().add("start");

		String s = "{\"policyName\":\"" + policyName + "\",\"policyVersion\":\"" + policyVersion + "\",\"policyState\":{\"firstName\":\"john\"},\"availableNextSteps\":[\"step2\",\"back\"],\"stepsCompleted\":[\"start\"]}";
		PolicyState actualPolicyState = applicationUtil.convertStringToPolicyState(s);

		assertEquals(policyState, actualPolicyState);
	}
	
	@Test
	public void convertStringToObjectFailure() {
		assertThrows(ChassisSystemException.class, () -> applicationUtil.convertStringToPolicyState("foo"));
	}

	@Test
	public void testExtractSorId() {
		int sorId = applicationUtil.extractSorId("accountId=123456789~~sorId=5");
		assertEquals(5, sorId);
	}

	@Test
	public void testExtractSorIdEmptyDecryptedRefId() {
		assertThrows(ChassisSystemException.class,
				() -> applicationUtil.extractSorId(""));
	}

	@Test
	public void testExtractSorIdInvalidDecryptedRefId() {
		assertThrows(ChassisSystemException.class,
				() -> applicationUtil.extractSorId("abc"));
	}

	@Test
	public void testExtractSorIdInvalidDecryptedSorId() {
		assertThrows(ChassisSystemException.class,
				() -> applicationUtil.extractSorId("accountId=123456789~~sorId=abc"));
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
