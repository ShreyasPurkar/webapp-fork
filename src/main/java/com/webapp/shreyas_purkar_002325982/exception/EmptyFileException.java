package com.webapp.shreyas_purkar_002325982.exception;

/**
 * Exception to handle empty file
 */
public class EmptyFileException extends RuntimeException {
    public EmptyFileException(String message) {
        super(message);
    }
}
