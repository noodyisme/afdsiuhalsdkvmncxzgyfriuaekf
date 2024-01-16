package com.capitalone.identity.identitybuilder.policycore.rest.config;

import com.capitalone.identity.identitybuilder.policycore.rest.config.filewatcher.FileWatcherReloadStrategy;
import com.capitalone.identity.identitybuilder.policycore.schema.SchemaManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.camel.CamelContext;
import org.apache.camel.component.jackson.JacksonDataFormat;
import org.apache.camel.spi.ExecutorServiceManager;
import org.apache.camel.spi.ThreadPoolProfile;
import org.apache.camel.spring.boot.CamelContextConfiguration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientProperties;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.*;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.security.oauth2.client.AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.InMemoryReactiveOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.ReactiveOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.InMemoryReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.reactive.function.client.ServerOAuth2AuthorizedClientExchangeFilterFunction;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Spring Bean Configuration Class
 * @author gtc237
 */
@Configuration("chassisApiSpringConfig")
@ComponentScan(basePackages = {"com.capitalone.chassis.cal1_5", "com.capitalone.identity.identitybuilder"})
@EnableAspectJAutoProxy(proxyTargetClass = true)
@PropertySource("classpath:application-default.properties")
public class SpringConfig {
	/** The logger for this class. */
    private final Logger logger = LogManager.getLogger(getClass());

	private final SchemaManager schemaManager;

	/**
	 * Implicit injection via constructor
	 * @param schemaManager used to invalidate schema cache on FileWatcher change
	 */
	public SpringConfig(SchemaManager schemaManager) {
		this.schemaManager = schemaManager;
	}

	/**
	 * Camel Context Configuration for default and custom threadpools
	 */
	@Bean
	CamelContextConfiguration camelContextConfiguration() {
		return new CamelContextConfiguration() {
			@SuppressWarnings("deprecation")
			@Override
			public void beforeApplicationStart(CamelContext camelContext) {
				// Set the default threadpool
		        ExecutorServiceManager manager = camelContext.getExecutorServiceManager();
		        ThreadPoolProfile profile = manager.getDefaultThreadPoolProfile();
		        profile.setId("default");
		        camelContext.setAllowUseOriginalMessage(false);
		        camelContext.setMessageHistory(false);
		        camelContext.getExecutorServiceManager().registerThreadPoolProfile(profile);

		        // For devint-test, watch for any changed XML files on our Camel resource path
		        // The default reload strategy is not set to be recursive so we supply a recursive
		        // one ourselves. Also, the directory path must be absolute so we'll resolve it
		        // from the resource path.
		        if ("devint-test".equals(System.getProperty("spring.profiles.active"))) {
			        URL url = this.getClass().getClassLoader().getResource("camel");
			        if (url != null) {
						String path = url.getPath();
						class CustomFileWatcherReloadStrategy extends FileWatcherReloadStrategy {
							CustomFileWatcherReloadStrategy(String directory, boolean isRecursive) {
								super(directory, isRecursive);
							}

							@Override
							public void onReloadXml(CamelContext camelContext, String name, InputStream resource) {
								super.onReloadXml(camelContext, name, resource);
								schemaManager.purgeCache();
							}
						}
						FileWatcherReloadStrategy strategy = new CustomFileWatcherReloadStrategy(path, true);
						try {
							camelContext.addService(strategy, true, true);
						} catch (Exception e) {
							logger.info("Unable to start devint-test policy reload strategy", e);
						}
					} else {
			        	logger.info("Unable to start devint-test policy reload strategy: resources/camel/ not found");
					}
		        }
			}

			@Override
			public void afterApplicationStart(CamelContext camelContext) {
				// Implementation not required. We're not doing anything after application startup.
			}
		};
	}


	@Bean
	public MessageSource messageSource(@Value("${message.reload.timeout}") Integer messageReloadTimeout, @Value("${error.messages.basename}") String baseNames) {
		return getMessageBundle(messageReloadTimeout, baseNames);
	}

	@Bean
	public MessageSource validationMessages(@Value("${message.reload.timeout}") Integer messageReloadTimeout, @Value("${validation.messages.basename}") String baseNames) {
		return getMessageBundle(messageReloadTimeout, baseNames);
	}

	private MessageSource getMessageBundle(Integer messageReloadTimeout, String baseNames) {
		ReloadableResourceBundleMessageSource messageSource = new ReloadableResourceBundleMessageSource();
		messageSource.setBasenames(baseNames, "classpath:policy-core.error-messages");
		messageSource.setCacheSeconds(messageReloadTimeout);
		return messageSource;
	}

	@Bean(name = "jackson")
	public JacksonDataFormat jacksonDataFormat(ObjectMapper objectMapper) {
	    return new JacksonDataFormat(objectMapper, HashMap.class);
	}

	@Bean
	@Primary
	public ReactiveClientRegistrationRepository reactiveClientRegistrationRepository(OAuth2ClientProperties oAuth2ClientProperties) {
		List<ClientRegistration> registrations = new ArrayList<>();
		oAuth2ClientProperties.getRegistration().forEach((registrationId, registration) -> {
				ClientRegistration aRegistration = ClientRegistration
						.withRegistrationId(registrationId)
						.tokenUri(oAuth2ClientProperties.getProvider().get(registrationId).getTokenUri())
						.authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
						.clientId(registration.getClientId())
						.clientSecret(registration.getClientSecret())
						.build();
				registrations.add(aRegistration);
			});
		return new InMemoryReactiveClientRegistrationRepository(registrations);
	}

	@Bean
	@Primary
	public WebClient oauth2Client(ReactiveClientRegistrationRepository clientRegistrationRepository, @Value("${env.gatewayURL}") String gatewayURL, @Value("${oauth.appname}") String clientRegistrationId){
		ReactiveOAuth2AuthorizedClientService reactiveOAuth2AuthorizedClientService = new InMemoryReactiveOAuth2AuthorizedClientService(
				clientRegistrationRepository);
		AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager oauthManager = new AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager(
				clientRegistrationRepository, reactiveOAuth2AuthorizedClientService);
		ServerOAuth2AuthorizedClientExchangeFilterFunction oauth2 = new ServerOAuth2AuthorizedClientExchangeFilterFunction(oauthManager);
		oauth2.setDefaultClientRegistrationId(clientRegistrationId);

		return WebClient
				.builder()
				.baseUrl(gatewayURL)
				.filter(oauth2)
				.build();
	}
}
