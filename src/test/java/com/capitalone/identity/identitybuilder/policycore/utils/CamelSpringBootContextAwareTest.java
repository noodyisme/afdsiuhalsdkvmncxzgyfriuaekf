package com.capitalone.identity.identitybuilder.policycore.utils;

import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurationExcludeFilter;
import org.springframework.boot.context.TypeExcludeFilter;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.core.annotation.AliasFor;
import org.springframework.test.context.ContextConfiguration;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Composite annotation for
 * {@link CamelSpringBootTest @CamelSpringBootTest},
 * {@link SpringBootTest @SpringBootTest},
 * {@link ContextConfiguration @ContextConfiguration},
 * {@link SpringBootConfiguration @SpringBootConfiguration}
 * {@link ComponentScan @ComponentScan}
 *
 * @author plz569
 */

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@CamelSpringBootTest
@SpringBootTest
@ContextConfiguration
@SpringBootConfiguration
@ComponentScan(
        excludeFilters = {@ComponentScan.Filter(
                type = FilterType.CUSTOM,
                classes = {TypeExcludeFilter.class}
        ), @ComponentScan.Filter(
                type = FilterType.CUSTOM,
                classes = {AutoConfigurationExcludeFilter.class}
        )}
)
public @interface CamelSpringBootContextAwareTest {

        @AliasFor(annotation = ContextConfiguration.class, attribute = "classes")
        Class<?>[] contextConfigClasses() default {};

        @AliasFor(annotation = ComponentScan.class, attribute = "excludeFilters")
        ComponentScan.Filter[] excludeFromComponentScan() default {};

        @AliasFor(annotation = ComponentScan.class, attribute = "basePackages")
        String[] basePackages() default {};

        @AliasFor(annotation = ComponentScan.class, attribute = "basePackageClasses")
        Class<?>[] basePackageClasses() default {};

}
