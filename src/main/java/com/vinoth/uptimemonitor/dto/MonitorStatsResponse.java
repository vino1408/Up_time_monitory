package com.vinoth.uptimemonitor.dto;

public class MonitorStatsResponse {

    private Long monitorId;
    private String monitorName;
    private String status;

    private long totalChecks;
    private long successfulChecks;
    private long failedChecks;

    private double uptimePercentage;
    private double averageResponseTimeMs;

    private Long lastResponseTimeMs;

    public MonitorStatsResponse() {
    }

    public MonitorStatsResponse(
            Long monitorId,
            String monitorName,
            String status,
            long totalChecks,
            long successfulChecks,
            long failedChecks,
            double uptimePercentage,
            double averageResponseTimeMs,
            Long lastResponseTimeMs) {

        this.monitorId = monitorId;
        this.monitorName = monitorName;
        this.status = status;
        this.totalChecks = totalChecks;
        this.successfulChecks = successfulChecks;
        this.failedChecks = failedChecks;
        this.uptimePercentage = uptimePercentage;
        this.averageResponseTimeMs = averageResponseTimeMs;
        this.lastResponseTimeMs = lastResponseTimeMs;
    }

    public Long getMonitorId() {
        return monitorId;
    }

    public void setMonitorId(Long monitorId) {
        this.monitorId = monitorId;
    }

    public String getMonitorName() {
        return monitorName;
    }

    public void setMonitorName(String monitorName) {
        this.monitorName = monitorName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public long getTotalChecks() {
        return totalChecks;
    }

    public void setTotalChecks(long totalChecks) {
        this.totalChecks = totalChecks;
    }

    public long getSuccessfulChecks() {
        return successfulChecks;
    }

    public void setSuccessfulChecks(long successfulChecks) {
        this.successfulChecks = successfulChecks;
    }

    public long getFailedChecks() {
        return failedChecks;
    }

    public void setFailedChecks(long failedChecks) {
        this.failedChecks = failedChecks;
    }

    public double getUptimePercentage() {
        return uptimePercentage;
    }

    public void setUptimePercentage(double uptimePercentage) {
        this.uptimePercentage = uptimePercentage;
    }

    public double getAverageResponseTimeMs() {
        return averageResponseTimeMs;
    }

    public void setAverageResponseTimeMs(double averageResponseTimeMs) {
        this.averageResponseTimeMs = averageResponseTimeMs;
    }

    public Long getLastResponseTimeMs() {
        return lastResponseTimeMs;
    }

    public void setLastResponseTimeMs(Long lastResponseTimeMs) {
        this.lastResponseTimeMs = lastResponseTimeMs;
    }
}