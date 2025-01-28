package com.webapp.shreyas_purkar_002325982.rest.resource.impl;

import com.webapp.shreyas_purkar_002325982.exception.PayloadNotAllowedException;
import com.webapp.shreyas_purkar_002325982.rest.resource.HealthCheckApi;
import com.webapp.shreyas_purkar_002325982.service.HealthCheckService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * Implementation of HealthCheckApi to handle health check API request
 */
@RestController
public class HealthCheckApiImpl implements HealthCheckApi {

    private static final Logger log = LoggerFactory.getLogger(HealthCheckApiImpl.class);

    @Autowired
    private HttpServletRequest request;

    @Autowired
    HealthCheckService service;

    /**
     * API to monitor health of application instance
     */
    @Override
    public ResponseEntity<?> healthCheck(String payload) {
        log.info("Initializing health check...");

        if (payload != null && !payload.trim().isEmpty()) {
            log.error("Payload not allowed for health check: {}", payload);
            throw new PayloadNotAllowedException();
        }

        if (!request.getParameterMap().isEmpty()) {
            log.error("Query params not allowed for health check");
            throw new PayloadNotAllowedException();
        }

        service.healthCheck();
        return ResponseEntity.ok().build();
    }
}
