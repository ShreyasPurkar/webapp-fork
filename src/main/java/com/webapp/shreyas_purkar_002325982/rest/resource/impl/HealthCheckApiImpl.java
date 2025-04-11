package com.webapp.shreyas_purkar_002325982.rest.resource.impl;

import com.webapp.shreyas_purkar_002325982.exception.PayloadNotAllowedException;
import com.webapp.shreyas_purkar_002325982.rest.resource.HealthCheckApi;
import com.webapp.shreyas_purkar_002325982.service.HealthCheckService;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpRequestMethodNotSupportedException;
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

    @Autowired
    MeterRegistry meterRegistry;

    /**
     * API to monitor health of application instance
     */
    @Override
    public ResponseEntity<?> healthCheck(String payload) {
        meterRegistry.counter("api.healthcheck.count").increment();
        Timer.Sample healthCheckApiTimer = Timer.start(meterRegistry);

        log.info("Initializing health check for webapp...");

        try {
            if (payload != null && !payload.trim().isEmpty()) {
                log.warn("Payload not allowed for health check: {}", payload);
                throw new PayloadNotAllowedException();
            }

            if (!request.getParameterMap().isEmpty()) {
                log.warn("Query params not allowed for health check");
                throw new PayloadNotAllowedException();
            }

            service.healthCheck();

            return ResponseEntity.ok().build();
        } finally {
            healthCheckApiTimer.stop(meterRegistry.timer("api.healthcheck.time"));
        }
    }

    @Override
    public ResponseEntity<?> healthCheckCicd(String payload) throws HttpRequestMethodNotSupportedException {
        return healthCheck(payload);
    }
}
