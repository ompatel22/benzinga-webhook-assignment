package com.benzinga.assignment.webhook_example.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LogPayload{

    @JsonProperty("user_id")
    private Integer userId;

    private Double total;
    private String title;
    private Meta meta;
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