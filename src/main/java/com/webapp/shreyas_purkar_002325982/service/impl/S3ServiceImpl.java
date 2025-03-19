package com.webapp.shreyas_purkar_002325982.service.impl;

import com.webapp.shreyas_purkar_002325982.dto.S3ObjectDto;
import com.webapp.shreyas_purkar_002325982.entity.S3ObjectEntity;
import com.webapp.shreyas_purkar_002325982.exception.DatabaseConnectionException;
import com.webapp.shreyas_purkar_002325982.exception.FileUploadException;
import com.webapp.shreyas_purkar_002325982.exception.S3ObjectNotFoundException;
import com.webapp.shreyas_purkar_002325982.repository.S3ObjectMetadataRepository;
import com.webapp.shreyas_purkar_002325982.service.S3Service;
import jakarta.persistence.PersistenceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.InvalidDataAccessResourceUsageException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.core.sync.RequestBody;

import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Service class implementation to monitor health of application instance
 */
@Service
public class S3ServiceImpl implements S3Service {

    private static final Logger log = LoggerFactory.getLogger(S3ServiceImpl.class);

    @Autowired
    S3ObjectMetadataRepository repository;

    private final S3Client s3Client;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    public S3ServiceImpl(S3ObjectMetadataRepository repository, @Value("${aws.s3.region}") String region) {
        this.repository = repository;
        this.s3Client = S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }

    /**
     * Method to get S3 object for given Id
     *
     * @param id of S3 object
     */
    @Override
    public S3ObjectDto getObject(String id) {
        Optional<S3ObjectEntity> entity = findS3Object(id);

        if (entity.isEmpty()) {
            throw new S3ObjectNotFoundException("Unable to find the object with id " + id);
        }

        S3ObjectDto dto = new S3ObjectDto();
        dto.setFileName(entity.get().getFileName());
        dto.setUrl(entity.get().getUrl());
        dto.setObjectId(entity.get().getObjectId());
        dto.setUploadDate(entity.get().getUploadDate());
        dto.setContentType(entity.get().getContentType());
        dto.setContentLength(entity.get().getContentLength());

        log.info("Retrieved S3 object successfully");
        return dto;
    }

    /**
     * Method to get S3 object metadata
     *
     * @param id of S3 object
     * @return entity containing S3 object metadata
     */
    private Optional<S3ObjectEntity> findS3Object(String id) {
        Optional<S3ObjectEntity> entity;

        try {
            entity = Optional.ofNullable(repository.findByObjectId(id));
        } catch (CannotCreateTransactionException | InvalidDataAccessResourceUsageException |
                 DataIntegrityViolationException | DataAccessResourceFailureException |
                 PersistenceException ex) {
            log.error("Failed to retrieve the S3 object");
            throw new DatabaseConnectionException("Failed to retrieve the S3 object");
        } catch (Exception ex) {
            log.error("Unexpected error occurred: {}", ex.getMessage());
            throw new DatabaseConnectionException("Failed to retrieve the S3 object");
        }
        return entity;
    }

    /**
     * Method to create S3 object
     *
     * @param file to upload on S3
     */
    @Override
    public S3ObjectDto uploadObject(MultipartFile file) {
        UUID fileId = UUID.randomUUID();
        log.info("UUID of the file is " + fileId);

        String url = uploadObjectToS3(file, fileId);

        Map<String, Object> metadata = getObjectMetadata(url, fileId.toString());

        S3ObjectEntity entity = new S3ObjectEntity();
        entity.setObjectId(fileId.toString());
        entity.setUrl(url);
        entity.setFileName(file.getOriginalFilename());
        entity.setUploadDate(Instant.now());
        entity.setContentLength(file.getSize());
        entity.setContentType(file.getContentType());
        entity.setEtag(metadata.get("ETag").toString());
        entity.setAcceptRanges(metadata.get("AcceptRanges").toString());
        entity.setServerSideEncryption(metadata.get("ServerSideEncryption").toString());
        entity.setLastModified(metadata.get("LastModified").toString());

        try {
            repository.save(entity);
            log.info("Successfully persisted metadata in database");
        } catch (CannotCreateTransactionException | InvalidDataAccessResourceUsageException |
                 DataIntegrityViolationException | DataAccessResourceFailureException |
                 PersistenceException ex) {
            log.error("Deleting the S3 object with id " + fileId);
            deleteS3Object(url.substring(url.indexOf("/") + 1), fileId.toString());

            log.error("Failed to persist the S3 object metadata");
            throw new DatabaseConnectionException("Failed to persist the S3 object metadata");
        } catch (Exception ex) {
            log.error("Deleting the S3 object with id " + fileId);
            deleteS3Object(url.substring(url.indexOf("/") + 1), fileId.toString());

            log.error("Unexpected error occurred: {}", ex.getMessage());
            throw new DatabaseConnectionException("Failed to persist the S3 object metadata");
        }

        S3ObjectDto dto = new S3ObjectDto();
        dto.setFileName(entity.getFileName());
        dto.setUrl(entity.getUrl());
        dto.setObjectId(entity.getObjectId());
        dto.setUploadDate(entity.getUploadDate());
        dto.setContentType(entity.getContentType());
        dto.setContentLength(entity.getContentLength());

        return dto;
    }

