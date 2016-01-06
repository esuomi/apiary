package io.induct.apiary;

/**
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
