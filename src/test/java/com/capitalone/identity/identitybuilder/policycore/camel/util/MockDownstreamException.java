package com.capitalone.identity.identitybuilder.policycore.camel.util;

import com.capitalone.identity.identitybuilder.policycore.service.exception.DownstreamException;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Map;


@Getter
@AllArgsConstructor
public class MockDownstreamException extends DownstreamException {

    Map<String, Object> body;

    @Override
    public Map<String, Object> getBody() {

        return body;
    }
}
