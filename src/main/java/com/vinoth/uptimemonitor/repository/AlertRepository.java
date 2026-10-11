package com.vinoth.uptimemonitor.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.vinoth.uptimemonitor.entity.Alert;

public interface AlertRepository extends JpaRepository<Alert, Long> {

    Optional<Alert> findFirstByMonitorIdAndStatusOrderByCreatedAtDesc(
            Long monitorId,
            String status
    );

    List<Alert> findByMonitorIdOrderByCreatedAtDesc(
            Long monitorId
    );
}