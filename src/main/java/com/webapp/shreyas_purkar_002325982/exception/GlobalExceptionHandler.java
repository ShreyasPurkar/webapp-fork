package com.webapp.shreyas_purkar_002325982.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;

/**
 * Global exception handler for API error scenarios
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Exception to handle database connectivity issues
     */
    @ExceptionHandler(DatabaseConnectionException.class)
    public ResponseEntity<String> handleDatabaseConnectionException(DatabaseConnectionException ex) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
    }

    /**
     * Exception to handle the error case where the payload is passed in a GET request
     */
    @ExceptionHandler(PayloadNotAllowedException.class)
    public ResponseEntity<String> handleInvalidPayloadException(PayloadNotAllowedException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
    }

    /**
     * Exception to handle method not allowed cases
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Void> handleMethodNotAllowedException(HttpRequestMethodNotSupportedException ex) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).build();
    }

    /**
     * Exception to handle resource not found cases
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<Void> handleNotFoundException(NoHandlerFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }
}
