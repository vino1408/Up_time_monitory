package com.vinoth.uptimemonitor.scheduler;

import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.vinoth.uptimemonitor.entity.Monitor;
import com.vinoth.uptimemonitor.service.MonitorService;
import com.vinoth.uptimemonitor.service.MonitoringService;

@Component
public class MonitoringScheduler {

    private static final Logger logger =
            LoggerFactory.getLogger(MonitoringScheduler.class);

    private final MonitorService monitorService;
    private final MonitoringService monitoringService;

    public MonitoringScheduler(
            MonitorService monitorService,
            MonitoringService monitoringService) {

        this.monitorService = monitorService;
        this.monitoringService = monitoringService;
    }

    /*
     * Run the scheduling loop every second.
     * Each monitor is checked according to its interval.
     */
    @Scheduled(fixedDelay = 1000)
    public void checkActiveMonitors() {

        List<Monitor> monitors =
                monitorService.getActiveMonitors();

        for (Monitor monitor : monitors) {

            try {
                if (shouldCheckMonitor(monitor)) {

                    monitoringService.checkMonitor(monitor);

                    logger.info(
                            "Monitor checked: {} | Interval: {} seconds",
                            monitor.getName(),
                            monitor.getIntervalSeconds()
                    );
                }

            } catch (Exception exception) {

                logger.error(
                        "Failed to check monitor: {}",
                        monitor.getName(),
                        exception
                );
            }
        }
    }

    private boolean shouldCheckMonitor(Monitor monitor) {

        if (monitor.getLastCheckedAt() == null) {
            return true;
        }

        if (monitor.getIntervalSeconds() == null
                || monitor.getIntervalSeconds() <= 0) {
            return false;
        }

        LocalDateTime nextCheck =
                monitor.getLastCheckedAt().plusSeconds(
                        monitor.getIntervalSeconds()
                );

        return !LocalDateTime.now().isBefore(nextCheck);
    }
}