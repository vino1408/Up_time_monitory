
package com.vinoth.uptimemonitor.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.InOrder;

import com.vinoth.uptimemonitor.entity.Monitor;
import com.vinoth.uptimemonitor.exception.MonitorNotFoundException;
import com.vinoth.uptimemonitor.repository.MonitorCheckRepository;
import com.vinoth.uptimemonitor.repository.MonitorRepository;

@ExtendWith(MockitoExtension.class)
class MonitorServiceTest {

    @Mock
    private MonitorRepository monitorRepository;

    @Mock
    private MonitorCheckRepository monitorCheckRepository;

    @InjectMocks
    private MonitorService monitorService;

    private Monitor monitor;

    @BeforeEach
    void setUp() {
        monitor = new Monitor();
        monitor.setName("Google Test");
        monitor.setUrl("https://www.google.com");
        monitor.setMethod("GET");
        monitor.setActive(true);
        monitor.setIntervalSeconds(60);
        monitor.setTimeoutSeconds(5);
        monitor.setExpectedStatus(200);
    }

    @Test
    void createMonitorShouldSaveMonitor() {
        when(monitorRepository.save(monitor)).thenReturn(monitor);

        Monitor result = monitorService.createMonitor(monitor);

        assertSame(monitor, result);
        verify(monitorRepository).save(monitor);
    }

    @Test
    void getAllMonitorsShouldReturnAllMonitors() {
        when(monitorRepository.findAll()).thenReturn(List.of(monitor));

        List<Monitor> result = monitorService.getAllMonitors();

        assertEquals(1, result.size());
        assertSame(monitor, result.get(0));
        verify(monitorRepository).findAll();
    }

    @Test
    void getActiveMonitorsShouldReturnActiveMonitors() {
        when(monitorRepository.findByActiveTrue())
                .thenReturn(List.of(monitor));

        List<Monitor> result = monitorService.getActiveMonitors();

        assertEquals(1, result.size());
        assertSame(monitor, result.get(0));
        verify(monitorRepository).findByActiveTrue();
    }

    @Test
    void getMonitorByIdShouldReturnMonitorWhenFound() {
        when(monitorRepository.findById(1L))
                .thenReturn(Optional.of(monitor));

        Monitor result = monitorService.getMonitorById(1L);

        assertSame(monitor, result);
        verify(monitorRepository).findById(1L);
    }

    @Test
    void getMonitorByIdShouldThrowWhenNotFound() {
        when(monitorRepository.findById(999L))
                .thenReturn(Optional.empty());

        assertThrows(
                MonitorNotFoundException.class,
                () -> monitorService.getMonitorById(999L)
        );

        verify(monitorRepository).findById(999L);
    }

    @Test
    void updateMonitorShouldUpdateAndSaveExistingMonitor() {
        Monitor updatedMonitor = new Monitor();
        updatedMonitor.setName("Updated Google");
        updatedMonitor.setUrl("https://example.com");
        updatedMonitor.setMethod("GET");
        updatedMonitor.setIntervalSeconds(120);
        updatedMonitor.setTimeoutSeconds(10);
        updatedMonitor.setExpectedStatus(201);
        updatedMonitor.setActive(false);

        when(monitorRepository.findById(1L))
                .thenReturn(Optional.of(monitor));
        when(monitorRepository.save(any(Monitor.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Monitor result = monitorService.updateMonitor(1L, updatedMonitor);

        assertEquals("Updated Google", result.getName());
        assertEquals("https://example.com", result.getUrl());
        assertEquals("GET", result.getMethod());
        assertEquals(120, result.getIntervalSeconds());
        assertEquals(10, result.getTimeoutSeconds());
        assertEquals(201, result.getExpectedStatus());
        assertEquals(false, result.getActive());

        verify(monitorRepository).save(monitor);
    }

    @Test
    void deleteMonitorShouldDeleteChecksBeforeMonitor() {
        when(monitorRepository.findById(1L))
                .thenReturn(Optional.of(monitor));

        monitorService.deleteMonitor(1L);

        InOrder order = inOrder(monitorCheckRepository, monitorRepository);

        order.verify(monitorCheckRepository).deleteAllByMonitorId(1L);
        order.verify(monitorCheckRepository).flush();
        order.verify(monitorRepository).delete(monitor);
        order.verify(monitorRepository).flush();
    }

    @Test
    void deleteMonitorShouldNotDeleteWhenMonitorDoesNotExist() {
        when(monitorRepository.findById(999L))
                .thenReturn(Optional.empty());

        assertThrows(
                MonitorNotFoundException.class,
                () -> monitorService.deleteMonitor(999L)
        );

        verify(monitorRepository, never()).delete(any(Monitor.class));
        verifyNoInteractions(monitorCheckRepository);
    }

    @Test
    void updateLastCheckedAtShouldUpdateAndSaveMonitor() {
        LocalDateTime checkedAt =
                LocalDateTime.of(2026, 10, 11, 12, 30);

        when(monitorRepository.findById(1L))
                .thenReturn(Optional.of(monitor));
        when(monitorRepository.save(monitor)).thenReturn(monitor);

        monitorService.updateLastCheckedAt(1L, checkedAt);

        assertEquals(checkedAt, monitor.getLastCheckedAt());
        verify(monitorRepository).save(monitor);
    }
}
