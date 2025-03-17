package com.webapp.shreyas_purkar_002325982.rest.resource;

import com.webapp.shreyas_purkar_002325982.dto.S3ObjectDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * Interface for handling API request for file uploads on s3
 */
@RequestMapping("/v1/file")
public interface S3Api {

    /**
     * API to get S3 object for given Id
     */
    @GetMapping("/{id}")
    ResponseEntity<S3ObjectDto> getObject(@PathVariable("id") String id);

    /**
     * API to create S3 object
     */
    @PostMapping(consumes = "multipart/form-data")
    ResponseEntity<S3ObjectDto> uploadObject(@RequestParam("file") MultipartFile file);

    /**
     * API to delete S3 object for given Id
     */
    @DeleteMapping("/{id}")
    ResponseEntity<?> deleteObject(@PathVariable("id") String id);
}
