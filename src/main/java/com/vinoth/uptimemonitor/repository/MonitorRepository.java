package com.vinoth.uptimemonitor.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.vinoth.uptimemonitor.entity.Monitor;

public interface MonitorRepository extends JpaRepository<Monitor, Long> {

    List<Monitor> findByActiveTrue();
}