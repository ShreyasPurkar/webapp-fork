package com.webapp.shreyas_purkar_002325982.rest.resource;

import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Interface to handle health check API request
 */
public interface HealthCheckApi {

    /**
     * API to monitor health of application instance
     */
    @GetMapping("/healthz")
    ResponseEntity<?> healthCheck(@RequestBody(required = false) String payload) throws HttpRequestMethodNotSupportedException;

    @GetMapping("/cicd")
    ResponseEntity<?> healthCheckCicd(@RequestBody(required = false) String payload) throws HttpRequestMethodNotSupportedException;
}
