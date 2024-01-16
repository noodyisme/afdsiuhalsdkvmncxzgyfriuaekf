package com.capitalone.identity.identitybuilder.policycore.pip.s3;

import org.apache.camel.CamelContext;
import org.apache.camel.Component;
import org.apache.camel.Endpoint;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest
@ContextConfiguration(classes = {PipAwsS3Component.class})
class PipAwsS3ComponentTest {

    @Mock
    private CamelContext camelContext;

    @Mock
    private Component aws2S3Component;

    @Mock
    private Endpoint endpoint;

    @Autowired
    private PipAwsS3Component pipAwsS3Component;

    @Test
    void testCreateEndpoint() throws Exception {
        pipAwsS3Component.setCamelContext(camelContext);
        String uri = "test_uri";
        Map<String, Object> parameters = Map.of();

        when(camelContext.getComponent("aws2-s3")).thenReturn(aws2S3Component);
        when(aws2S3Component.createEndpoint(anyString(), anyMap())).thenReturn(endpoint);

        Endpoint result = pipAwsS3Component.createEndpoint(uri,"remaining", parameters);

        assertNotNull(result);
    }
}
