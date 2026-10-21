package com.vinoth.uptimemonitor.service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.vinoth.uptimemonitor.dto.MonitorStatsResponse;
import com.vinoth.uptimemonitor.entity.Monitor;
import com.vinoth.uptimemonitor.entity.MonitorCheck;
import com.vinoth.uptimemonitor.repository.MonitorCheckRepository;
import com.vinoth.uptimemonitor.repository.MonitorRepository;

@Service
public class MonitoringService {

    private static final int FAILURE_THRESHOLD = 3;

    private final MonitorCheckRepository monitorCheckRepository;
    private final MonitorRepository monitorRepository;
    private final HttpClient httpClient;
    private final AlertService alertService;
    private final TransactionTemplate transactionTemplate;

    /*
     * One lock per monitor ID.
     * Checks for the same monitor cannot overlap in this application instance.
     */
    private final ConcurrentHashMap<Long, ReentrantLock> monitorLocks =
            new ConcurrentHashMap<>();

    public MonitoringService(
            MonitorCheckRepository monitorCheckRepository,
            MonitorRepository monitorRepository,
            AlertService alertService,
            PlatformTransactionManager transactionManager) {

        this.monitorCheckRepository = monitorCheckRepository;
        this.monitorRepository = monitorRepository;
        this.alertService = alertService;
        this.transactionTemplate =
                new TransactionTemplate(transactionManager);

        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
    }

    // =========================================================
    // CHECK MONITOR
    // =========================================================

    public MonitorCheck checkMonitor(Monitor monitor) {

        if (monitor == null || monitor.getId() == null) {
            throw new IllegalArgumentException(
                    "A saved monitor with a valid ID is required.");
        }

        Long monitorId = monitor.getId();

        ReentrantLock lock = monitorLocks.computeIfAbsent(
                monitorId,
                id -> new ReentrantLock());

        lock.lock();

        try {
            /*
             * The transaction completes before the lock is released.
             * Reload the monitor so counters are not taken from a stale
             * object supplied by the scheduler or controller.
             */
            MonitorCheck result = transactionTemplate.execute(
                    transactionStatus -> performCheck(monitorId));

            if (result == null) {
                throw new IllegalStateException(
                        "Monitor check did not produce a result.");
            }

            return result;

        } finally {
            lock.unlock();
        }
    }

    private MonitorCheck performCheck(Long monitorId) {

        Monitor monitor = monitorRepository.findById(monitorId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Monitor not found: " + monitorId));

        long startTime = System.nanoTime();

        LocalDateTime checkedAt = LocalDateTime.now();

        MonitorCheck check = new MonitorCheck();
        check.setMonitor(monitor);
        check.setCheckedAt(checkedAt);

        boolean success;

        // =====================================================
        // HTTP CHECK
        // =====================================================

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(monitor.getUrl()))
                    .timeout(Duration.ofSeconds(
                            monitor.getTimeoutSeconds()))
                    .method(
                            monitor.getMethod(),
                            HttpRequest.BodyPublishers.noBody())
                    .build();

            HttpResponse<Void> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.discarding());

            long responseTimeMs =
                    (System.nanoTime() - startTime) / 1_000_000;

            int statusCode = response.statusCode();

            success = statusCode == monitor.getExpectedStatus();

            check.setStatusCode(statusCode);
            check.setResponseTimeMs(responseTimeMs);
            check.setSuccess(success);

