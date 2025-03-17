package com.webapp.shreyas_purkar_002325982.dto;

import lombok.*;

import java.time.Instant;

/**
 * DTO for S3 objects
 */
@Data
public class S3ObjectDto {
    private String fileName;
    private String objectId;
    private String url;
    private Instant uploadDate;
}
