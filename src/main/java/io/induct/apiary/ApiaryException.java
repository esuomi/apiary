package io.induct.apiary;

/**
 * @author Esko Suomi <suomi.esko@gmail.com>
 * @since 3.1.2016
 */
class ApiaryException extends RuntimeException {
    ApiaryException(String message) {
        super(message);
    }

    public ApiaryException(String message, Throwable cause) {
        super(message, cause);
    }

}
