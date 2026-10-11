package com.vinoth.uptimemonitor.service;

import java.time.LocalDateTime;
import java.util.List;

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

    public MonitorService(
            MonitorRepository monitorRepository,
            MonitorCheckRepository monitorCheckRepository) {

        this.monitorRepository = monitorRepository;
        this.monitorCheckRepository = monitorCheckRepository;
    }

    // =========================================================
    // CREATE
    // =========================================================

    public Monitor createMonitor(Monitor monitor) {
        return monitorRepository.save(monitor);
    }

    // =========================================================
    // GET ALL
    // =========================================================

    @Transactional(readOnly = true)
    public List<Monitor> getAllMonitors() {
        return monitorRepository.findAll();
    }

    // =========================================================
    // GET ACTIVE
    // =========================================================

    @Transactional(readOnly = true)
    public List<Monitor> getActiveMonitors() {
        return monitorRepository.findByActiveTrue();
    }

    // =========================================================
    // GET BY ID
    // =========================================================

    @Transactional(readOnly = true)
    public Monitor getMonitorById(Long id) {

        return monitorRepository.findById(id)
                .orElseThrow(() ->
                        new MonitorNotFoundException(id)
                );
    }

    // =========================================================
    // UPDATE
    // =========================================================

    public Monitor updateMonitor(
            Long id,
            Monitor updatedMonitor) {

        Monitor existingMonitor =
                getMonitorById(id);

        existingMonitor.setName(
                updatedMonitor.getName()
        );

        existingMonitor.setUrl(
                updatedMonitor.getUrl()
        );

        existingMonitor.setMethod(
                updatedMonitor.getMethod()
        );

        existingMonitor.setIntervalSeconds(
                updatedMonitor.getIntervalSeconds()
        );

        existingMonitor.setTimeoutSeconds(
                updatedMonitor.getTimeoutSeconds()
        );

        existingMonitor.setExpectedStatus(
                updatedMonitor.getExpectedStatus()
        );

        existingMonitor.setActive(
                updatedMonitor.getActive()
        );

        return monitorRepository.save(existingMonitor);
    }

    // =========================================================
    // DELETE
    // =========================================================

    public void deleteMonitor(Long id) {

        // 1. Make sure monitor exists
        Monitor monitor =
                getMonitorById(id);

        // 2. Delete all monitor checks first
        monitorCheckRepository.deleteAllByMonitorId(id);

        // 3. Flush the delete immediately
        monitorCheckRepository.flush();

        // 4. Delete the monitor
        monitorRepository.delete(monitor);

        // 5. Flush monitor deletion
        monitorRepository.flush();
    }

    // =========================================================
    // UPDATE LAST CHECKED TIME
    // =========================================================

    public void updateLastCheckedAt(
            Long id,
            LocalDateTime checkedAt) {

        Monitor monitor =
                getMonitorById(id);

        monitor.setLastCheckedAt(checkedAt);

        monitorRepository.save(monitor);
    }
}