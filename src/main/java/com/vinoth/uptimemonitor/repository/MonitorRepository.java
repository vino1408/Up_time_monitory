
package com.vinoth.uptimemonitor.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.vinoth.uptimemonitor.entity.Monitor;

public interface MonitorRepository extends JpaRepository<Monitor, Long> {

    // Get all active monitors
    List<Monitor> findByActiveTrue();

    // Get all monitors with pagination
    Page<Monitor> findAll(Pageable pageable);

    // Get active monitors with pagination
    Page<Monitor> findByActiveTrue(Pageable pageable);
}
