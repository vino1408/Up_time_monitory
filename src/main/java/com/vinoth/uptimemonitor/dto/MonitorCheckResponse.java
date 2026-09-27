package com.vinoth.uptimemonitor.dto;

import java.time.LocalDateTime;

public class MonitorCheckResponse {

    private Long id;
    private LocalDateTime checkedAt;
    private Integer statusCode;
    private Long responseTimeMs;
    private Boolean success;
    private String errorMessage;

    public MonitorCheckResponse() {
    }

    public MonitorCheckResponse(
            Long id,
            LocalDateTime checkedAt,
            Integer statusCode,
            Long responseTimeMs,
            Boolean success,
            String errorMessage) {

        this.id = id;
        this.checkedAt = checkedAt;
        this.statusCode = statusCode;
        this.responseTimeMs = responseTimeMs;
        this.success = success;
        this.errorMessage = errorMessage;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDateTime getCheckedAt() {
        return checkedAt;
    }

    public void setCheckedAt(LocalDateTime checkedAt) {
        this.checkedAt = checkedAt;
    }

    public Integer getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(Integer statusCode) {
        this.statusCode = statusCode;
    }

    public Long getResponseTimeMs() {
        return responseTimeMs;
    }

    public void setResponseTimeMs(Long responseTimeMs) {
        this.responseTimeMs = responseTimeMs;
    }

    public Boolean getSuccess() {
        return success;
    }

    public void setSuccess(Boolean success) {
        this.success = success;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}