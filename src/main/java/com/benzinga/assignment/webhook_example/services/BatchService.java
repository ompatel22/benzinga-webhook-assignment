package com.benzinga.assignment.webhook_example.services;

import com.benzinga.assignment.webhook_example.dtos.LogPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.concurrent.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class BatchService {

    @Value("${app.batch-size}")
    private int batchSize;

    @Value("${app.batch-interval-seconds}")
    private int batchIntervalSeconds;

    @Value("${app.post-endpoint}")
    private String postEndpoint;

    private final ConcurrentLinkedQueue<LogPayload> queue = new ConcurrentLinkedQueue<>();

    //using a single-threaded scheduled executor ensures - only one scheduled task runs at any time
    //the next run waits until the current run finishes, so the queue is always accessed in a thread-safe, predictable order
    //so by this we can avoid overlapping
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    //newCachedThreadPool() = create new threads when needed or reuse idle threads for batch-processing tasks to allow multiple batch-processing tasks to happen in parallel
    private final ExecutorService senderExecutor = Executors.newCachedThreadPool();

     private final PostService postService;

    @PostConstruct
    public void init() {
        log.info("BatchService initialized - batchSize={}, batchIntervalSeconds={}, postEndpoint={}",
                batchSize, batchIntervalSeconds, postEndpoint);

        // Schedule periodic batch process
        scheduler.scheduleAtFixedRate(this::processBatchIfQueueIsNotEmpty, batchIntervalSeconds, batchIntervalSeconds, TimeUnit.SECONDS);
    }

    public void addLog(LogPayload record) {
        queue.add(record);
        if (queue.size() >= batchSize) {
            // process asynchronously to avoid blocking main thread
            senderExecutor.submit(this::processBatch);
        }
    }

    private void processBatchIfQueueIsNotEmpty() {
        if (!queue.isEmpty()) {
            // process asynchronously to avoid blocking main thread
            senderExecutor.submit(this::processBatch);
        }
    }

    public void processBatch() {
        //logic
    }

    @PreDestroy
    public void shutdown() {
        log.info("Shutting down BatchService - flushing remaining items");
        processBatchIfQueueIsNotEmpty();
        scheduler.shutdown();
        senderExecutor.shutdown();
    }
}
