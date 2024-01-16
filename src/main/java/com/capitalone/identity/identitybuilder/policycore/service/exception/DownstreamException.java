package com.capitalone.identity.identitybuilder.policycore.service.exception;

import java.util.Map;

/**
 * Base exception class for errors returned from a downstream API.
 *
 * @author oqu271
 */
public abstract class DownstreamException extends RuntimeException {
    private static final long serialVersionUID = 4497147721748295078L;

    public abstract Map<String, Object> getBody();


}

/*
 * Copyright 2020 Capital One Financial Corporation All Rights Reserved.
 *
 * This software contains valuable trade secrets and proprietary information of
 * Capital One and is protected by law. It may not be copied or distributed in
 * any form or medium, disclosed to third parties, reverse engineered or used in
 * any manner without prior written authorization from Capital One.
 */