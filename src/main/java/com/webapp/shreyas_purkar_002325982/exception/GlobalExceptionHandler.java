package com.webapp.shreyas_purkar_002325982.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
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
        return getMapResponseEntityForServiceUnavailable();
    }

    private ResponseEntity<String> getMapResponseEntityForServiceUnavailable() {
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
     * Exception to handle unsupported HTTP methods
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<String> handleMethodNotAllowedException(
            HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {

        String method = request.getMethod();
        String requestURI = request.getRequestURI();

        if (requestURI.startsWith("/v1/file")) {
            if (method.equalsIgnoreCase("GET") || method.equalsIgnoreCase("DELETE")) {
                return getMapResponseEntityForBadRequest();
            } else if (method.equalsIgnoreCase("PUT") || method.equalsIgnoreCase("PATCH")
                    || method.equalsIgnoreCase("HEAD") || method.equalsIgnoreCase("OPTIONS")
                    || method.equalsIgnoreCase("UPDATE")) {
                return getMapResponseForMethodNotAllowed();
            }
        }

        if (requestURI.startsWith("/healthz")) {
            if (method.equalsIgnoreCase("PUT") || method.equalsIgnoreCase("PATCH")
                    || method.equalsIgnoreCase("HEAD") || method.equalsIgnoreCase("OPTIONS")
                    || method.equalsIgnoreCase("UPDATE") || method.equalsIgnoreCase("DELETE")
            || method.equalsIgnoreCase("POST")) {
                return getMapResponseForMethodNotAllowed();
            }
        }

        if (requestURI.startsWith("/v1/file/")) {
            if (method.equalsIgnoreCase("PUT") || method.equalsIgnoreCase("PATCH")
                    || method.equalsIgnoreCase("HEAD") || method.equalsIgnoreCase("OPTIONS")
                    || method.equalsIgnoreCase("UPDATE") || method.equalsIgnoreCase("POST")) {
                return getMapResponseForMethodNotAllowed();
            }
        }

        return getMapResponseForMethodNotAllowed();
    }

    private static ResponseEntity<String> getMapResponseEntityForBadRequest() {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
    }

    private static ResponseEntity<String> getMapResponseForMethodNotAllowed() {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).build();
    }

    /**
     * Exception to handle resource not found cases
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<Void> handleNotFoundException(NoHandlerFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }

    /**
     * Exception to handle resource not found cases for S3
     */
    @ExceptionHandler(S3ObjectNotFoundException.class)
    public ResponseEntity<String> handleS3ObjectNotFoundException(S3ObjectNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }

    /**
     * Exception to handle incorrect content type (if request body is not multipart/form-data)
     */
    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<String> handleMultipartException(MultipartException ex) {
        return getMapResponseEntityForBadRequest();
    }

    /**
     * Exception to handle incorrect content type
     */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<String> handleUnsupportedMediaType(
            HttpMediaTypeNotSupportedException ex, HttpServletRequest request) {
        return getMapResponseEntityForBadRequest();
    }

    /**
     * Exception to handle missing file (invalid request)
     */
    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<String> handleMissingFileException(MissingServletRequestPartException ex) {
        return getMapResponseEntityForBadRequest();
    }

    /**
     * Exception to handle empty file upload cases for S3
     */
    @ExceptionHandler(EmptyFileException.class)
    public ResponseEntity<String> handleEmptyFileException(EmptyFileException ex) {
        return getMapResponseEntityForBadRequest();
    }

    /**
     * Exception to handle file upload cases for S3
     */
    @ExceptionHandler(FileUploadException.class)
    public ResponseEntity<String> handleFileUploadException(FileUploadException ex) {
        return getMapResponseEntityForBadRequest();
    }

    /**
     * Exception to handle file deletion failure
     */
    @ExceptionHandler
    public ResponseEntity<String> handleFileDeletionException(FileDeletionException ex) {
        return getMapResponseEntityForServiceUnavailable();
    }
}
