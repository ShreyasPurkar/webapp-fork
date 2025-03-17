package com.webapp.shreyas_purkar_002325982.service;

import com.webapp.shreyas_purkar_002325982.dto.S3ObjectDto;
import org.springframework.web.multipart.MultipartFile;

/**
 * Service class for managing S3 objects
 */
public interface S3Service {

    /**
     * Method to get S3 object for given Id
     *
     * @param id of S3 object
     */
    S3ObjectDto getObject(String id);

    /**
     * Method to create S3 object
     *
     * @param file to upload on S3
     */
    S3ObjectDto uploadObject(MultipartFile file);

    /**
     * Method to delete S3 object for given Id
     *
     * @param id of S3 object
     */
    void deleteObject(String id);
}
