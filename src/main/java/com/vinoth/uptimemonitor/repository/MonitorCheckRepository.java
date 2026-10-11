package com.vinoth.uptimemonitor.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.vinoth.uptimemonitor.entity.MonitorCheck;

public interface MonitorCheckRepository
        extends JpaRepository<MonitorCheck, Long> {

    // Find checks within a date and time range
    List<MonitorCheck> findByMonitorIdAndCheckedAtBetween(
            Long monitorId,
            LocalDateTime start,
            LocalDateTime end
    );

    // Find all checks, newest first
    List<MonitorCheck> findByMonitorIdOrderByCheckedAtDesc(
            Long monitorId
    );

    // Find checks with pagination, newest first
    Page<MonitorCheck> findByMonitorIdOrderByCheckedAtDesc(
            Long monitorId,
            Pageable pageable
    );

    // Filter successful or failed checks with pagination
    Page<MonitorCheck> findByMonitorIdAndSuccessOrderByCheckedAtDesc(
            Long monitorId,
            boolean success,
            Pageable pageable
    );

    // Count all checks for a monitor
    long countByMonitorId(Long monitorId);

    // Count successful checks
    long countByMonitorIdAndSuccessTrue(Long monitorId);

    // Delete all checks belonging to a monitor
    @Modifying
    @Query("""
            DELETE FROM MonitorCheck mc
            WHERE mc.monitor.id = :monitorId
            """)
    int deleteAllByMonitorId(
            @Param("monitorId") Long monitorId
    );
}