package com.vinoth.uptimemonitor.service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
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

    public MonitoringService(
            MonitorCheckRepository monitorCheckRepository,
            MonitorRepository monitorRepository) {

        this.monitorCheckRepository = monitorCheckRepository;
        this.monitorRepository = monitorRepository;

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

        check.setCheckedAt(
                LocalDateTime.now()
        );

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

            long responseTimeMs =
                    (System.nanoTime() - startTime)
                            / 1_000_000;

            int statusCode =
                    response.statusCode();

            boolean success =
                    statusCode ==
                            monitor.getExpectedStatus();

            check.setStatusCode(
                    statusCode
            );

            check.setResponseTimeMs(
                    responseTimeMs
            );

            check.setSuccess(
                    success
            );

            if (!success) {

                check.setErrorMessage(
                        "Expected HTTP "
                                + monitor.getExpectedStatus()
                                + " but received HTTP "
                                + statusCode
                );
            }

        } catch (Exception exception) {

            long responseTimeMs =
                    (System.nanoTime() - startTime)
                            / 1_000_000;

            check.setResponseTimeMs(
                    responseTimeMs
            );

            check.setSuccess(
                    false
            );

            check.setErrorMessage(
                    exception.getClass()
                            .getSimpleName()
                            + ": "
                            + exception.getMessage()
            );
        }

        // =====================================================
        // UPDATE LAST CHECKED TIME
        // =====================================================

        LocalDateTime checkedAt =
                LocalDateTime.now();

        monitor.setLastCheckedAt(
                checkedAt
        );

        /*
         * IMPORTANT:
         *
         * Persist the updated monitor.
         *
         * Without this, lastCheckedAt may not be stored
         * in PostgreSQL because the Monitor can be detached
         * when it reaches this service.
         */
        monitorRepository.save(
                monitor
        );

        // =====================================================
        // SAVE CHECK RESULT
        // =====================================================

        return monitorCheckRepository.save(
                check
        );
    }

    // =========================================================
    // GET MONITOR STATISTICS
    // =========================================================

    @Transactional(readOnly = true)
    public MonitorStatsResponse getMonitorStats(
            Monitor monitor) {

        Long monitorId =
                monitor.getId();

        long totalChecks =
                monitorCheckRepository
                        .countByMonitorId(
                                monitorId
                        );

        long successfulChecks =
                monitorCheckRepository
                        .countByMonitorIdAndSuccessTrue(
                                monitorId
                        );

        long failedChecks =
                totalChecks -
                        successfulChecks;

        double uptimePercentage =
                0.0;

        if (totalChecks > 0) {

            uptimePercentage =
                    ((double) successfulChecks
                            / totalChecks)
                            * 100.0;
        }

        List<MonitorCheck> checks =
                monitorCheckRepository
                        .findByMonitorIdOrderByCheckedAtDesc(
                                monitorId
                        );

        double averageResponseTimeMs =
                0.0;

        Long lastResponseTimeMs =
                null;

        if (!checks.isEmpty()) {

            // Latest check
            MonitorCheck latestCheck =
                    checks.get(0);

            lastResponseTimeMs =
                    latestCheck.getResponseTimeMs();

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