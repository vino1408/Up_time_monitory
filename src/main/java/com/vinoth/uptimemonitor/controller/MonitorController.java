package com.vinoth.uptimemonitor.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.vinoth.uptimemonitor.dto.MonitorCheckResponse;
import com.vinoth.uptimemonitor.dto.MonitorStatsResponse;
import com.vinoth.uptimemonitor.entity.Monitor;
import com.vinoth.uptimemonitor.entity.MonitorCheck;
import com.vinoth.uptimemonitor.repository.MonitorCheckRepository;
import com.vinoth.uptimemonitor.service.MonitorService;
import com.vinoth.uptimemonitor.service.MonitoringService;

@RestController
@CrossOrigin(origins = "http://localhost:5173")

@RequestMapping("/api/monitors")
public class MonitorController {

    private final MonitorService monitorService;
    private final MonitoringService monitoringService;
    private final MonitorCheckRepository monitorCheckRepository;

    public MonitorController(
            MonitorService monitorService,
            MonitoringService monitoringService,
            MonitorCheckRepository monitorCheckRepository) {

        this.monitorService = monitorService;
        this.monitoringService = monitoringService;
        this.monitorCheckRepository = monitorCheckRepository;
    }

    // Create a new monitor
    @PostMapping
    public ResponseEntity<Monitor> createMonitor(
            @RequestBody Monitor monitor) {

        Monitor createdMonitor =
                monitorService.createMonitor(monitor);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(createdMonitor);
    }

    // Get all monitors
    @GetMapping
    public ResponseEntity<List<Monitor>> getAllMonitors() {

        return ResponseEntity.ok(
                monitorService.getAllMonitors()
        );
    }

    // Get active monitors
    @GetMapping("/active")
    public ResponseEntity<List<Monitor>> getActiveMonitors() {

        return ResponseEntity.ok(
                monitorService.getActiveMonitors()
        );
    }

    // Get monitor by ID
    @GetMapping("/{id}")
    public ResponseEntity<Monitor> getMonitorById(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                monitorService.getMonitorById(id)
        );
    }

    // Update monitor
    @PutMapping("/{id}")
    public ResponseEntity<Monitor> updateMonitor(
            @PathVariable Long id,
            @RequestBody Monitor monitor) {

        return ResponseEntity.ok(
                monitorService.updateMonitor(id, monitor)
        );
    }

    // Delete monitor
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMonitor(
            @PathVariable Long id) {

        monitorService.deleteMonitor(id);

        return ResponseEntity.noContent().build();
    }

    // Manually check a monitor
    @PostMapping("/{id}/check")
    public ResponseEntity<MonitorCheck> checkMonitor(
            @PathVariable Long id) {

        Monitor monitor =
                monitorService.getMonitorById(id);

        MonitorCheck result =
                monitoringService.checkMonitor(monitor);

        return ResponseEntity.ok(result);
    }

    // Get monitor check history
    @GetMapping("/{id}/checks")
    public ResponseEntity<List<MonitorCheckResponse>> getMonitorChecks(
            @PathVariable Long id) {

        List<MonitorCheckResponse> checks =
                monitorCheckRepository
                        .findByMonitorIdOrderByCheckedAtDesc(id)
                        .stream()
                        .map(check -> new MonitorCheckResponse(
                                check.getId(),
                                check.getCheckedAt(),
                                check.getStatusCode(),
                                check.getResponseTimeMs(),
                                check.getSuccess(),
                                check.getErrorMessage()
                        ))
                        .toList();

        return ResponseEntity.ok(checks);
    }

    // Get monitor statistics
    @GetMapping("/{id}/stats")
    public ResponseEntity<MonitorStatsResponse> getMonitorStats(
            @PathVariable Long id) {

        Monitor monitor =
                monitorService.getMonitorById(id);

        MonitorStatsResponse stats =
                monitoringService.getMonitorStats(monitor);

        return ResponseEntity.ok(stats);
    }
}