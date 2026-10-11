package com.vinoth.uptimemonitor.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vinoth.uptimemonitor.entity.Alert;
import com.vinoth.uptimemonitor.entity.Monitor;
import com.vinoth.uptimemonitor.repository.AlertRepository;

@Service
@Transactional
public class AlertService {

    private final AlertRepository alertRepository;

    public AlertService(AlertRepository alertRepository) {
        this.alertRepository = alertRepository;
    }

    // =========================================================
    // ALERT THRESHOLD
    // =========================================================

    /*
     * Create a DOWN alert only after
     * 3 consecutive failures.
     */
    private static final int FAILURE_THRESHOLD = 3;

    // =========================================================
    // PROCESS ALERT
    // =========================================================

    public void processAlert(
            Monitor monitor,
            boolean success) {

        // =====================================================
        // SUCCESS
        // =====================================================

        if (success) {

            /*
             * If the monitor has recovered,
             * resolve the currently firing alert.
             */
            if (monitor.getConsecutiveSuccesses() >= 1) {

                resolveDownAlert(monitor);
            }

            return;
        }

        // =====================================================
        // FAILURE
        // =====================================================

        /*
         * Do not create an alert immediately.
         *
         * Wait until the monitor has failed
         * 3 consecutive times.
         */
        if (monitor.getConsecutiveFailures()
                >= FAILURE_THRESHOLD) {

            createDownAlert(monitor);
        }
    }

    // =========================================================
    // CREATE DOWN ALERT
    // =========================================================

    public Alert createDownAlert(
            Monitor monitor) {

        // -----------------------------------------------------
        // Prevent duplicate FIRING alerts
        // -----------------------------------------------------

        OptionalAlertResult result =
                findActiveAlert(
                        monitor.getId()
                );

        if (result.exists()) {

            return result.alert();
        }

        // -----------------------------------------------------
        // Create new alert
        // -----------------------------------------------------

        Alert alert = new Alert();

        alert.setMonitor(
                monitor
        );

        alert.setCreatedAt(
                LocalDateTime.now()
        );

        alert.setMessage(
                "Monitor '"
                        + monitor.getName()
                        + "' is DOWN after "
                        + FAILURE_THRESHOLD
                        + " consecutive failures."
        );

        alert.setStatus(
                "FIRING"
        );

        alert.setResolvedAt(
                null
        );

        return alertRepository.save(
                alert
        );
    }

    // =========================================================
    // RESOLVE DOWN ALERT
    // =========================================================

    public Alert resolveDownAlert(
            Monitor monitor) {

        return alertRepository
                .findFirstByMonitorIdAndStatusOrderByCreatedAtDesc(
                        monitor.getId(),
                        "FIRING"
                )
                .map(alert -> {

                    alert.setStatus(
                            "RESOLVED"
                    );

                    alert.setResolvedAt(
                            LocalDateTime.now()
                    );

                    alert.setMessage(
                            "Monitor '"
                                    + monitor.getName()
                                    + "' has recovered."
                    );

                    return alertRepository.save(
                            alert
                    );
                })
                .orElse(null);
    }

    // =========================================================
    // GET MONITOR ALERTS
    // =========================================================

    @Transactional(readOnly = true)
    public List<Alert> getMonitorAlerts(
            Long monitorId) {

        return alertRepository
                .findByMonitorIdOrderByCreatedAtDesc(
                        monitorId
                );
    }

    // =========================================================
    // GET ACTIVE ALERT
    // =========================================================

    private OptionalAlertResult findActiveAlert(
            Long monitorId) {

        return alertRepository
                .findFirstByMonitorIdAndStatusOrderByCreatedAtDesc(
                        monitorId,
                        "FIRING"
                )
                .map(OptionalAlertResult::new)
                .orElseGet(
                        OptionalAlertResult::empty
                );
    }

    // =========================================================
    // SMALL INTERNAL WRAPPER
    // =========================================================

    private record OptionalAlertResult(
            Alert alert) {

        boolean exists() {

            return alert != null;
        }

        static OptionalAlertResult empty() {

            return new OptionalAlertResult(
                    null
            );
        }
    }
}