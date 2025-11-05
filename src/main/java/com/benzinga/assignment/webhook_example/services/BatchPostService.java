package com.benzinga.assignment.webhook_example.services;

import com.benzinga.assignment.webhook_example.dtos.LogPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class BatchPostService {
    public boolean postBatchWithRetries(List<LogPayload> batch, int maxRetries, Duration waitBetween) {
        //logic
        return true;
    }
}
