package com.benzinga.assignment.webhook_example.controllers;

import com.benzinga.assignment.webhook_example.dtos.LogPayload;
import com.benzinga.assignment.webhook_example.services.BatchService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(WebhookController.class)
class WebhookControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private BatchService batchService;

    @TestConfiguration
    static class TestConfig {
        @Bean
        BatchService batchService() {
            return Mockito.mock(BatchService.class);
        }
    }

    @Test
    void healthCheck_ShouldReturnOK() throws Exception {
        mockMvc.perform(get("/healthz"))
                .andExpect(status().isOk())
                .andExpect(content().string("OK"));
    }

    @Test
    void receiveLog_WithValidPayload_ShouldReturnLogPayLoadAccepted() throws Exception {
        LogPayload payload = createValidPayload();
        when(batchService.addLogPayload(any(LogPayload.class))).thenReturn(true);

        mockMvc.perform(post("/log")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(content().string("Log Payload Accepted"));
    }

    @Test
    void receiveLog_WhenQueueFull_ShouldReturnServiceUnavailable() throws Exception {
        LogPayload payload = createValidPayload();
        when(batchService.addLogPayload(any(LogPayload.class))).thenReturn(false);

        mockMvc.perform(post("/log")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isServiceUnavailable())
                .andExpect(content().string("Queue is full, please retry later"));
    }

    @Test
    void receiveLog_WithInvalidJSON_ShouldReturnBadRequest() throws Exception {
        mockMvc.perform(post("/log")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"id\": null }"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void receiveLog_WithMissingContentType_ShouldReturnUnsupportedMediaType() throws Exception {
        LogPayload payload = createValidPayload();

        mockMvc.perform(post("/log")
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isUnsupportedMediaType());
    }

    @Test
    void receiveLog_WithComplexNestedPayload_ShouldDeserializeCorrectly() throws Exception {
        LogPayload payload = createComplexPayload();
        when(batchService.addLogPayload(any(LogPayload.class))).thenReturn(true);

        mockMvc.perform(post("/log")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(content().string("Log Payload Accepted"));
    }

    private LogPayload createValidPayload() {
        LogPayload.PhoneNumbers phoneNumbers = new LogPayload.PhoneNumbers("555-1212", "123-5555");
        LogPayload.Login login = new LogPayload.Login("2020-08-08T01:52:50Z", "0.0.0.0");
        LogPayload.Meta meta = new LogPayload.Meta(List.of(login), phoneNumbers);
        return new LogPayload(1, 1.65, "delectus aut autem", meta, false);
    }

    private LogPayload createComplexPayload() {
        LogPayload.PhoneNumbers phoneNumbers = new LogPayload.PhoneNumbers("555-9999", "999-8888");
        LogPayload.Login login1 = new LogPayload.Login("2020-08-08T01:52:50Z", "192.168.1.1");
        LogPayload.Login login2 = new LogPayload.Login("2020-08-09T10:30:00Z", "10.0.0.1");
        LogPayload.Meta meta = new LogPayload.Meta(List.of(login1, login2), phoneNumbers);
        return new LogPayload(999, 999.99, "Complex test payload", meta, true
        );
    }

}