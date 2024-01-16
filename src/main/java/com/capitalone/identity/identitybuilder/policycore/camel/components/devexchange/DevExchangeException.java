package com.capitalone.identity.identitybuilder.policycore.camel.components.devexchange;

import com.capitalone.identity.identitybuilder.policycore.model.DevExchangeRequest;
import com.capitalone.identity.identitybuilder.policycore.model.DevExchangeResponse;
import com.capitalone.identity.identitybuilder.policycore.service.exception.DownstreamException;
import lombok.Getter;

import java.util.Map;

/**
 * Represents an error returned from a downstream API invoked through DevExchange.
 *
 * @author oqu271
 */
public class DevExchangeException extends DownstreamException {
    private static final long serialVersionUID = -432258262512444723L;

    private final transient @Getter
    DevExchangeRequest request;
    private final transient @Getter
    DevExchangeResponse response;

    public DevExchangeException(DevExchangeRequest request, DevExchangeResponse response) {
        this.request = request;
        this.response = response;
    }

    @Override
    public String toString() {
        return String.format("Downstream API \"%s\" returned %d: body=\"%s\"",
                request.getServiceName(), response.getHttpStatus(), response.getDxResponse());
    }

    @Override
    public Map<String, Object> getBody() {
        return null;
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