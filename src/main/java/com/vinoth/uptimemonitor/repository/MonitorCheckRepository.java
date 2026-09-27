package com.vinoth.uptimemonitor.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.vinoth.uptimemonitor.entity.MonitorCheck;

public interface MonitorCheckRepository
        extends JpaRepository<MonitorCheck, Long> {

    List<MonitorCheck> findByMonitorIdAndCheckedAtBetween(
            Long monitorId,
            LocalDateTime start,
            LocalDateTime end
    );

    List<MonitorCheck> findByMonitorIdOrderByCheckedAtDesc(
            Long monitorId
    );

    long countByMonitorId(Long monitorId);

    long countByMonitorIdAndSuccessTrue(Long monitorId);

    @Modifying
    @Query("""
            DELETE FROM MonitorCheck mc
            WHERE mc.monitor.id = :monitorId
            """)
    int deleteAllByMonitorId(
            @Param("monitorId") Long monitorId
    );
}