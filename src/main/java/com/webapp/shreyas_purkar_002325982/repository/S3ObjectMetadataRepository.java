package com.webapp.shreyas_purkar_002325982.repository;

import com.webapp.shreyas_purkar_002325982.entity.S3ObjectEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository to store S3 object metadata
 */
@Repository
public interface S3ObjectMetadataRepository extends JpaRepository<S3ObjectEntity, Long> {

    /**
     * Find the S3 object for given object Id
     *
     * @param objectId for object in S3
     * @return S3ObjectEntity
     */
    S3ObjectEntity findByObjectId(String objectId);
}
