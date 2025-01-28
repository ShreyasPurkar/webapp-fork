package com.webapp.shreyas_purkar_002325982.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.Instant;

/**
 * Entity class for health check
 */
@Data
@Entity
@Table (name = "health_check")
public class HealthCheckEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long checkId;

    @Column(name = "datetime", nullable = false)
    private Instant dateTime;
}
