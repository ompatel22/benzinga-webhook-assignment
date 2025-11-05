package com.benzinga.assignment.webhook_example.services;

import com.benzinga.assignment.webhook_example.dtos.LogPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class BatchPostService {

    private final RestTemplate restTemplate;

    @Value("${app.post-endpoint}")
    private String postEndpoint;

    public int postBatchWithRetries(List<LogPayload> batch, int maxRetries, Duration waitBetween) {
        int totalAttempts = maxRetries + 1; // 1 initial + 3 retries = 4 total attempts

        for (int attempt = 1; attempt <= totalAttempts; attempt++) {
            try {
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                HttpEntity<List<LogPayload>> requestEntity = new HttpEntity<>(batch, headers);

                log.info("Posting batch to {}, attempt {}/{}", postEndpoint, attempt, totalAttempts);
                ResponseEntity<String> response = restTemplate.exchange(postEndpoint, HttpMethod.POST, requestEntity, String.class);
                int statusCode = response.getStatusCode().value();

                // Check if successful (2xx status codes)
                if (response.getStatusCode().is2xxSuccessful()) {
                    log.info("Batch posted successfully - statusCode={}", statusCode);
                    return statusCode;
                }

                // Non-2xx response (like 3xx redirects or unexpected codes)
                log.warn("Batch post returned non-2xx statusCode={}", statusCode);
                // Considering it as a failure and retrying

            } catch (HttpClientErrorException e) {
                // 4xx errors - client error (bad request, unauthorized, etc.), There should not be a retry.
                log.error("Client error on attempt {}/{}, statusCode={}, message={}", attempt, totalAttempts, e.getStatusCode(), e.getMessage());
                log.error("Due to Client side error, No retry");
                return e.getStatusCode().value();

            } catch (HttpServerErrorException e) {
                // 5xx errors - server error (should retry)
                log.error("Server error on attempt {}/{}, statusCode={}, message={}", attempt, totalAttempts, e.getStatusCode(), e.getMessage());

            } catch (RestClientException e) {
                // Network errors, timeouts, etc.
                log.error("Network/connection error on attempt {}/{}, message={}", attempt, totalAttempts, e.getMessage());

            } catch (Exception e) {
                // Unexpected errors
                log.error("Unexpected error on attempt {}/{}, error-info={} - {}", attempt, totalAttempts, e.getClass().getSimpleName(), e.getMessage());
            }
            // Wait 2 seconds before retry (but not after the last attempt)
            if (attempt < totalAttempts) {
                log.info("Waiting {} seconds before retry...", waitBetween.getSeconds());
                try {
                    Thread.sleep(waitBetween.toMillis());
                } catch (InterruptedException ie) {
                    log.error("Retry... wait interrupted", ie);
                    Thread.currentThread().interrupt();
                    return -1;
                }
            }
        }
        // All attempts failed
        log.error("All {} attempts failed to post batch", totalAttempts);
        return -1;
    }
}