package com.capitalone.identity.identitybuilder.policycore.model;

import org.junit.jupiter.api.Test;
import org.meanbean.lang.EquivalentFactory;
import org.meanbean.lang.Factory;
import org.meanbean.test.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;

import java.net.URI;
import java.net.URISyntaxException;

@SpringBootTest(classes = DevExchangeRequest.class)
public class DevExchangeRequestTest {
	
    private static class HttpHeadersFactory implements Factory<HttpHeaders> {
        @Override
        public HttpHeaders create() {
            return new HttpHeaders();
        }
    }
    
    private static class DevExchangeRequestFactory implements EquivalentFactory<DevExchangeRequest> {
		@Override
		public DevExchangeRequest create() {
			try {
				DevExchangeRequest req = new DevExchangeRequest();
				req.setHttpHeaders(new HttpHeaders());
				req.setHttpMethod(HttpMethod.PUT);
				req.setRequestBody("{ \"foo\": \"bar\" }");
				req.setRequestBodyAudit("{}");
				req.setServiceName("serviceName");
                req.setScheme("scheme");
                req.setPolicyInfo("test_policy_v1");
				req.setUri(new URI("https://foo.bar.com"));
				return req;
			} catch (URISyntaxException e) {
				throw new RuntimeException(e);
			}
		}
    }

    // The tester needs to know how to make a HttpHeaders.
    private Configuration configuration = new ConfigurationBuilder().overrideFactory("httpHeaders", new HttpHeadersFactory()).build();

    @Test
    public void testSettersAndGetters() {
        new BeanTester().testBean(DevExchangeRequest.class, configuration);
    }
    
    @Test
    public void testHashCode() {
    	new HashCodeMethodTester().testHashCodeMethod(new DevExchangeRequestFactory());
    }
    
    @Test
    public void testEquals() {
    	new EqualsMethodTester().testEqualsMethod(new DevExchangeRequestFactory(), configuration);
    }
}
