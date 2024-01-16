package com.capitalone.identity.identitybuilder.policycore.service.util;

import com.capitalone.chassis.engine.model.exception.ChassisSystemException;
import com.fasterxml.jackson.databind.ObjectWriter;
import org.bouncycastle.asn1.x509.sigi.PersonalData;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.util.AssertionErrors.assertTrue;


@ExtendWith(MockitoExtension.class)
public class JsonUtilTest {

	String ivhString = "{\"users\":[{\"transactionId\":\"transactionId\",\"userAttributes\":[{\"attributeName\":\"FirstName\",\"attributeValue\":\"VIRGINIA\"},{\"attributeName\":\"LastName\",\"attributeValue\":\"RICHMOND\"},{\"attributeName\":\"MobileNumber\",\"attributeValue\":\"8042213626\"},{\"attributeName\":\"DateOfBirth\",\"attributeValue\":\"19760112\"},{\"attributeName\":\"TaxId\",\"attributeValue\":\"1a2sa3fd4\"},{\"attributeName\":\"AddressLine1\",\"attributeValue\":\"2300 W BROAD ST APT 5B\"},{\"attributeName\":\"City\",\"attributeValue\":\"RICHMOND\"},{\"attributeName\":\"StateCode\",\"attributeValue\":\"VA\"},{\"attributeName\":\"PostalCode\",\"attributeValue\":\"23269-0999\"},{\"attributeName\":\"CountryCode\",\"attributeValue\":\"US\"}],\"sequenceNumber\":1}],\"configId\":\"9862bf54-7951-4077-b9fd-6d311001c1c8\"}";


	String govIdString="{\"customerDetails\":{\"caseId\":\"TEST\"},\"documentData\":{\"address\":{\"address\":\"2300 W BROAD ST  RICHMOND, VA 23269-0999\",\"addressLine1\":\"2300 W BROAD ST\",\"city\":\"RICHMOND\",\"postalCode\":\"23269-0999\",\"state\":\"VA\"},\"biographic\":{\"documentName\":\"Virginia (VA) Driver's License\",\"documentType\":\"Drivers License\",\"expirationDate\":\"2022-10-14\",\"inputDocumentDetails\":{},\"issueDate\":\"2016-03-02\",\"issueStateCode\":\"VA\",\"issueStateName\":\"Virginia\"}},\"documentStatus\":\"Attention\",\"documentSubStatusItems\":[{\"subStatus\":\"Attention\",\"subStatusDescription\":\"The document type could not be determined\"},{\"subStatus\":\"Passed\",\"subStatusDescription\":\"The document has not expired\"},{\"subStatus\":\"Passed\",\"subStatusDescription\":\"No evidence of tampering or digital manipulation was detected.\"},{\"subStatus\":\"Passed\",\"subStatusDescription\":\"The issue date is valid\"},{\"subStatus\":\"Passed\",\"subStatusDescription\":\"Background DMV - A visible pattern was found\"}],\"keepAliveTimestamp\":\"2018-09-12 12:54:26\",\"linkExpiryTimestamp\":\"2018-09-12 12:54:26\",\"numberOfAttempts\":\"1\",\"referenceId\":\"a7dd42e61d9749dc8d5ea03d5f37cc49\",\"referenceIdLocked\":false}";

	@Test
	public void testJsonObjectException(){
		assertThrows(ChassisSystemException.class, () -> JsonUtil.jsonToObject("blah", null));
	}

	@Test
	public void testGetFilteredResponseException(){
		assertDoesNotThrow(() -> JsonUtil.getFilteredResponse("blah", null, "booyahservice"));
	}

	@Test
	public void testArrayFilterCaseInsensitiveWithMatch(){
		String filtered =JsonUtil.getFilteredResponse("{\"attributeName\":\"FirstName\",\"attributeValue\":\"VIRGINIA\"}", "attributeName,attributeValue", "booyahservice");
		assertEquals("{\"attributeName\":\"XXXXXX\",\"attributeValue\":\"XXXXXX\"}", filtered);
	}

	@Test
	public void testArrayFilterCaseInsensitiveWithoutMatch(){
		String filtered =JsonUtil.getFilteredResponse("{\"Attributename\":\"FirstName\",\"Attributevalue\":\"VIRGINIA\"}", "attributeName,attributeValue", "booyahservice");
		assertEquals("{\"Attributename\":\"XXXXXX\",\"Attributevalue\":\"XXXXXX\"}", filtered);
	}

	@Test
	public void testArrayFilter(){

		String filtered =JsonUtil.getFilteredResponse(ivhString, "attributeName,attributeValue", "booyahservice");
		assertTrue("should contain attributeName", filtered.contains("attributeName"));
		assertTrue("should contain attributeValue", filtered.contains("attributeValue"));
	}

	@Test
	public void testGovIdFilter(){

		String filtered =JsonUtil.getFilteredResponse(govIdString, "accountReferenceId,documentData,firstName,surName,middleName,givenName,fullName,gender,documentBirthDate,placeOfBirth,documentNumber,personalNumber,fatherName,motherName,inputBirthDate,inputFullName,address|imageUrls,attributeName,attributeValue,address,customerDetails,expirationDate,issueDate,replacementDate", "booyahservice");
		assertTrue("should contain documentData", filtered.contains("documentData"));
	}

	@Test
	void testImageArrayFilter(){
		String imageString = "{\"images\":[ \"aaa\", \"bbb\"], \"customerDetails\":{ \"images\":[ \"aaa\", \"bbb\"]}}";
		String filtered = JsonUtil.getFilteredResponse(imageString, "images", "booyahservice");
		assertFalse(filtered.contains("aaa") || filtered.contains("bbb"),"no image data allowed");
		assertTrue("images key should remain",filtered.contains("images"));
	}

	@Test
	public void testWriteFor(){
		ObjectWriter objWriter = JsonUtil.getObjectWriter(PersonalData.class);
		assertNotNull(objWriter);
	}

	@ParameterizedTest
	@CsvSource({
			"requestBody,12345,{\"requestBody\":\"XXXXXX\"}",
			"requestBody,null,{\"requestBody\":\"XXXXXX\"}",
			"result,6789,{\"result\":\"XXXXXX\"}",
			"result,null,{\"result\":\"XXXXXX\"}"
	})
	void testOperationalAuditFilter(String key, String value, String filteredString) {
		Map<String, Object> request = new HashMap<>();
		request.put(key, value);
		assertEquals(JsonUtil.getFilteredResponse(request, "requestBody,result"), filteredString);
	}

	@Test
	void testOperationalAuditFilterDeepObjectFilter() {
		Map<String, Object> response = new HashMap<>();
		response.put("result", "{\"pip\":true,\"decision\":true}");
		response.put("policyStatus", "SUCCESS");
		Map<String, Object> policyRequest = new HashMap<>();
		policyRequest.put("policyRequest", response);
		String filteredString = "{\"policyRequest\":{\"result\":\"XXXXXX\",\"policyStatus\":\"SUCCESS\"}}";
		assertEquals(JsonUtil.getFilteredResponse(policyRequest, "requestBody,result"), filteredString);
	}

	@Test
	void testGetFilteredResponseForNullObject() {
		assertNull(JsonUtil.getFilteredResponse(null, "requestBody,result"));
	}

	@Test
	void testGetFilteredResponseForNullFilterField() {
		final String expected = "{\"hello\":\"world\"}";
		final String actual = JsonUtil.getFilteredResponse(new Object() {public final String hello = "world";}, null);
		Assertions.assertEquals(expected, actual);
	}
}
