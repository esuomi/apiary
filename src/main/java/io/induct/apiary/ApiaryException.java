package io.induct.apiary;

/**
 * @author Esko Suomi <suomi.esko@gmail.com>
 * @since 3.1.2016
 */
public class ApiaryException extends RuntimeException {
    public ApiaryException(String message) {
        super(message);
    }

    public ApiaryException(String message, Throwable cause) {
        super(message, cause);
    }

    public ApiaryException(Throwable cause) {
        super(cause);
    }
}
