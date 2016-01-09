package io.induct.apiary.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Defines individual API call specifics.
 *
 * @since 1.1.2016
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Api {
    /**
     * Path (excluding parameters) of the API
     * @return API path
     */
    String path();

}
