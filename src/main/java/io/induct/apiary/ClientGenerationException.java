package io.induct.apiary;

/**
 * @since 3.1.2016
 */
class ClientGenerationException extends ApiaryException {
    public ClientGenerationException(String message, Throwable throwable) {
        super(message, throwable);
    }

    public ClientGenerationException(String message) {
        super(message);
    }
}
