package com.benzinga.assignment.webhook_example.services;

import com.benzinga.assignment.webhook_example.dtos.LogPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class BatchService {

    @Value("${app.batch.size}")
    private int batchSize;

    @Value("${app.batch.interval-ms}")
    private int batchIntervalMilliSeconds;

    @Value("${app.max-queue-size:10000}")
    private int maxQueueSize;

    // BlockingQueue - A bounded, thread-safe queue with a fixed capacity
    // Prevents memory issues by rejecting new items when full
    // Multiple threads can safely add/remove items concurrently
    // Request thread can add logs into queue and Batch sender thread can remove logs from queue at the same time
    private BlockingQueue<LogPayload> logQueue;

    // Using a single-threaded scheduled executor ensures only one scheduled task runs at any time
    // The next run waits until the current run finishes, avoiding multiple small or out-of-order batches
    private final ScheduledExecutorService batchScheduler = Executors.newSingleThreadScheduledExecutor();

    // Using a single-threaded executor ensures only one sendBatch() executes at a time
    // Prevents race conditions when size trigger and interval trigger fire simultaneously
    private final ExecutorService batchSender = Executors.newSingleThreadExecutor();

    private final BatchPostService batchPostService;

    @PostConstruct
    public void init() {

        if (batchSize <= 0) {
            throw new IllegalArgumentException("Invalid batch size");
        }

        logQueue = new ArrayBlockingQueue<>(maxQueueSize);

        log.info("BatchService initialized - batchSize={}, batchIntervalMilliSeconds={}, maxQueueSize={}",
                batchSize, batchIntervalMilliSeconds, maxQueueSize);

        batchScheduler.scheduleAtFixedRate(this::sendBatchIfQueueNotEmpty, batchIntervalMilliSeconds, batchIntervalMilliSeconds, TimeUnit.MILLISECONDS);
        log.info("Batch scheduler started at a fixed rate of {} seconds.", batchIntervalMilliSeconds);
    }

    public boolean addLogPayload(LogPayload logPayload) {
        boolean added = logQueue.offer(logPayload);

        if (added) {
            log.info("Received payload — current queue size: {}", logQueue.size());

            // If enough log payloads are in queue, trigger an immediate batch send asynchronously
            if (logQueue.size() >= batchSize) {
                batchSender.submit(this::sendBatch);
            }
        } else {
            log.warn("Queue full (size={}), rejecting payload - {}", maxQueueSize, logPayload);
        }
        return added;
    }

    private void sendBatchIfQueueNotEmpty() {
        if (!logQueue.isEmpty()) {
            batchSender.submit(this::sendBatch);
        }
    }

    public void sendBatch() {
        List<LogPayload> batch = new ArrayList<>();
        logQueue.drainTo(batch, batchSize);

        if (batch.isEmpty()) return;

        long startTime = System.nanoTime();
        int statusCode = batchPostService.postBatchWithRetries(batch, 3, Duration.ofSeconds(2));
        long durationMs = (System.nanoTime() - startTime) / 1_000_000;

        log.info("Sent batch size={}, statusCode={}, duration in ms={}",
                batch.size(), statusCode, durationMs);

        // Exit after 3 retry failures (as per requirements)
        if (statusCode == -1) {
            log.error("Failed to post batch after 3 retries. Exiting application.");
            System.exit(1);
        }
    }

    @PreDestroy
    public void shutdown() {
        log.info("Shutting down BatchService - flushing remaining items, queue size={}", logQueue.size());

        // Flush all remaining batches
        while (!logQueue.isEmpty()) {
            sendBatch();
        }

        batchScheduler.shutdown();
        batchSender.shutdown();

        try {
            // Wait for tasks to complete (with timeout)
            if (!batchSender.awaitTermination(30, TimeUnit.SECONDS)) {
                log.warn("BatchSender did not terminate in time, forcing shutdown");
                batchSender.shutdownNow();
            }
            if (!batchScheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                log.warn("BatchScheduler did not terminate in time, forcing shutdown");
                batchScheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            log.error("Shutdown interrupted", e);
            batchSender.shutdownNow();
            batchScheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        log.info("BatchService shutdown complete");
    }
}