package com.vinoth.uptimemonitor.controller;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.vinoth.uptimemonitor.dto.AlertResponse;
import com.vinoth.uptimemonitor.dto.MonitorCheckResponse;
import com.vinoth.uptimemonitor.dto.MonitorRequest;
import com.vinoth.uptimemonitor.dto.MonitorStatsResponse;
import com.vinoth.uptimemonitor.dto.PagedResponse;
import com.vinoth.uptimemonitor.entity.Monitor;
import com.vinoth.uptimemonitor.entity.MonitorCheck;
import com.vinoth.uptimemonitor.repository.MonitorCheckRepository;
import com.vinoth.uptimemonitor.repository.MonitorRepository;
import com.vinoth.uptimemonitor.service.AlertService;
import com.vinoth.uptimemonitor.service.MonitorService;
import com.vinoth.uptimemonitor.service.MonitoringService;

import jakarta.validation.Valid;

@RestController
@CrossOrigin(origins = "http://localhost:5173")
@RequestMapping("/api/monitors")
public class MonitorController {

    private final MonitorService monitorService;
    private final MonitoringService monitoringService;
    private final MonitorCheckRepository monitorCheckRepository;
    private final MonitorRepository monitorRepository;
    private final AlertService alertService;

    public MonitorController(
            MonitorService monitorService,
            MonitoringService monitoringService,
            MonitorCheckRepository monitorCheckRepository,
            MonitorRepository monitorRepository,
            AlertService alertService) {

        this.monitorService = monitorService;
        this.monitoringService = monitoringService;
        this.monitorCheckRepository = monitorCheckRepository;
        this.monitorRepository = monitorRepository;
        this.alertService = alertService;
    }

    // CREATE MONITOR
    @PostMapping
    public ResponseEntity<Monitor> createMonitor(
            @Valid @RequestBody MonitorRequest request) {

        Monitor monitor = new Monitor();
        monitor.setName(request.getName());
        monitor.setUrl(request.getUrl());
        monitor.setMethod(request.getMethod());
        monitor.setIntervalSeconds(request.getIntervalSeconds());
        monitor.setTimeoutSeconds(request.getTimeoutSeconds());
        monitor.setExpectedStatus(request.getExpectedStatus());
        monitor.setActive(request.getActive());

        Monitor createdMonitor = monitorService.createMonitor(monitor);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(createdMonitor);
    }

    // GET ALL MONITORS WITH PAGINATION
    @GetMapping
    public ResponseEntity<PagedResponse<Monitor>> getAllMonitors(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        validatePagination(page, size);

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by("id").ascending()
        );

        Page<Monitor> result = monitorRepository.findAll(pageable);

