package com.vinoth.uptimemonitor.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vinoth.uptimemonitor.entity.Monitor;
import com.vinoth.uptimemonitor.repository.MonitorRepository;

@Service
@Transactional
public class MonitorService {

    private final MonitorRepository monitorRepository;

    public MonitorService(MonitorRepository monitorRepository) {
        this.monitorRepository = monitorRepository;
    }

    public Monitor createMonitor(Monitor monitor) {
        return monitorRepository.save(monitor);
    }

    @Transactional(readOnly = true)
    public List<Monitor> getAllMonitors() {
        return monitorRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Monitor> getActiveMonitors() {
        return monitorRepository.findByActiveTrue();
    }

    @Transactional(readOnly = true)
    public Monitor getMonitorById(Long id) {
        return monitorRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Monitor not found with id: " + id
                        ));
    }

    public Monitor updateMonitor(Long id, Monitor updatedMonitor) {

        Monitor existingMonitor = getMonitorById(id);

        existingMonitor.setName(updatedMonitor.getName());
        existingMonitor.setUrl(updatedMonitor.getUrl());
        existingMonitor.setMethod(updatedMonitor.getMethod());
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

    public void deleteMonitor(Long id) {

        Monitor monitor = getMonitorById(id);

        monitorRepository.delete(monitor);
    }

    public void updateLastCheckedAt(Long id, LocalDateTime checkedAt) {

        Monitor monitor = getMonitorById(id);

        monitor.setLastCheckedAt(checkedAt);

        monitorRepository.save(monitor);
    }
}