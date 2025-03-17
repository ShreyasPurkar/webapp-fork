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
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
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
//        AwsBasicCredentials awsCreds = AwsBasicCredentials.create(
//                "AKIARZ5BNEVFNOPBBHCW",
//                "uAxgRai+4Jxj7XlQcZHRDRHMhlwd7cPU+cpXEqbq"
//        );
        this.s3Client = S3Client.builder()
                .region(Region.of(region))
//                .credentialsProvider(StaticCredentialsProvider.create(awsCreds))
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
            throw new DatabaseConnectionException("");
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

        S3ObjectEntity entity = new S3ObjectEntity();
        entity.setObjectId(fileId.toString());
        entity.setUrl(url);
        entity.setFileName(file.getOriginalFilename());
        entity.setUploadDate(Instant.now());

        try {
            repository.save(entity);
            log.info("Successfully persisted metadata in database");
        } catch (CannotCreateTransactionException | InvalidDataAccessResourceUsageException |
                 DataIntegrityViolationException | DataAccessResourceFailureException |
                 PersistenceException ex) {
            log.error("Failed to persist the S3 object metadata");
            throw new DatabaseConnectionException("Failed to persist the S3 object metadata");
        } catch (Exception ex) {
            log.error("Unexpected error occurred: {}", ex.getMessage());
            throw new DatabaseConnectionException("Failed to persist the S3 object metadata");
        }

        S3ObjectDto dto = new S3ObjectDto();
        dto.setFileName(entity.getFileName());
        dto.setUrl(entity.getUrl());
        dto.setObjectId(entity.getObjectId());
        dto.setUploadDate(entity.getUploadDate());

        return dto;
    }

    /**
     * Method to upload S3 object
     *
     * @param file to be uploaded
     * @return object url
     */
    private String uploadObjectToS3(MultipartFile file, UUID fileId) {
        String key = fileId + "/" + file.getOriginalFilename();

        Map<String, String> metadata = new HashMap<>();
        metadata.put("Content-Type", file.getContentType());
        metadata.put("Content-Length", String.valueOf(file.getSize()));

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                                                            .bucket(bucketName)
                                                            .key(key)
                                                            .metadata(metadata)
                                                            .build();

        try {
            s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
        } catch (IOException e) {
            log.error("Failed to upload the file on S3 with id {}", fileId);
            throw new FileUploadException("Failed to upload the S3 object metadata");
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
        Optional<S3ObjectEntity> entity = findS3Object(id);

        if (entity.isEmpty()) {
            throw new S3ObjectNotFoundException("Unable to find the object with id " + id);
        }

        deleteS3Object(entity);

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
     * @param entity containing object metadata
     */
    private void deleteS3Object(Optional<S3ObjectEntity> entity) {
        DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                                                                     .bucket(bucketName)
                                                                     .key(entity.get().getUrl().substring(bucketName.length() + 1))
                                                                     .build();

        s3Client.deleteObject(deleteObjectRequest);
    }
}