        return ResponseEntity.ok(toPagedResponse(result));
    }

    // GET ACTIVE MONITORS WITH PAGINATION
    @GetMapping("/active")
    public ResponseEntity<PagedResponse<Monitor>> getActiveMonitors(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        validatePagination(page, size);

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by("id").ascending()
        );

        Page<Monitor> result =
                monitorRepository.findByActiveTrue(pageable);

        return ResponseEntity.ok(toPagedResponse(result));
    }

    // VALIDATE PAGINATION PARAMETERS
    private void validatePagination(int page, int size) {
        if (page < 0) {
            throw new IllegalArgumentException(
                    "Page must be zero or greater"
            );
        }

        if (size < 1 || size > 100) {
            throw new IllegalArgumentException(
                    "Page size must be between 1 and 100"
            );
        }
    }

    // CONVERT PAGE TO PAGED RESPONSE
    private <T> PagedResponse<T> toPagedResponse(Page<T> result) {
        return new PagedResponse<>(
                result.getContent(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages(),
                result.isFirst(),
                result.isLast()
        );
    }

    // GET MONITOR BY ID
    @GetMapping("/{id}")
    public ResponseEntity<Monitor> getMonitorById(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                monitorService.getMonitorById(id)
        );
    }

    // UPDATE MONITOR
    @PutMapping("/{id}")
    public ResponseEntity<Monitor> updateMonitor(
            @PathVariable Long id,
            @Valid @RequestBody MonitorRequest request) {

        Monitor monitor = new Monitor();
        monitor.setName(request.getName());
        monitor.setUrl(request.getUrl());
        monitor.setMethod(request.getMethod());
        monitor.setIntervalSeconds(request.getIntervalSeconds());
        monitor.setTimeoutSeconds(request.getTimeoutSeconds());
        monitor.setExpectedStatus(request.getExpectedStatus());
        monitor.setActive(request.getActive());

        return ResponseEntity.ok(
                monitorService.updateMonitor(id, monitor)
        );
    }

    // DELETE MONITOR
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMonitor(
            @PathVariable Long id) {

        monitorService.deleteMonitor(id);

        return ResponseEntity.noContent().build();
    }

    // MANUALLY CHECK MONITOR
    @PostMapping("/{id}/check")
    public ResponseEntity<MonitorCheck> checkMonitor(
            @PathVariable Long id) {

        Monitor monitor = monitorService.getMonitorById(id);

        MonitorCheck result =
                monitoringService.checkMonitor(monitor);

        return ResponseEntity.ok(result);
    }

    // GET MONITOR CHECK HISTORY WITH PAGINATION AND FILTERING
    @GetMapping("/{id}/checks")
    public ResponseEntity<PagedResponse<MonitorCheckResponse>>
            getMonitorChecks(
                    @PathVariable Long id,
                    @RequestParam(defaultValue = "0") int page,
                    @RequestParam(defaultValue = "10") int size,
                    @RequestParam(required = false) Boolean success) {

        validatePagination(page, size);

        // Verify that the monitor exists
        monitorService.getMonitorById(id);

        // Fetch newest checks first
        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by("checkedAt").descending()
        );

        Page<MonitorCheck> result;

        if (success == null) {
            // No filter: return all checks
            result = monitorCheckRepository
                    .findByMonitorIdOrderByCheckedAtDesc(id, pageable);
        } else {
            // Filter by successful or failed checks
            result = monitorCheckRepository
                    .findByMonitorIdAndSuccessOrderByCheckedAtDesc(
                            id,
                            success,
                            pageable
                    );
        }

        // Convert entities into response DTOs
        Page<MonitorCheckResponse> responsePage =
                result.map(check -> new MonitorCheckResponse(
                        check.getId(),
                        check.getCheckedAt(),
                        check.getStatusCode(),
                        check.getResponseTimeMs(),
                        check.getSuccess(),
                        check.getErrorMessage()
                ));

        return ResponseEntity.ok(toPagedResponse(responsePage));
    }

    // GET MONITOR STATISTICS
    @GetMapping("/{id}/stats")
    public ResponseEntity<MonitorStatsResponse> getMonitorStats(
            @PathVariable Long id) {

        Monitor monitor = monitorService.getMonitorById(id);

        return ResponseEntity.ok(
                monitoringService.getMonitorStats(monitor)
        );
    }

    // GET LAST 24 HOURS STATISTICS
    @GetMapping("/{id}/stats/24h")
    public ResponseEntity<MonitorStatsResponse>
            getLast24HoursStats(
                    @PathVariable Long id) {

        Monitor monitor = monitorService.getMonitorById(id);

        return ResponseEntity.ok(
                monitoringService.getLast24HoursStats(monitor)
        );
    }

    // GET MONITOR ALERTS
    @GetMapping("/{id}/alerts")
    public ResponseEntity<List<AlertResponse>> getMonitorAlerts(
            @PathVariable Long id) {

        monitorService.getMonitorById(id);

        List<AlertResponse> alerts =
                alertService.getMonitorAlerts(id)
                        .stream()
                        .map(alert -> new AlertResponse(
                                alert.getId(),
                                alert.getMonitor().getId(),
                                alert.getMonitor().getName(),
                                alert.getMessage(),
                                alert.getStatus(),
                                alert.getCreatedAt(),
                                alert.getResolvedAt()
                        ))
                        .toList();

        return ResponseEntity.ok(alerts);
    }
}