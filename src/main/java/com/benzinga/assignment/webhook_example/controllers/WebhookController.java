package com.benzinga.assignment.webhook_example.controllers;

import com.benzinga.assignment.webhook_example.dtos.LogPayload;
import com.benzinga.assignment.webhook_example.services.BatchService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Slf4j
public class WebhookController {

    private final BatchService batchService;

    // GET endpoint to check health of the application
    @GetMapping("/healthz")
    public  ResponseEntity<String> healthCheck() {
        log.info("GET request received at /healthz");
        return ResponseEntity.ok("OK");
    }

    // POST endpoint to accept JSON payloads
    @PostMapping("/log")
    public ResponseEntity<String> receiveLog(@Valid @RequestBody LogPayload logPayload) {
        log.info("POST request received at /log endpoint with payload - {}", logPayload);
        boolean accepted = batchService.addLogPayload(logPayload);

        if (accepted) {
            return ResponseEntity.ok("Log Payload Accepted");
        } else {
            return new ResponseEntity<>("Queue is full, please retry later",HttpStatus.SERVICE_UNAVAILABLE);
        }
    }
}