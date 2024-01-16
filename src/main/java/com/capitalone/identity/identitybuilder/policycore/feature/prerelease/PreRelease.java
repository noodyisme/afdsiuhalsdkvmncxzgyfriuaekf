package com.capitalone.identity.identitybuilder.policycore.feature.prerelease;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotate spring-injected component classes and methods to prevent them from being used in production environments.
 * <ol>
 *     <li>Never add to method or class currently in-use in production.</li>
 *     <li>Meant for core feature components and services only. Not every new class or method.</li>
 *     <li>Meant as a runtime safeguard. It doesn't address issues common with widespread public
 *     libraries like warning library users a class api can change in the future.</li>
 * </ol>
 *
 * @see PreReleaseGuard#PROPERTY_NAME
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface PreRelease {

}
