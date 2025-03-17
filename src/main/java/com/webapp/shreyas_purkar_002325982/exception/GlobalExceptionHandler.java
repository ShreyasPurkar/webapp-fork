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

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Global exception handler for API error scenarios
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Exception to handle database connectivity issues
     */
    @ExceptionHandler(DatabaseConnectionException.class)
    public ResponseEntity<Map<String, Object>> handleDatabaseConnectionException(DatabaseConnectionException ex) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("timestamp", LocalDateTime.now());
        errorResponse.put("status", HttpStatus.SERVICE_UNAVAILABLE.value());
        errorResponse.put("error", HttpStatus.SERVICE_UNAVAILABLE.name());
        errorResponse.put("message", ex.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(errorResponse);
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
    public ResponseEntity<Map<String, Object>> handleMethodNotAllowedException(
            HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {

        String method = request.getMethod();
        String requestURI = request.getRequestURI();

        if (requestURI.startsWith("/v1/file")) {
            if (method.equalsIgnoreCase("GET") || method.equalsIgnoreCase("DELETE")) {
                return getMapResponseEntityForBadRequest("This HTTP method is not allowed for this endpoint.");
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

    private static ResponseEntity<Map<String, Object>> getMapResponseEntityForBadRequest(String message) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("timestamp", LocalDateTime.now());
        errorResponse.put("status", HttpStatus.BAD_REQUEST.value());
        errorResponse.put("error", HttpStatus.BAD_REQUEST.name());
        errorResponse.put("message", message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    private static ResponseEntity<Map<String, Object>> getMapResponseForMethodNotAllowed() {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("timestamp", LocalDateTime.now());
        errorResponse.put("status", HttpStatus.METHOD_NOT_ALLOWED.value());
        errorResponse.put("error", HttpStatus.METHOD_NOT_ALLOWED.name());
        errorResponse.put("message", "This HTTP method is not allowed for this endpoint.");
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(errorResponse);
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
    public ResponseEntity<Map<String, Object>> handleS3ObjectNotFoundException(S3ObjectNotFoundException ex) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("timestamp", LocalDateTime.now());
        errorResponse.put("status", HttpStatus.NOT_FOUND.value());
        errorResponse.put("error", HttpStatus.NOT_FOUND.name());
        errorResponse.put("message", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    /**
     * Exception to handle incorrect content type (if request body is not multipart/form-data)
     */
    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<Map<String, Object>> handleMultipartException(MultipartException ex) {
        return getMapResponseEntityForBadRequest("Invalid request. Only multipart/form-data is allowed.");
    }

    /**
     * Exception to handle incorrect content type
     */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<Map<String, Object>> handleUnsupportedMediaType(
            HttpMediaTypeNotSupportedException ex, HttpServletRequest request) {
        return getMapResponseEntityForBadRequest("Invalid request. Only multipart/form-data is allowed.");
    }

    /**
     * Exception to handle missing file (invalid request)
     */
    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<Map<String, Object>> handleMissingFileException(MissingServletRequestPartException ex) {
        return getMapResponseEntityForBadRequest("File is required. Please upload a file.");
    }

    /**
     * Exception to handle empty file upload cases for S3
     */
    @ExceptionHandler(EmptyFileException.class)
    public ResponseEntity<Map<String, Object>> handleEmptyFileException(EmptyFileException ex) {
        return getMapResponseEntityForBadRequest(ex.getMessage());
    }

    /**
     * Exception to handle file upload cases for S3
     */
    @ExceptionHandler(FileUploadException.class)
    public ResponseEntity<Map<String, Object>> handleFileUploadException(FileUploadException ex) {
        return getMapResponseEntityForBadRequest(ex.getMessage());
    }
}
