package com.webapp.shreyas_purkar_002325982.repository;

import com.webapp.shreyas_purkar_002325982.entity.HealthCheckEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository to store health check data
 */
@Repository
public interface HealthCheckRepository extends JpaRepository<HealthCheckEntity, Long> {
}
