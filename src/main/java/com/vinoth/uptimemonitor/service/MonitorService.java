
package com.vinoth.uptimemonitor.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vinoth.uptimemonitor.entity.Monitor;
import com.vinoth.uptimemonitor.exception.MonitorNotFoundException;
import com.vinoth.uptimemonitor.repository.MonitorCheckRepository;
import com.vinoth.uptimemonitor.repository.MonitorRepository;

@Service
@Transactional
public class MonitorService {

    private final MonitorRepository monitorRepository;
    private final MonitorCheckRepository monitorCheckRepository;
    private final MonitorUrlValidator urlValidator =
            new MonitorUrlValidator();

    public MonitorService(
            MonitorRepository monitorRepository,
            MonitorCheckRepository monitorCheckRepository) {

        this.monitorRepository = monitorRepository;
        this.monitorCheckRepository = monitorCheckRepository;
    }

    // CREATE
    public Monitor createMonitor(Monitor monitor) {
        validateMonitor(monitor);
        return monitorRepository.save(monitor);
    }

    // GET ALL
    @Transactional(readOnly = true)
    public List<Monitor> getAllMonitors() {
        return monitorRepository.findAll();
    }

    // GET ACTIVE
    @Transactional(readOnly = true)
    public List<Monitor> getActiveMonitors() {
        return monitorRepository.findByActiveTrue();
    }

    // GET BY ID
    @Transactional(readOnly = true)
    public Monitor getMonitorById(Long id) {
        return monitorRepository.findById(id)
                .orElseThrow(() -> new MonitorNotFoundException(id));
    }

    // UPDATE
    public Monitor updateMonitor(Long id, Monitor updatedMonitor) {
        validateMonitor(updatedMonitor);

        Monitor existingMonitor = getMonitorById(id);

        existingMonitor.setName(updatedMonitor.getName().trim());
        existingMonitor.setUrl(updatedMonitor.getUrl().trim());
        existingMonitor.setMethod(
                updatedMonitor.getMethod().trim().toUpperCase(Locale.ROOT));
        existingMonitor.setIntervalSeconds(
                updatedMonitor.getIntervalSeconds());
        existingMonitor.setTimeoutSeconds(
                updatedMonitor.getTimeoutSeconds());
        existingMonitor.setExpectedStatus(
                updatedMonitor.getExpectedStatus());
        existingMonitor.setActive(updatedMonitor.getActive());

        return monitorRepository.save(existingMonitor);
    }

    // DELETE
    public void deleteMonitor(Long id) {
        Monitor monitor = getMonitorById(id);

        monitorCheckRepository.deleteAllByMonitorId(id);
        monitorCheckRepository.flush();

        monitorRepository.delete(monitor);
        monitorRepository.flush();
    }

    // UPDATE LAST CHECKED TIME
    public void updateLastCheckedAt(Long id, LocalDateTime checkedAt) {
        Monitor monitor = getMonitorById(id);
        monitor.setLastCheckedAt(checkedAt);
        monitorRepository.save(monitor);
    }

    // VALIDATE MONITOR INPUT
    private void validateMonitor(Monitor monitor) {
        if (monitor == null) {
            throw new IllegalArgumentException(
                    "Monitor request body is required.");
        }

        if (monitor.getName() == null
                || monitor.getName().isBlank()) {
            throw new IllegalArgumentException(
                    "Monitor name is required.");
        }

        if (monitor.getName().trim().length() > 100) {
            throw new IllegalArgumentException(
                    "Monitor name must not exceed 100 characters.");
        }

        if (monitor.getUrl() == null
                || monitor.getUrl().isBlank()) {
            throw new IllegalArgumentException(
                    "Monitor URL is required.");
        }

        validateUrl(monitor.getUrl().trim());

        if (monitor.getMethod() == null
                || monitor.getMethod().isBlank()) {
            throw new IllegalArgumentException(
                    "HTTP method is required.");
        }

        String method = monitor.getMethod()
                .trim().toUpperCase(Locale.ROOT);

        if (!method.equals("GET") && !method.equals("HEAD")) {
            throw new IllegalArgumentException(
                    "Only GET and HEAD methods are supported.");
        }

        if (monitor.getIntervalSeconds() == null
                || monitor.getIntervalSeconds() < 10
                || monitor.getIntervalSeconds() > 86400) {
            throw new IllegalArgumentException(
                    "Check interval must be between 10 and 86400 seconds.");
        }

        if (monitor.getTimeoutSeconds() == null
                || monitor.getTimeoutSeconds() < 1
                || monitor.getTimeoutSeconds() > 60) {
            throw new IllegalArgumentException(
                    "Timeout must be between 1 and 60 seconds.");
        }

        if (monitor.getExpectedStatus() == null
                || monitor.getExpectedStatus() < 100
                || monitor.getExpectedStatus() > 599) {
            throw new IllegalArgumentException(
                    "Expected HTTP status must be between 100 and 599.");
        }
    }

    // URL VALIDATION
    private void validateUrl(String url) {
        urlValidator.validate(url);
    }
}
