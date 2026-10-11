package com.vinoth.uptimemonitor.exception;

public class MonitorNotFoundException extends RuntimeException {

    public MonitorNotFoundException(Long monitorId) {
        super("Monitor with ID " + monitorId + " does not exist");
    }
}