package com.capitalone.identity.identitybuilder.policycore.service.dao;

import com.capitalone.identity.identitybuilder.policycore.model.DevExchangeRequest;
import com.capitalone.identity.identitybuilder.policycore.model.DevExchangeResponse;

public interface DevExchangeDAO   {
	
	DevExchangeResponse invokeDxAPI (DevExchangeRequest devexchangeRequest);

}


/*
 * Copyright 2018 Capital One Financial Corporation All Rights Reserved.
 * 
 * This software contains valuable trade secrets and proprietary information of
 * Capital One and is protected by law. It may not be copied or distributed in
 * any form or medium, disclosed to third parties, reverse engineered or used in
 * any manner without prior written authorization from Capital One.
 */
