package com.vinoth.uptimemonitor.exception;

import java.time.LocalDateTime;
import java.util.Map;

public class ErrorResponse {

    private int status;
    private String error;
    private String message;
    private LocalDateTime timestamp;
    private Map<String, String> errors;

    // Constructor for normal errors
    public ErrorResponse(
            int status,
            String error,
            String message,
            LocalDateTime timestamp) {

        this.status = status;
        this.error = error;
        this.message = message;
        this.timestamp = timestamp;
    }

    // Constructor for validation errors
    public ErrorResponse(
            int status,
            String error,
            String message,
            LocalDateTime timestamp,
            Map<String, String> errors) {

        this.status = status;
        this.error = error;
        this.message = message;
        this.timestamp = timestamp;
        this.errors = errors;
    }

    public int getStatus() {
        return status;
    }

    public String getError() {
        return error;
    }

    public String getMessage() {
        return message;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public Map<String, String> getErrors() {
        return errors;
    }
}