package com.webapp.shreyas_purkar_002325982.service.impl;

import com.webapp.shreyas_purkar_002325982.dto.S3ObjectDto;
import com.webapp.shreyas_purkar_002325982.entity.S3ObjectEntity;
import com.webapp.shreyas_purkar_002325982.exception.*;
import com.webapp.shreyas_purkar_002325982.repository.S3ObjectMetadataRepository;
import com.webapp.shreyas_purkar_002325982.service.S3Service;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
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
import software.amazon.awssdk.core.exception.SdkException;
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

    @Autowired
    MeterRegistry meterRegistry;

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
        meterRegistry.counter("api.get-object.count").increment();
        Timer.Sample getFileApiTimer = Timer.start(meterRegistry);

        try {
            Optional<S3ObjectEntity> entity = findS3Object(id);

            if (entity.isEmpty()) {
                log.error("No file with Id: {} found in database.", id);
                throw new S3ObjectNotFoundException();
            }

            S3ObjectDto dto = new S3ObjectDto();
            dto.setFileName(entity.get().getFileName());
            dto.setUrl(entity.get().getUrl());
            dto.setObjectId(entity.get().getObjectId());
            dto.setUploadDate(entity.get().getUploadDate());
            dto.setContentType(entity.get().getContentType());
            dto.setContentLength(entity.get().getContentLength());

            log.info("Retrieved file with Id: {} successfully", id);
            return dto;
        } finally {
            getFileApiTimer.stop(meterRegistry.timer("api.get-object.time"));
        }
    }

    /**
     * Method to get S3 object metadata
     *
     * @param id of S3 object
     * @return entity containing S3 object metadata
     */
    private Optional<S3ObjectEntity> findS3Object(String id) {
        log.info("Retrieving file with Id: {} from database...", id);
        Optional<S3ObjectEntity> entity;

        Timer.Sample dbTimer = Timer.start(meterRegistry);

        try {
            entity = Optional.ofNullable(repository.findByObjectId(id));
            return entity;
        } catch (CannotCreateTransactionException | InvalidDataAccessResourceUsageException |
                 DataIntegrityViolationException | DataAccessResourceFailureException |
                 PersistenceException ex) {
            log.error("Failed to retrieve the file with Id: {}. Error: {}", id, ex.getMessage(), ex);
            throw new DatabaseConnectionException();
        } catch (Exception ex) {
            log.error("Unexpected error occurred: {}", ex.getMessage(), ex);
            throw new DatabaseConnectionException();
        } finally {
            dbTimer.stop(meterRegistry.timer("db.query.time"));
        }
    }

    /**
     * Method to create S3 object
     *
     * @param file to upload on S3
     */
    @Override
    public S3ObjectDto uploadObject(MultipartFile file) {
        meterRegistry.counter("api.file-upload-on-s3.count").increment();
        Timer.Sample uploadFileApiTimer = Timer.start(meterRegistry);

        UUID fileId = UUID.randomUUID();
        log.info("UUID of the file is {}", fileId);

        String url = null;


        S3ObjectEntity entity = new S3ObjectEntity();

        try {
            url = uploadObjectToS3(file, fileId);

            Map<String, Object> metadata = getObjectMetadata(url, fileId.toString());

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
            entity.setAwsRequestId(metadata.get("x-amz-request-id").toString());
            entity.setExtendedRequestId(metadata.get("x-amz-id-2").toString());

            Timer.Sample dbTimer = Timer.start(meterRegistry);
            try {
                repository.save(entity);
            } finally {
                dbTimer.stop(meterRegistry.timer("db.query.time"));
            }

            S3ObjectDto dto = new S3ObjectDto();
            dto.setFileName(entity.getFileName());
            dto.setUrl(entity.getUrl());
            dto.setObjectId(entity.getObjectId());
            dto.setUploadDate(entity.getUploadDate());
            dto.setContentType(entity.getContentType());
            dto.setContentLength(entity.getContentLength());

            log.info("Successfully persisted metadata for file with Id: {} in database", fileId);
            return dto;
        } catch (CannotCreateTransactionException | InvalidDataAccessResourceUsageException |
                 DataIntegrityViolationException | DataAccessResourceFailureException |
                 PersistenceException ex) {
            log.error("Failed to persist the metadata for file with Id: {} from S3 bucket: {} at path: {}. Error: {}", fileId, bucketName, url, ex.getMessage(), ex);
            log.info("Deleting the file with Id: {} from S3 bucket: {} at path: {}", fileId, bucketName, url);
            deleteS3Object(url.substring(url.indexOf("/") + 1), fileId.toString());

            throw new DatabaseConnectionException();
        } catch (Exception ex) {
            log.error("Unexpected error occurred: {}", ex.getMessage(), ex);
            log.info("Deleting the file with Id: {} from S3 bucket: {} at path: {}", fileId, bucketName, url);
            deleteS3Object(url.substring(url.indexOf("/") + 1), fileId.toString());

            throw new DatabaseConnectionException();
        } finally {
            uploadFileApiTimer.stop(meterRegistry.timer("api.file-upload-on-s3.time"));
        }
    }

    /**
     * Method to get S3 object system metadata
     *
     * @param url of object in S3
     * @return metadata
     */
    private Map<String, Object> getObjectMetadata(String url, String fileId) {
        log.info("Getting metadata for file with Id: {} from S3 bucket: {}...", fileId, bucketName);

        HeadObjectResponse response;

        String key = url.substring(url.indexOf("/") + 1);

        HeadObjectRequest headObjectRequest = HeadObjectRequest.builder()
                                                               .bucket(bucketName)
                                                               .key(key)
                                                               .build();

        Timer.Sample s3HeadApiTimer = Timer.start(meterRegistry);

        try {
            response = s3Client.headObject(headObjectRequest);

            Map<String, Object> metadata = new HashMap<>();
            metadata.put("AcceptRanges", response.acceptRanges());
            metadata.put("LastModified", response.lastModified());
            metadata.put("ContentLength", response.contentLength());
            metadata.put("ETag", response.eTag());
            metadata.put("ContentType", response.contentType());
            metadata.put("ServerSideEncryption", response.serverSideEncryptionAsString());
            metadata.put("x-amz-request-id", response.responseMetadata().extendedRequestId());
            metadata.put("x-amz-id-2", response.responseMetadata().requestId());

            log.info("Retrieved metadata: {} for file with Id: {} from S3 bucket: {} at path: {}", metadata, fileId, bucketName, key);
            return metadata;
        } catch (SdkClientException e) {
            log.error("Missing AWS credentials. Error: {}", e.getMessage(), e);
            throw new AwsAuthorizationException();
        } catch (SdkException e) {
            log.error("S3 is unavailable. Failed to fetch metadata for file with Id: {} from S3 bucket: {} at path: {}. Error: {}", fileId, bucketName, key, e.getMessage(), e);
            log.info("Deleting the file with Id: {} from S3 bucket: {} at path: {}", fileId, bucketName, key);
            deleteS3Object(key, fileId);

            throw new DatabaseConnectionException();
        } catch (Exception e) {
            log.error("Failed to fetch metadata for file with Id: {} from S3 bucket: {} at path: {}. Error: {}", fileId, bucketName, key, e.getMessage(), e);
            log.info("Deleting the file with Id: {} from S3 bucket: {} at path: {}", fileId, bucketName, key);
            deleteS3Object(key, fileId);

            throw new FetchObjectMetadataException();
        } finally {
            s3HeadApiTimer.stop(meterRegistry.timer("s3.call.time"));
        }
    }

    /**
     * Method to upload S3 object
     *
     * @param file to be uploaded
     * @return object url
     */
    private String uploadObjectToS3(MultipartFile file, UUID fileId) {
        log.info("Uploading file on S3 bucket: {} with id: {}", bucketName, fileId);

        String key = fileId + "/" + file.getOriginalFilename();

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                                                            .bucket(bucketName)
                                                            .key(key)
                                                            .build();

        Timer.Sample s3PutApiTimer = Timer.start(meterRegistry);

        try {
            s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

            log.info("Object with Id: {} uploaded successfully on bucket: {} at path: {}", fileId, bucketName, key);
            return bucketName + "/" + key;
        } catch (IOException e) {
            log.error("Failed to upload the file with Id: {} on S3 bucket: {} at path: {}. Error: {}", fileId, bucketName, key, e.getMessage(), e);
            throw new FileUploadException();
        } catch (SdkClientException e) {
            log.error("Missing AWS credentials. Error: {}", e.getMessage(), e);
            throw new AwsAuthorizationException();
        } catch (SdkException e) {
            log.error("S3 is unavailable. Upload failed for file with Id: {}. on S3 bucket: {} at path: {}. Error: {}", fileId, bucketName, key, e.getMessage(), e);
            throw new DatabaseConnectionException();
        } finally {
            s3PutApiTimer.stop(meterRegistry.timer("s3.call.time"));
        }
    }

    /**
     * Method to delete S3 object for given Id
     *
     * @param id of S3 object
     */
    @Override
    public void deleteObject(String id) {
        meterRegistry.counter("api.delete-file-on-s3.count").increment();
        Timer.Sample deleteFileApiTimer = Timer.start(meterRegistry);

        String key = null;

        try {
            Optional<S3ObjectEntity> entity = findS3Object(id);

            if (entity.isEmpty()) {
                log.error("No file with Id: {} found in database.", id);
                throw new S3ObjectNotFoundException();
            }

            key = entity.get().getUrl().substring(bucketName.length() + 1);

            deleteS3Object(key, entity.get().getObjectId());

            Timer.Sample dbTimer = Timer.start(meterRegistry);
            try {
                repository.delete(entity.get());
            } finally {
                dbTimer.stop(meterRegistry.timer("db.query.time"));
            }
        } catch (CannotCreateTransactionException | InvalidDataAccessResourceUsageException |
                 DataIntegrityViolationException | DataAccessResourceFailureException |
                 PersistenceException ex) {
            log.error("Failed to delete the file with Id: {} on S3 bucket: {} at path: {}. Error: {}", id, bucketName, key, ex.getMessage(), ex);
            throw new DatabaseConnectionException();
        } catch (Exception ex) {
            log.error("Unexpected error occurred: {}", ex.getMessage(), ex);
            throw new DatabaseConnectionException();
        } finally {
            deleteFileApiTimer.stop(meterRegistry.timer("api.delete-file-on-s3.time"));
        }
    }

    /**
     * Method to delete S3 object
     *
     * @param key containing object key
     * @param id of S3 bucket
     */
    private void deleteS3Object(String key, String id) {
        log.info("Deleting file with id {} from S3 bucket: {} at path: {}", id, bucketName, key);
        DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                                                                     .bucket(bucketName)
                                                                     .key(key)
                                                                     .build();

        Timer.Sample s3DeleteApiTimer = Timer.start(meterRegistry);

        try {
            s3Client.headObject(HeadObjectRequest.builder().bucket(bucketName).key(key).build());
            s3Client.deleteObject(deleteObjectRequest);

            log.info("Successfully deleted file with Id: {} from S3 bucket: {} at path: {}", id, bucketName, key);
        } catch (SdkClientException e) {
            log.error("Missing AWS credentials. Error: {}", e.getMessage(), e);
            throw new AwsAuthorizationException();
        } catch (SdkException e) {
            log.error("S3 is unavailable. Failed to delete file with Id: {}. on S3 bucket: {} at path: {}. Error: {}", id, bucketName, key, e.getMessage(), e);
            throw new DatabaseConnectionException();
        } catch (Exception e) {
            log.error("Unexpected issue while deleting file with Id: {} from S3 bucket: {} at path: {}. Error: {}", id, bucketName, key, e.getMessage(), e);
            throw new FileDeletionException();
        } finally {
            s3DeleteApiTimer.stop(meterRegistry.timer("s3.call.time"));
        }
    }
}