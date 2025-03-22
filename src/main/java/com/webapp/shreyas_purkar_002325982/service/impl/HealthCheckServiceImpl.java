package com.webapp.shreyas_purkar_002325982.service.impl;

import com.webapp.shreyas_purkar_002325982.entity.HealthCheckEntity;
import com.webapp.shreyas_purkar_002325982.exception.DatabaseConnectionException;
import com.webapp.shreyas_purkar_002325982.repository.HealthCheckRepository;
import com.webapp.shreyas_purkar_002325982.service.HealthCheckService;
import jakarta.persistence.PersistenceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.InvalidDataAccessResourceUsageException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.CannotCreateTransactionException;

import java.time.Instant;

/**
 * Service class implementation to monitor health of application instance
 */
@Service
public class HealthCheckServiceImpl implements HealthCheckService {

    private static final Logger log = LoggerFactory.getLogger(HealthCheckServiceImpl.class);

    @Autowired
    HealthCheckRepository repository;

    /**
     * Method to monitor health of application instance
     */
    @Override
    public void healthCheck() {
        HealthCheckEntity entity = new HealthCheckEntity();
        entity.setDateTime(Instant.now());

        try {
           repository.save(entity);
            log.info("Health check successful: {}", entity);
        } catch (CannotCreateTransactionException | InvalidDataAccessResourceUsageException |
                 DataIntegrityViolationException | DataAccessResourceFailureException |
                 PersistenceException ex) {
            log.error("Health check failed. Error: {}", ex.getMessage());
            throw new DatabaseConnectionException("Failed to persist health check log");
        } catch (Exception ex) {
            log.error("Unexpected error during health check. Error:{}", ex.getMessage());
            throw new DatabaseConnectionException("Failed to persist health check log");
        }
    }
}