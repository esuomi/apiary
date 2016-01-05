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
}
