package com.webapp.shreyas_purkar_002325982.rest.resource.impl;

import com.webapp.shreyas_purkar_002325982.dto.S3ObjectDto;
import com.webapp.shreyas_purkar_002325982.exception.EmptyFileException;
import com.webapp.shreyas_purkar_002325982.rest.resource.S3Api;
import com.webapp.shreyas_purkar_002325982.service.S3Service;
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

    /**
     * API to get S3 object for given Id
     *
     * @param id of S3 object
     */
    @Override
    public ResponseEntity<S3ObjectDto> getObject(String id) {
        log.info("Fetching S3 object with id {}...", id);
        S3ObjectDto dto = service.getObject(id);
        return ResponseEntity.status(HttpStatus.OK).body(dto);
    }

    /**
     * API to create S3 object
     *
     * @param file to upload on S3
     */
    @Override
    public ResponseEntity<S3ObjectDto> uploadObject(MultipartFile file) {
        log.info("Uploading file on S3...");
        if (file.isEmpty()) {
            throw new EmptyFileException("Uploaded file is empty. Please select a valid file.");
        }

        S3ObjectDto dto = service.uploadObject(file);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    /**
     * API to delete S3 object for given Id
     *
     * @param id of S3 object
     */
    @Override
    public ResponseEntity<?> deleteObject(String id) {
        log.info("Deleting S3 object with id {}...", id);
        service.deleteObject(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
