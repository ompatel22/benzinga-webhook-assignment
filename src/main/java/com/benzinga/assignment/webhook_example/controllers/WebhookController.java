package com.benzinga.assignment.webhook_example.controllers;

import com.benzinga.assignment.webhook_example.dtos.LogPayload;
import com.benzinga.assignment.webhook_example.services.BatchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Slf4j
public class WebhookController {

    private final BatchService batchService;

    @GetMapping("/healthz")
    public  ResponseEntity<String> healthCheck() {
        log.info("GET request received at /healthz");
        return ResponseEntity.ok("OK");
    }
    @PostMapping("/log")
    public ResponseEntity<LogPayload> receiveLog(@RequestBody LogPayload logPayload) {
        log.info("POST request received at /log endpoint with payload - {}", logPayload);
        batchService.addLog(logPayload);
        return ResponseEntity.ok(logPayload);
    }
}
