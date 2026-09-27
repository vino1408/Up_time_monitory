package com.vinoth.uptimemonitor.scheduler;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.vinoth.uptimemonitor.entity.Monitor;
import com.vinoth.uptimemonitor.service.MonitorService;
import com.vinoth.uptimemonitor.service.MonitoringService;

@Component
public class MonitoringScheduler {

    private final MonitorService monitorService;

    private final MonitoringService monitoringService;

    public MonitoringScheduler(
            MonitorService monitorService,
            MonitoringService monitoringService) {

        this.monitorService =
                monitorService;

        this.monitoringService =
                monitoringService;
    }

    // =========================================================
    // MONITORING LOOP
    // =========================================================

    /*
     * Runs every second.
     *
     * The scheduler itself runs every second,
     * but each monitor is checked only according
     * to its intervalSeconds value.
     */
    @Scheduled(fixedRate = 1000)
    public void checkActiveMonitors() {

        List<Monitor> monitors =
                monitorService
                        .getActiveMonitors();

        for (Monitor monitor : monitors) {

            try {

                if (shouldCheckMonitor(monitor)) {

                    monitoringService
                            .checkMonitor(monitor);

                    System.out.println(
                            "Monitor checked: "
                                    + monitor.getName()
                                    + " | Interval: "
                                    + monitor.getIntervalSeconds()
                                    + " seconds"
                    );
                }

            } catch (Exception exception) {

                System.err.println(
                        "Failed to check monitor: "
                                + monitor.getName()
                                + " - "
                                + exception.getMessage()
                );
            }
        }
    }

    // =========================================================
    // SHOULD CHECK MONITOR
    // =========================================================

    private boolean shouldCheckMonitor(
            Monitor monitor) {

        /*
         * New monitor:
         *
         * If it has never been checked,
         * check it immediately.
         */
        if (monitor.getLastCheckedAt()
                == null) {

            return true;
        }

        /*
         * Invalid interval:
         *
         * Do not execute the check.
         */
        if (monitor.getIntervalSeconds()
                == null
                || monitor.getIntervalSeconds()
                        <= 0) {

            return false;
        }

        /*
         * Calculate the next check time.
         */
        LocalDateTime nextCheck =
                monitor.getLastCheckedAt()
                        .plusSeconds(
                                monitor.getIntervalSeconds()
                        );

        /*
         * Check when current time has
         * reached or passed nextCheck.
         */
        return !LocalDateTime.now()
                .isBefore(nextCheck);
    }
}