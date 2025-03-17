package com.webapp.shreyas_purkar_002325982.exception;

/**
 * Exception to handle file upload issues on S3
 */
public class FileUploadException extends RuntimeException {
    public FileUploadException(String message) {
        super(message);
    }

}