            if (!success) {
                check.setErrorMessage(
                        "Expected HTTP "
                                + monitor.getExpectedStatus()
                                + " but received HTTP "
                                + statusCode);
            }

        } catch (Exception exception) {

            long responseTimeMs =
                    (System.nanoTime() - startTime) / 1_000_000;

            success = false;

            check.setResponseTimeMs(responseTimeMs);
            check.setSuccess(false);

            String message = exception.getMessage();

            check.setErrorMessage(
                    exception.getClass().getSimpleName()
                            + (message == null || message.isBlank()
                                    ? ""
                                    : ": " + message));
        }

        // =====================================================
        // CONSECUTIVE SUCCESS / FAILURE TRACKING
        // =====================================================

        int consecutiveSuccesses =
                monitor.getConsecutiveSuccesses() == null
                        ? 0
                        : monitor.getConsecutiveSuccesses();

        int consecutiveFailures =
                monitor.getConsecutiveFailures() == null
                        ? 0
                        : monitor.getConsecutiveFailures();

        if (success) {

            monitor.setConsecutiveSuccesses(consecutiveSuccesses + 1);
            monitor.setConsecutiveFailures(0);
            monitor.setCurrentStatus("UP");

        } else {

            monitor.setConsecutiveFailures(consecutiveFailures + 1);
            monitor.setConsecutiveSuccesses(0);

            if (monitor.getConsecutiveFailures() >= FAILURE_THRESHOLD) {
                monitor.setCurrentStatus("DOWN");
            }
        }

        // =====================================================
        // UPDATE LAST CHECKED TIME
        // =====================================================

        monitor.setLastCheckedAt(checkedAt);

        // =====================================================
        // SAVE MONITOR AND CHECK RESULT
        // =====================================================

        monitorRepository.save(monitor);

        MonitorCheck savedCheck = monitorCheckRepository.save(check);

        // =====================================================
        // ALERT PROCESSING
        // =====================================================

        try {
            alertService.processAlert(monitor, success);

        } catch (Exception exception) {
            /*
             * Preserve the existing behavior: alert processing errors
             * are logged without intentionally discarding the check.
             */
            System.err.println(
                    "Alert processing failed for monitor "
                            + monitor.getId()
                            + ": "
                            + exception.getMessage());
        }

        return savedCheck;
    }

    // =========================================================
    // GET MONITOR STATISTICS
    // =========================================================

    public MonitorStatsResponse getMonitorStats(Monitor monitor) {

        Long monitorId = monitor.getId();

        long totalChecks =
                monitorCheckRepository.countByMonitorId(monitorId);

        long successfulChecks =
                monitorCheckRepository
                        .countByMonitorIdAndSuccessTrue(monitorId);

        long failedChecks = totalChecks - successfulChecks;

        double uptimePercentage = 0.0;

        if (totalChecks > 0) {
            uptimePercentage =
                    ((double) successfulChecks / totalChecks) * 100.0;
        }

        List<MonitorCheck> checks =
                monitorCheckRepository
                        .findByMonitorIdOrderByCheckedAtDesc(monitorId);

        double averageResponseTimeMs = 0.0;
        Long lastResponseTimeMs = null;

        if (!checks.isEmpty()) {

            MonitorCheck latestCheck = checks.get(0);
            lastResponseTimeMs = latestCheck.getResponseTimeMs();

            long totalResponseTime = 0;
            long responseTimeCount = 0;

            for (MonitorCheck check : checks) {
                if (check.getResponseTimeMs() != null) {
                    totalResponseTime += check.getResponseTimeMs();
                    responseTimeCount++;
                }
            }

            if (responseTimeCount > 0) {
                averageResponseTimeMs =
                        (double) totalResponseTime / responseTimeCount;
            }
        }

        String status = monitor.getCurrentStatus();

        if (status == null || status.isBlank()) {
            status = "NO_DATA";
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
                lastResponseTimeMs);
    }

    // =========================================================
    // GET LAST 24 HOURS STATISTICS
    // =========================================================

    public MonitorStatsResponse getLast24HoursStats(Monitor monitor) {

        Long monitorId = monitor.getId();

        LocalDateTime end = LocalDateTime.now();
        LocalDateTime start = end.minusHours(24);

        List<MonitorCheck> checks =
                monitorCheckRepository
                        .findByMonitorIdAndCheckedAtBetween(
                                monitorId, start, end);

        long totalChecks = checks.size();

        long successfulChecks = checks.stream()
                .filter(check -> Boolean.TRUE.equals(check.getSuccess()))
                .count();

        long failedChecks = totalChecks - successfulChecks;

        double uptimePercentage = 0.0;

        if (totalChecks > 0) {
            uptimePercentage =
                    ((double) successfulChecks / totalChecks) * 100.0;
        }

        double averageResponseTimeMs = 0.0;
        Long lastResponseTimeMs = null;

        if (!checks.isEmpty()) {

            MonitorCheck latestCheck = checks.stream()
                    .max(Comparator.comparing(MonitorCheck::getCheckedAt))
                    .orElse(null);

            if (latestCheck != null) {
                lastResponseTimeMs = latestCheck.getResponseTimeMs();
            }

            long totalResponseTime = 0;
            long responseTimeCount = 0;

            for (MonitorCheck check : checks) {
                if (check.getResponseTimeMs() != null) {
                    totalResponseTime += check.getResponseTimeMs();
                    responseTimeCount++;
                }
            }

            if (responseTimeCount > 0) {
                averageResponseTimeMs =
                        (double) totalResponseTime / responseTimeCount;
            }
        }

        String status = monitor.getCurrentStatus();

        if (status == null) {
            status = "NO_DATA";
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
                lastResponseTimeMs);
    }

    // =========================================================
    // GET LAST 7 DAYS STATISTICS
    // =========================================================

    public MonitorStatsResponse getLast7DaysStats(Monitor monitor) {

        Long monitorId = monitor.getId();

        LocalDateTime end = LocalDateTime.now();
        LocalDateTime start = end.minusDays(7);

        List<MonitorCheck> checks =
                monitorCheckRepository
                        .findByMonitorIdAndCheckedAtBetween(
                                monitorId, start, end);

        long totalChecks = checks.size();

        long successfulChecks = checks.stream()
                .filter(check -> Boolean.TRUE.equals(check.getSuccess()))
                .count();

        long failedChecks = totalChecks - successfulChecks;

        double uptimePercentage = 0.0;

        if (totalChecks > 0) {
            uptimePercentage =
                    ((double) successfulChecks / totalChecks) * 100.0;
        }

        double averageResponseTimeMs = 0.0;
        Long lastResponseTimeMs = null;

        if (!checks.isEmpty()) {

            MonitorCheck latestCheck = checks.stream()
                    .max(Comparator.comparing(MonitorCheck::getCheckedAt))
                    .orElse(null);

            if (latestCheck != null) {
                lastResponseTimeMs = latestCheck.getResponseTimeMs();
            }

            long totalResponseTime = 0;
            long responseTimeCount = 0;

            for (MonitorCheck check : checks) {
                if (check.getResponseTimeMs() != null) {
                    totalResponseTime += check.getResponseTimeMs();
                    responseTimeCount++;
                }
            }

            if (responseTimeCount > 0) {
                averageResponseTimeMs =
                        (double) totalResponseTime / responseTimeCount;
            }
        }

        String status = monitor.getCurrentStatus();

        if (status == null) {
            status = "NO_DATA";
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
                lastResponseTimeMs);
    }
}