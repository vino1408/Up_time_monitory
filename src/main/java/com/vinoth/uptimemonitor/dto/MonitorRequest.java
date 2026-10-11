
package com.vinoth.uptimemonitor.dto;

import com.vinoth.uptimemonitor.validation.ValidMonitorConfiguration;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@ValidMonitorConfiguration
public class MonitorRequest {

    @NotBlank(message = "Name is required")
    @Size(max = 100, message = "Name must not exceed 100 characters")
    private String name;

    @NotBlank(message = "URL is required")
    @Size(max = 2048, message = "URL must not exceed 2048 characters")
    @Pattern(
        regexp = "^https?://.+$",
        message = "URL must start with http:// or https://"
    )
    private String url;

    @NotBlank(message = "HTTP method is required")
    @Pattern(
        regexp = "GET|POST|PUT|PATCH|DELETE|HEAD|OPTIONS",
        message = "Invalid HTTP method"
    )
    private String method = "GET";

    @NotNull(message = "Interval is required")
    @Min(value = 10, message = "Interval must be at least 10 seconds")
    private Integer intervalSeconds = 60;

    @NotNull(message = "Timeout is required")
    @Min(value = 1, message = "Timeout must be at least 1 second")
    private Integer timeoutSeconds = 5;

    @NotNull(message = "Expected status is required")
    @Min(value = 100, message = "Expected status must be between 100 and 599")
    @Max(value = 599, message = "Expected status must be between 100 and 599")
    private Integer expectedStatus = 200;

    @NotNull(message = "Active status is required")
    private Boolean active = true;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public Integer getIntervalSeconds() {
        return intervalSeconds;
    }

    public void setIntervalSeconds(Integer intervalSeconds) {
        this.intervalSeconds = intervalSeconds;
    }

    public Integer getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public void setTimeoutSeconds(Integer timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }

    public Integer getExpectedStatus() {
        return expectedStatus;
    }

    public void setExpectedStatus(Integer expectedStatus) {
        this.expectedStatus = expectedStatus;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }
}
