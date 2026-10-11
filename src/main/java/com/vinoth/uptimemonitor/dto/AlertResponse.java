package com.vinoth.uptimemonitor.dto;

import java.time.LocalDateTime;

public class AlertResponse {

    private Long id;
    private Long monitorId;
    private String monitorName;
    private String message;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime resolvedAt;

    public AlertResponse(
            Long id,
            Long monitorId,
            String monitorName,
            String message,
            String status,
            LocalDateTime createdAt,
            LocalDateTime resolvedAt) {

        this.id = id;
        this.monitorId = monitorId;
        this.monitorName = monitorName;
        this.message = message;
        this.status = status;
        this.createdAt = createdAt;
        this.resolvedAt = resolvedAt;
    }

    public Long getId() {
        return id;
    }

    public Long getMonitorId() {
        return monitorId;
    }

    public String getMonitorName() {
        return monitorName;
    }

    public String getMessage() {
        return message;
    }

    public String getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getResolvedAt() {
        return resolvedAt;
    }
}