    /**
     * Method to get S3 object system metadata
     *
     * @param url of object in S3
     * @return metadata
     */
    private Map<String, Object> getObjectMetadata(String url, String fileId) {
        log.info("Getting object metadata...");

        HeadObjectResponse response;

        String key = url.substring(url.indexOf("/") + 1);

        HeadObjectRequest headObjectRequest = HeadObjectRequest.builder()
                                                               .bucket(bucketName)
                                                               .key(key)
                                                               .build();

        try {
            response = s3Client.headObject(headObjectRequest);
        } catch (SdkClientException e) {
            log.error("Deleting the S3 object with id " + fileId);
            deleteS3Object(key, fileId);

            log.error("S3 is unavailable. Failed to fetch metadata for {}", key);
            throw new DatabaseConnectionException("S3 service is down. Please try again later.");
        } catch (Exception e) {
            log.error("Deleting the S3 object with id " + fileId);
            deleteS3Object(key, fileId);

            log.error("Failed to upload the file on S3");
            throw new FileUploadException("Failed to upload the file on S3");
        }

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("AcceptRanges", response.acceptRanges());
        metadata.put("LastModified", response.lastModified());
        metadata.put("ContentLength", response.contentLength());
        metadata.put("ETag", response.eTag());
        metadata.put("ContentType", response.contentType());
        metadata.put("ServerSideEncryption", response.serverSideEncryptionAsString());

        log.info("Retrieved Metadata: {}", metadata);

        return metadata;
    }

    /**
     * Method to upload S3 object
     *
     * @param file to be uploaded
     * @return object url
     */
    private String uploadObjectToS3(MultipartFile file, UUID fileId) {
        String key = fileId + "/" + file.getOriginalFilename();

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                                                            .bucket(bucketName)
                                                            .key(key)
                                                            .build();

        try {
            s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
        } catch (IOException e) {
            log.error("Failed to upload the file on S3 with id {}", fileId);
            throw new FileUploadException("Failed to upload the file on S3 with id " + fileId);
        } catch (SdkClientException e) {
            log.error("S3 is unavailable. Upload failed for file ID: {}", fileId);
            throw new DatabaseConnectionException("S3 service is currently unavailable. Please try again later.");
        }

        log.info("S3 object uploaded successfully");
        return bucketName + "/" + key;
    }

    /**
     * Method to delete S3 object for given Id
     *
     * @param id of S3 object
     */
    @Override
    public void deleteObject(String id) {
        log.info("Deleting the S3 object with id {}...", id);

        Optional<S3ObjectEntity> entity = findS3Object(id);

        if (entity.isEmpty()) {
            throw new S3ObjectNotFoundException("Unable to find the object with id " + id);
        }

        deleteS3Object(entity.get().getUrl().substring(bucketName.length() + 1), entity.get().getObjectId());

        try {
            repository.delete(entity.get());
        } catch (CannotCreateTransactionException | InvalidDataAccessResourceUsageException |
                 DataIntegrityViolationException | DataAccessResourceFailureException |
                 PersistenceException ex) {
            log.error("Failed to delete the S3 object metadata");
            throw new DatabaseConnectionException("Failed to delete the S3 object metadata");
        } catch (Exception ex) {
        log.error("Unexpected error occurred: {}", ex.getMessage());
        throw new DatabaseConnectionException("Failed to delete the S3 object metadata");
        }
    }

    /**
     * Method to delete S3 object
     *
     * @param key containing object key
     * @param objectId of S3 bucket
     */
    private void deleteS3Object(String key, String objectId) {
        DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                                                                     .bucket(bucketName)
                                                                     .key(key)
                                                                     .build();

        try {
            s3Client.headObject(HeadObjectRequest.builder().bucket(bucketName).key(key).build());
            s3Client.deleteObject(deleteObjectRequest);
        } catch (SdkClientException e) {
            log.error("S3 is down! Cannot delete file: {}", objectId);
            throw new DatabaseConnectionException("S3 service is down. Please try again later.");
        } catch (NoSuchKeyException  e) {
            log.warn("S3 object already deleted or does not exist: {}", objectId);
        } catch (Exception e) {
            log.error("Unexpected issue while deleting S3 object: {}", objectId);
        }
    }
}