
package com.vinoth.uptimemonitor.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;

import com.vinoth.uptimemonitor.entity.Monitor;
import com.vinoth.uptimemonitor.entity.MonitorCheck;
import com.vinoth.uptimemonitor.repository.MonitorCheckRepository;
import com.vinoth.uptimemonitor.repository.MonitorRepository;

class MonitoringServiceConcurrencyTest {

    @Test
    void simultaneousChecksForSameMonitorAreSerialized() throws Exception {

        MonitorCheckRepository checkRepository =
                mock(MonitorCheckRepository.class);

        MonitorRepository monitorRepository =
                mock(MonitorRepository.class);

        AlertService alertService = mock(AlertService.class);

        PlatformTransactionManager transactionManager =
                mock(PlatformTransactionManager.class);

        when(transactionManager.getTransaction(any(TransactionDefinition.class)))
                .thenReturn(mock(TransactionStatus.class));

        when(transactionManager.getTransaction(any()))
                .thenReturn(mock(TransactionStatus.class));

        Monitor monitor = new Monitor();
        monitor.setId(100L);
        monitor.setName("Concurrency Test");
        monitor.setUrl("http://127.0.0.1:1/");
        monitor.setMethod("GET");
        monitor.setTimeoutSeconds(1);
        monitor.setExpectedStatus(200);
        monitor.setConsecutiveFailures(0);
        monitor.setConsecutiveSuccesses(0);
        monitor.setCurrentStatus("UNKNOWN");
        monitor.setActive(true);

        when(monitorRepository.findById(100L))
                .thenReturn(java.util.Optional.of(monitor));

        when(monitorRepository.save(any(Monitor.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AtomicInteger activeChecks = new AtomicInteger();
        AtomicInteger maximumConcurrentChecks = new AtomicInteger();

        when(checkRepository.save(any(MonitorCheck.class)))
                .thenAnswer(invocation -> {
                    MonitorCheck check = invocation.getArgument(0);
                    check.setId((long) activeChecks.incrementAndGet());
                    return check;
                });

        /*
         * The target uses a closed local port, so the HTTP check should fail.
         * This test focuses on the service's per-monitor lock and counters.
         */

        MonitoringService service = new MonitoringService(
                checkRepository,
                monitorRepository,
                alertService,
                transactionManager);

        int requestCount = 5;

        ExecutorService executor =
                Executors.newFixedThreadPool(requestCount);

        CountDownLatch ready = new CountDownLatch(requestCount);
        CountDownLatch start = new CountDownLatch(1);

        List<Future<MonitorCheck>> futures = new ArrayList<>();

        try {
            for (int i = 0; i < requestCount; i++) {
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    start.await();

                    int current = activeChecks.incrementAndGet();
                    maximumConcurrentChecks.accumulateAndGet(
                            current, Math::max);

                    try {
                        return service.checkMonitor(monitor);
                    } finally {
                        activeChecks.decrementAndGet();
                    }
                }));
            }

            assertEquals(
                    true,
                    ready.await(5, TimeUnit.SECONDS),
                    "All test threads should be ready.");

            start.countDown();

            for (Future<MonitorCheck> future : futures) {
                assertNotNull(
                        future.get(15, TimeUnit.SECONDS),
                        "Every check should return a result.");
            }

            verify(checkRepository, times(requestCount))
                    .save(any(MonitorCheck.class));

            verify(monitorRepository, times(requestCount))
                    .save(any(Monitor.class));

            assertEquals(
                    requestCount,
                    monitor.getConsecutiveFailures(),
                    "Each failed check should increment the failure counter.");

        } finally {
            executor.shutdownNow();
        }
    }
}
