package com.capitalone.identity.identitybuilder.policycore.service.dao.impl;

import com.capitalone.chassis.engine.consumerservices.common.servicelocator.EndpointLocator;
import com.capitalone.chassis.engine.consumerservices.common.servicelocator.model.EndpointProperties;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Component
@ConditionalOnProperty(value = DevExchangeDAOWebImpl.WEB_CLIENT_ENABLED_FLAG)
public class DevExchangeWebClientFactory {
	@Value("${security.oauth2.client.partnerAccessTokenUri:}")
	private String partnerAccessTokenUri;

	private final int defaultConnectionTimeout;

	private final int defaultRequestTimeout;

	private EndpointLocator chassisEndpointlocator;
	private Map<String, Integer> requestTimeoutMap;
	private Map<String, Integer> connectionTimeoutMap;
	private Map<String, WebClient> requestFactoryMap;

	private WebClient devexWebclient;
	private WebClient pdxWebClient;

	@Autowired
	public DevExchangeWebClientFactory(@Qualifier("devexWebClient") WebClient devexWebclient,@Qualifier("pdxWebClient") WebClient pdxWebClient, @Qualifier("chassisFileServiceLocator") EndpointLocator chassisEndpointlocator,
									   @Value("${chassis.servicelocator.default.connectTimeout}") int connectionTimeout, @Value("${chassis.servicelocator.default.requestTimeout}") int requestTimeout) {
		this.devexWebclient = devexWebclient;
		this.pdxWebClient = pdxWebClient;
		this.chassisEndpointlocator = chassisEndpointlocator;
		this.defaultConnectionTimeout = connectionTimeout;
		this.defaultRequestTimeout = requestTimeout;
	}

	@PostConstruct
	void init() {
		connectionTimeoutMap = new HashMap<>();
		requestTimeoutMap = new HashMap<>();
		requestFactoryMap = new HashMap<>();
	}

	public WebClient getDevExchangeWebClient(String serviceName, String scheme) {
		String key = scheme+"-"+serviceName;
		WebClient webClient = requestFactoryMap.get(key);
		if (webClient != null) {
			return webClient;
		}

		webClient = createWebClientForService(serviceName, scheme);
		requestFactoryMap.put(key, webClient);
		return webClient;
	}

	private WebClient createWebClientForService(String serviceName, String scheme) {
		//if scheme is pdx and missing partnerAccessTokenUri, throw exception
		if("pdx".equalsIgnoreCase(scheme) && "".equals(this.partnerAccessTokenUri)){
			throw new IllegalArgumentException("Add the missing application property security.oauth2.client.partnerAccessTokenUri");
		}

		WebClient webClient = "dx".equalsIgnoreCase(scheme) ? devexWebclient:pdxWebClient;
		ClientHttpConnector connector = createHttpConnector(serviceName, scheme);

		return webClient.mutate().clientConnector(connector).build();
	}

	private ClientHttpConnector createHttpConnector(String serviceName, String scheme){
		int connectionTimeout;
		int requestTimeout;

		Optional<EndpointProperties> epProperties = chassisEndpointlocator
				.getEndpointProperties(serviceName);
		if (epProperties.isEmpty()) {
			connectionTimeout = this.defaultConnectionTimeout;
			requestTimeout = this.defaultRequestTimeout;
		} else {
			connectionTimeout = epProperties.get().getConnectTimeout();
			requestTimeout = epProperties.get().getRequestTimeout();
		}

		// Timeout connection maps per service. Static values required for connection handler lambdas
		String key = scheme+"-"+serviceName;
		requestTimeoutMap.put(key, requestTimeout);
		connectionTimeoutMap.put(key, connectionTimeout);

		HttpClient httpClient = HttpClient.create()
				.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectionTimeout)
				.doOnConnected(conn ->
						conn.addHandlerLast(new ReadTimeoutHandler(requestTimeoutMap.get(key), TimeUnit.MILLISECONDS))
								.addHandlerLast(new WriteTimeoutHandler(connectionTimeoutMap.get(key), TimeUnit.MILLISECONDS)));

		return new ReactorClientHttpConnector(httpClient);
	}
}

