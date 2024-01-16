package com.capitalone.identity.identitybuilder.policycore.service.util;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;

import org.glassfish.jersey.logging.LoggingFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.TestProperties;

import org.mockito.MockitoAnnotations;

import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.GenericApplicationContext;

import java.util.*;


/**
 * JerseyTest class configured to run with SpringBoot application context. Extend your test class with this class
 * and include the getResourceClasses() and getResourcePackages() methods to your test to register additional packages
 * and classes.
 * */
@SpringBootTest
public abstract class SpringJerseyTest extends JerseyTest {



    @Override
    protected ResourceConfig configure() {
        MockitoAnnotations.openMocks(this);
        enable(TestProperties.LOG_TRAFFIC);
        enable(TestProperties.DUMP_ENTITY);
        set(TestProperties.CONTAINER_PORT, "0");
        final ResourceConfig resourceConfig =
                new ResourceConfig()
                        .property("contextConfig", createSpringContext(getBeanMap()))
                        .property(LoggingFeature.LOGGING_FEATURE_LOGGER_LEVEL_SERVER, "WARNING")
                        .registerClasses(getResourceClasses());

        return serverConfig(resourceConfig);
    }

    /**
     * Gives the test class opportunity to further customize the configuration. Like registering a
     * MultiPartFeature if required.
     *
     * @param config ResourceConfig used in configuration
     * @return same as param. internal ref.
     */
    protected ResourceConfig serverConfig(final ResourceConfig config) {
        return config;
    }

    /**
     * Supplies all the bean objects required to be loaded in the application context for the Resource class
     * under test
     *
     * @return initialized empty list of Spring Beans
     */
    protected List<Object> getBeans() {
        return Collections.emptyList();
    }

    /**
     * Supplies all the bean objects with name qualifier required to be loaded in the application context for the Resource class
     * under test
     *
     * @return initialized empty Map of qualified Spring Beans
     */
    protected Map<String, Object> getQualifiedBeans() {
        return Collections.emptyMap();
    }

    protected Map<String, Object> getBeanMap() {
        final Map<String, Object> result = new HashMap<>();
        CollectionUtils.emptyIfNull(getBeans())
                .forEach(obj -> result.put(StringUtils.uncapitalize(obj.getClass().getSimpleName()), obj));
        result.putAll(MapUtils.emptyIfNull(getQualifiedBeans()));
        return result;
    }

    /**
     * Resource class under test
     *
     * @return set of classes to register as resources
     */
    protected abstract Set<Class<?>> getResourceClasses();



    /**
     * Creates & returns a Spring GenericApplicationContext from the given beans with qualified names
     *
     * @param beans the Spring Beans to register.
     * @return Spring GenericApplicationContext created from given beans.
     */
    public static ApplicationContext createSpringContext(Map<String, Object> beans) {
        final DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
        MapUtils.emptyIfNull(beans).forEach(beanFactory::registerSingleton);
        final GenericApplicationContext context = new GenericApplicationContext(beanFactory);
        context.refresh();
        return context;
    }
}
