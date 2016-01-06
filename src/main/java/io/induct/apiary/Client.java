package io.induct.apiary;

import com.google.common.base.CaseFormat;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Defines metadata used to control the generation process of HTTP API clients.
 *
 * @since 1.1.2016
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Client {
    /**
     * Format of the parameters the API uses.
     */
    CaseFormat paramFormat() default CaseFormat.LOWER_CAMEL;

    /**
     * Target package for generated HTTP API client. Defaults to `${root}.impl`, where `root` matches with the defining
     * interface's own package.
     */
    String targetPackage() default "${root}.impl";

    /**
     * Target class name for generated HTTP API client. Defaults to `${clientName}Client`, where `clientName` is the
     * name of the defining interface.
     */
    String targetClassName() default "${clientName}Client";

    /**
     * Metadata for all environments this client can be run in, identified by {@link Environment#name()}. Defining
     * multiple environments in annotations allows one to define clients which can run in multiple environments, eg.
     * local, QA, staging and production, different DCs etc. etc.
     */
    Environment[] environments();

    /**
     * Environment specific configuration data, identified by {@link #name()}.
     *
     * @since 6.1.2016
     */
    @Target(ElementType.ANNOTATION_TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    @interface Environment {
        /**
         * Unique name of the environment this configuration should be used in.
         */
        String name();

        /**
         * Root part of the URI that should be prepended to all API paths defined with {@link Api#path()}. More formally
         * this is the *scheme*, *separator* and *authority* parts of the full URI, for example <code>abc://username:password@example.com:123</code>
         *
         * @see <a href="https://en.wikipedia.org/wiki/Uniform_Resource_Identifier#Examples">Wikipedia: URI</a>
         */
        String root();
    }
}
