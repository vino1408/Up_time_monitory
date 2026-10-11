package com.vinoth.uptimemonitor.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "monitors",
        indexes = {
                @Index(name = "idx_monitors_active", columnList = "active")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class Monitor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 2048)
    private String url;

    @Column(nullable = false, length = 10)
    private String method = "GET";

    @Column(name = "interval_seconds", nullable = false)
    private Integer intervalSeconds = 60;

    @Column(name = "timeout_seconds", nullable = false)
    private Integer timeoutSeconds = 5;

    @Column(name = "expected_status", nullable = false)
    private Integer expectedStatus = 200;

    @Column(nullable = false)
    private Boolean active = true;

    // V2 monitoring state
    @Column(name = "consecutive_failures", nullable = false)
    private Integer consecutiveFailures = 0;

    @Column(name = "consecutive_successes", nullable = false)
    private Integer consecutiveSuccesses = 0;

    @Column(name = "current_status", nullable = false, length = 20)
    private String currentStatus = "UNKNOWN";

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "last_checked_at")
    private LocalDateTime lastCheckedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}