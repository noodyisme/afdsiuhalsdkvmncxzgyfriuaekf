package com.capitalone.identity.identitybuilder.policycore.service.exception;

/**
 * This is a custom exception class to track any issue with request attribute validations of MasterBuilder.
 * @author nro567
 *
 */
public class AttributeValidationException extends Exception {

	private static final long serialVersionUID = -1336573481963499268L;
	
	public AttributeValidationException(String message) {
		super(message);
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