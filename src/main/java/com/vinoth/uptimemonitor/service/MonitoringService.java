package com.vinoth.uptimemonitor.service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vinoth.uptimemonitor.dto.MonitorStatsResponse;
import com.vinoth.uptimemonitor.entity.Monitor;
import com.vinoth.uptimemonitor.entity.MonitorCheck;
import com.vinoth.uptimemonitor.repository.MonitorCheckRepository;
import com.vinoth.uptimemonitor.repository.MonitorRepository;

@Service
public class MonitoringService {

    private final MonitorCheckRepository monitorCheckRepository;

    private final MonitorRepository monitorRepository;

    private final HttpClient httpClient;

    private final AlertService alertService;

    // =========================================================
    // CONSTRUCTOR
    // =========================================================

    public MonitoringService(
            MonitorCheckRepository monitorCheckRepository,
            MonitorRepository monitorRepository,
            AlertService alertService) {

        this.monitorCheckRepository = monitorCheckRepository;
        this.monitorRepository = monitorRepository;
        this.alertService = alertService;

        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    // =========================================================
    // CHECK MONITOR
    // =========================================================

    @Transactional
    public MonitorCheck checkMonitor(Monitor monitor) {

        long startTime = System.nanoTime();

        MonitorCheck check = new MonitorCheck();

        check.setMonitor(monitor);

        LocalDateTime checkedAt = LocalDateTime.now();

        check.setCheckedAt(checkedAt);

        // =====================================================
        // DEFAULT SUCCESS VALUE
        // =====================================================

        boolean success = false;

        // =====================================================
        // HTTP CHECK
        // =====================================================

        try {

            HttpRequest request =
                    HttpRequest.newBuilder()
                            .uri(
                                    URI.create(
                                            monitor.getUrl()
                                    )
                            )
                            .timeout(
                                    Duration.ofSeconds(
                                            monitor.getTimeoutSeconds()
                                    )
                            )
                            .method(
                                    monitor.getMethod(),
                                    HttpRequest.BodyPublishers.noBody()
                            )
                            .build();

            HttpResponse<Void> response =
                    httpClient.send(
                            request,
                            HttpResponse.BodyHandlers.discarding()
                    );

            // =================================================
            // RESPONSE TIME
            // =================================================

            long responseTimeMs =
                    (System.nanoTime() - startTime)
                            / 1_000_000;

            // =================================================
            // STATUS CODE
            // =================================================

            int statusCode =
                    response.statusCode();

            // =================================================
            // SUCCESS CHECK
            // =================================================

            success =
                    statusCode ==
                            monitor.getExpectedStatus();

            // =================================================
            // SAVE STATUS CODE
            // =================================================

            check.setStatusCode(
                    statusCode
            );

            // =================================================
            // SAVE RESPONSE TIME
            // =================================================

            check.setResponseTimeMs(
                    responseTimeMs
            );

            // =================================================
            // SAVE SUCCESS
            // =================================================

            check.setSuccess(
                    success
            );

            // =================================================
            // ERROR MESSAGE
            // =================================================

            if (!success) {

                check.setErrorMessage(
                        "Expected HTTP "
                                + monitor.getExpectedStatus()
                                + " but received HTTP "
                                + statusCode
                );
            }

        } catch (Exception exception) {

            // =================================================
            // RESPONSE TIME WHEN REQUEST FAILS
            // =================================================

            long responseTimeMs =
                    (System.nanoTime() - startTime)
                            / 1_000_000;

            check.setResponseTimeMs(
                    responseTimeMs
            );

            // =================================================
            // REQUEST FAILED
            // =================================================

            success = false;

            check.setSuccess(false);

            // =================================================
            // ERROR MESSAGE
            // =================================================

            check.setErrorMessage(
                    exception.getClass()
                            .getSimpleName()
                            + ": "
                            + exception.getMessage()
            );
        }

        // =========================================================
        // CONSECUTIVE SUCCESS / FAILURE TRACKING
        // =========================================================

        if (success) {

            // -------------------------------------------------
            // SUCCESS
            // -------------------------------------------------

            monitor.setConsecutiveSuccesses(
                    monitor.getConsecutiveSuccesses() + 1
            );

            // Reset failure counter
            monitor.setConsecutiveFailures(0);

            // Monitor is UP
            monitor.setCurrentStatus("UP");

        } else {

            // -------------------------------------------------
            // FAILURE
            // -------------------------------------------------

            monitor.setConsecutiveFailures(
                    monitor.getConsecutiveFailures() + 1
            );

            // Reset success counter
            monitor.setConsecutiveSuccesses(0);

            // Monitor is DOWN
            monitor.setCurrentStatus("DOWN");
        }

        // =========================================================
        // UPDATE LAST CHECKED TIME
        // =========================================================

        monitor.setLastCheckedAt(
                checkedAt
        );

        // =========================================================
        // SAVE MONITOR
        // =========================================================

        monitorRepository.save(
                monitor
        );

        // =========================================================
        // SAVE CHECK RESULT
        // =========================================================

        MonitorCheck savedCheck =
                monitorCheckRepository.save(
                        check
                );

        // =========================================================
        // ALERT PROCESSING
        // =========================================================

        try {

            alertService.processAlert(
                    monitor,
                    success
            );

        } catch (Exception exception) {

            System.err.println(
                    "Alert processing failed for monitor "
                            + monitor.getId()
                            + ": "
                            + exception.getMessage()
            );
        }

        // =========================================================
        // RETURN CHECK
        // =========================================================

        return savedCheck;
    }

    // =========================================================
    // GET MONITOR STATISTICS
    // =========================================================

    @Transactional(readOnly = true)
    public MonitorStatsResponse getMonitorStats(
            Monitor monitor) {

        Long monitorId =
                monitor.getId();

        // =====================================================
        // TOTAL CHECKS
        // =====================================================

        long totalChecks =
                monitorCheckRepository
                        .countByMonitorId(
                                monitorId
                        );

        // =====================================================
        // SUCCESSFUL CHECKS
        // =====================================================

        long successfulChecks =
                monitorCheckRepository
                        .countByMonitorIdAndSuccessTrue(
                                monitorId
                        );

        // =====================================================
        // FAILED CHECKS
        // =====================================================

        long failedChecks =
                totalChecks -
                        successfulChecks;

        // =====================================================
        // UPTIME PERCENTAGE
        // =====================================================

        double uptimePercentage =
                0.0;

        if (totalChecks > 0) {

            uptimePercentage =
                    ((double) successfulChecks
                            / totalChecks)
                            * 100.0;
        }

        // =====================================================
        // GET ALL CHECKS
        // =====================================================

        List<MonitorCheck> checks =
                monitorCheckRepository
                        .findByMonitorIdOrderByCheckedAtDesc(
                                monitorId
                        );

        // =====================================================
        // RESPONSE TIME
        // =====================================================

        double averageResponseTimeMs =
                0.0;

        Long lastResponseTimeMs =
                null;

        if (!checks.isEmpty()) {

            // -------------------------------------------------
            // LATEST CHECK
            // -------------------------------------------------

            MonitorCheck latestCheck =
                    checks.get(0);

            lastResponseTimeMs =
                    latestCheck.getResponseTimeMs();

            // -------------------------------------------------
            // CALCULATE AVERAGE
            // -------------------------------------------------

            long totalResponseTime =
                    0;

            long responseTimeCount =
                    0;

            for (MonitorCheck check : checks) {

                if (check.getResponseTimeMs()
                        != null) {

                    totalResponseTime +=
                            check.getResponseTimeMs();

                    responseTimeCount++;
                }
            }

            if (responseTimeCount > 0) {

                averageResponseTimeMs =
                        (double)
                                totalResponseTime
                                / responseTimeCount;
            }
        }

        // =====================================================
        // CURRENT STATUS
        // =====================================================

        String status =
                "NO_DATA";

        if (!checks.isEmpty()) {

            MonitorCheck latestCheck =
                    checks.get(0);

            if (Boolean.TRUE.equals(
                    latestCheck.getSuccess())) {

                status = "UP";

            } else {

                status = "DOWN";
            }
        }

        // =====================================================
        // RETURN STATISTICS
        // =====================================================

        return new MonitorStatsResponse(
                monitorId,
                monitor.getName(),
                status,
                totalChecks,
                successfulChecks,
                failedChecks,
                uptimePercentage,
                averageResponseTimeMs,
                lastResponseTimeMs
        );
    }

    // =========================================================
    // GET LAST 24 HOURS STATISTICS
    // =========================================================

    @Transactional(readOnly = true)
    public MonitorStatsResponse getLast24HoursStats(
            Monitor monitor) {

        Long monitorId =
                monitor.getId();

        LocalDateTime end =
                LocalDateTime.now();

        LocalDateTime start =
                end.minusHours(24);

        // =====================================================
        // GET CHECKS FROM LAST 24 HOURS
        // =====================================================

        List<MonitorCheck> checks =
                monitorCheckRepository
                        .findByMonitorIdAndCheckedAtBetween(
                                monitorId,
                                start,
                                end
                        );

        // =====================================================
        // TOTAL CHECKS
        // =====================================================

        long totalChecks =
                checks.size();

        // =====================================================
        // SUCCESSFUL CHECKS
        // =====================================================

        long successfulChecks =
                checks.stream()
                        .filter(check ->
                                Boolean.TRUE.equals(
                                        check.getSuccess()
                                ))
                        .count();

        // =====================================================
        // FAILED CHECKS
        // =====================================================

        long failedChecks =
                totalChecks -
                        successfulChecks;

        // =====================================================
        // UPTIME PERCENTAGE
        // =====================================================

        double uptimePercentage =
                0.0;

        if (totalChecks > 0) {

            uptimePercentage =
                    ((double) successfulChecks
                            / totalChecks)
                            * 100.0;
        }

        // =====================================================
        // RESPONSE TIME
        // =====================================================

        double averageResponseTimeMs =
                0.0;

        Long lastResponseTimeMs =
                null;

        if (!checks.isEmpty()) {

            // -------------------------------------------------
            // FIND LATEST CHECK
            // -------------------------------------------------

            MonitorCheck latestCheck =
                    checks.stream()
                            .max(
                                    Comparator.comparing(
                                            MonitorCheck::getCheckedAt
                                    )
                            )
                            .orElse(null);

            if (latestCheck != null) {

                lastResponseTimeMs =
                        latestCheck.getResponseTimeMs();
            }

            // -------------------------------------------------
            // CALCULATE AVERAGE RESPONSE TIME
            // -------------------------------------------------

            long totalResponseTime =
                    0;

            long responseTimeCount =
                    0;

            for (MonitorCheck check : checks) {

                if (check.getResponseTimeMs()
                        != null) {

                    totalResponseTime +=
                            check.getResponseTimeMs();

                    responseTimeCount++;
                }
            }

            if (responseTimeCount > 0) {

                averageResponseTimeMs =
                        (double)
                                totalResponseTime
                                / responseTimeCount;
            }
        }

        // =====================================================
        // CURRENT STATUS
        // =====================================================

        String status =
                monitor.getCurrentStatus();

        if (status == null) {

            status = "NO_DATA";
        }

        // =====================================================
        // RETURN STATISTICS
        // =====================================================

        return new MonitorStatsResponse(
                monitorId,
                monitor.getName(),
                status,
                totalChecks,
                successfulChecks,
                failedChecks,
                uptimePercentage,
                averageResponseTimeMs,
                lastResponseTimeMs
        );
    }
    // =========================================================
// GET LAST 7 DAYS STATISTICS
// =========================================================

@Transactional(readOnly = true)
public MonitorStatsResponse getLast7DaysStats(
        Monitor monitor) {

    Long monitorId =
            monitor.getId();

    LocalDateTime end =
            LocalDateTime.now();

    LocalDateTime start =
            end.minusDays(7);

    // =====================================================
    // GET CHECKS FROM LAST 7 DAYS
    // =====================================================

    List<MonitorCheck> checks =
            monitorCheckRepository
                    .findByMonitorIdAndCheckedAtBetween(
                            monitorId,
                            start,
                            end
                    );

    // =====================================================
    // TOTAL CHECKS
    // =====================================================

    long totalChecks =
            checks.size();

    // =====================================================
    // SUCCESSFUL CHECKS
    // =====================================================

    long successfulChecks =
            checks.stream()
                    .filter(check ->
                            Boolean.TRUE.equals(
                                    check.getSuccess()
                            ))
                    .count();

    // =====================================================
    // FAILED CHECKS
    // =====================================================

    long failedChecks =
            totalChecks -
                    successfulChecks;

    // =====================================================
    // UPTIME PERCENTAGE
    // =====================================================

    double uptimePercentage =
            0.0;

    if (totalChecks > 0) {

        uptimePercentage =
                ((double) successfulChecks
                        / totalChecks)
                        * 100.0;
    }

    // =====================================================
    // RESPONSE TIME
    // =====================================================

    double averageResponseTimeMs =
            0.0;

    Long lastResponseTimeMs =
            null;

    if (!checks.isEmpty()) {

        // -------------------------------------------------
        // FIND LATEST CHECK
        // -------------------------------------------------

        MonitorCheck latestCheck =
                checks.stream()
                        .max(
                                Comparator.comparing(
                                        MonitorCheck::getCheckedAt
                                )
                        )
                        .orElse(null);

        if (latestCheck != null) {

            lastResponseTimeMs =
                    latestCheck.getResponseTimeMs();
        }

        // -------------------------------------------------
        // CALCULATE AVERAGE RESPONSE TIME
        // -------------------------------------------------

        long totalResponseTime =
                0;

        long responseTimeCount =
                0;

        for (MonitorCheck check : checks) {

            if (check.getResponseTimeMs()
                    != null) {

                totalResponseTime +=
                        check.getResponseTimeMs();

                responseTimeCount++;
            }
        }

        if (responseTimeCount > 0) {

            averageResponseTimeMs =
                    (double)
                            totalResponseTime
                            / responseTimeCount;
        }
    }

    // =====================================================
    // CURRENT STATUS
    // =====================================================

    String status =
            monitor.getCurrentStatus();

    if (status == null) {

        status = "NO_DATA";
    }

    // =====================================================
    // RETURN STATISTICS
    // =====================================================

    return new MonitorStatsResponse(
            monitorId,
            monitor.getName(),
            status,
            totalChecks,
            successfulChecks,
            failedChecks,
            uptimePercentage,
            averageResponseTimeMs,
            lastResponseTimeMs
    );
}

}