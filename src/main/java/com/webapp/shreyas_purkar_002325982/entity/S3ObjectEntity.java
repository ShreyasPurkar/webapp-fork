package com.webapp.shreyas_purkar_002325982.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.Instant;

/**
 * Entity class for S3 objects
 */
@Data
@Entity
@Table(name = "s3_object_metadata")
public class S3ObjectEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "object_id", nullable = false)
    private String objectId;

    @Column(name = "url", nullable = false)
    private String url;

    @Column(name = "upload_date", nullable = false)
    private Instant uploadDate;

    @Column(name = "content_length", nullable = false)
    private Long contentLength;

    @Column(name = "content_type", nullable = false)
    private String contentType;
}
