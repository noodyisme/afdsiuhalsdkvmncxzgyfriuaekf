package com.capitalone.identity.identitybuilder.policycore.model;

import org.junit.jupiter.api.Test;
import org.meanbean.lang.Factory;
import org.meanbean.test.BeanTester;
import org.meanbean.test.Configuration;
import org.meanbean.test.ConfigurationBuilder;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;

@SpringBootTest(classes = DevExchangeResponse.class)
public class DevExchangeResponseTest {
	
    private static class HttpHeadersFactory implements Factory<HttpHeaders> {
        @Override
        public HttpHeaders create() {
            return new HttpHeaders();
        }
    }
    
    // The tester needs to know how to make a HttpHeaders.
    private Configuration configuration = new ConfigurationBuilder().overrideFactory("httpHeaders", new HttpHeadersFactory()).build();

    @Test
    public void testSettersAndGetters() {
        new BeanTester().testBean(DevExchangeResponse.class, configuration);
    }
}
