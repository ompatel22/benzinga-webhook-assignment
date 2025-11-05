package com.benzinga.assignment.webhook_example.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.NotNull;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LogPayload{

    @NotNull(message = "user_id cannot be null")
    @JsonProperty("user_id")
    private Integer userId;

    @NotNull(message = "total cannot be null")
    private Double total;

    @NotNull(message = "title cannot be null")
    private String title;

    @Valid // Validates nested object
    private Meta meta;

    @NotNull(message = "completed cannot be null")
    private Boolean completed;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Meta {
        private List<Login> logins;

        @JsonProperty("phone_numbers")
        private PhoneNumbers phoneNumbers;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Login {
        private String time;
        private String ip;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PhoneNumbers {
        private String home;
        private String mobile;
    }
}