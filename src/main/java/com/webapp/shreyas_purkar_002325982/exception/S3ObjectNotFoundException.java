package com.webapp.shreyas_purkar_002325982.exception;

/**
 * Exception to handle S3 object not found error
 */
public class S3ObjectNotFoundException extends RuntimeException {

    public S3ObjectNotFoundException(String message) {
        super(message);
    }
}
