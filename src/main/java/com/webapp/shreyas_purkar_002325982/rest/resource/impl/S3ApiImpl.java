package com.webapp.shreyas_purkar_002325982.rest.resource.impl;

import com.webapp.shreyas_purkar_002325982.dto.S3ObjectDto;
import com.webapp.shreyas_purkar_002325982.exception.EmptyFileException;
import com.webapp.shreyas_purkar_002325982.rest.resource.S3Api;
import com.webapp.shreyas_purkar_002325982.service.S3Service;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Implementation of S3ObjectUploadApi to handle API requests for S3 objects
 */
@RestController
public class S3ApiImpl implements S3Api {

    private static final Logger log = LoggerFactory.getLogger(S3ApiImpl.class);

    @Autowired
    S3Service service;

    @Autowired
    MeterRegistry meterRegistry;

    /**
     * API to get S3 object for given Id
     *
     * @param id of S3 object
     */
    @Override
    public ResponseEntity<S3ObjectDto> getObject(String id) {
        meterRegistry.counter("api.get-object.count").increment();
        Timer.Sample getFileApiTimer = Timer.start(meterRegistry);

        log.info("Fetching file with id {}...", id);

        try {
            S3ObjectDto dto = service.getObject(id);
            return ResponseEntity.status(HttpStatus.OK).body(dto);
        } finally {
            getFileApiTimer.stop(meterRegistry.timer("api.get-object.time"));
        }
    }

    /**
     * API to create S3 object
     *
     * @param file to upload on S3
     */
    @Override
    public ResponseEntity<S3ObjectDto> uploadObject(MultipartFile file) {
        meterRegistry.counter("api.file-upload-on-s3.count").increment();
        Timer.Sample uploadFileApiTimer = Timer.start(meterRegistry);

        log.info("Initializing uploading of file on S3...");

        try {
            if (file.isEmpty()) {
                log.warn("Bad Request - No file is uploaded. Please select a valid file");
                throw new EmptyFileException();
            }

            S3ObjectDto dto = service.uploadObject(file);
            return ResponseEntity.status(HttpStatus.CREATED).body(dto);
        } finally {
            uploadFileApiTimer.stop(meterRegistry.timer("api.file-upload-on-s3.time"));
        }
    }

    /**
     * API to delete S3 object for given Id
     *
     * @param id of S3 object
     */
    @Override
    public ResponseEntity<?> deleteObject(String id) {
        meterRegistry.counter("api.delete-file-on-s3.count").increment();
        Timer.Sample deleteFileApiTimer = Timer.start(meterRegistry);

        log.info("Initializing deleting file with id {}...", id);

        try {
            service.deleteObject(id);
            return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
        } finally {
            deleteFileApiTimer.stop(meterRegistry.timer("api.delete-file-on-s3.time"));
        }

    }
}
