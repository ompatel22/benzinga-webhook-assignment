package com.benzinga.assignment.webhook_example;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
@Slf4j
public class WebhookExampleApplication {
	public static void main(String[] args) {
		SpringApplication.run(WebhookExampleApplication.class, args);
		log.info("Webhook Example Application started successfully.");
	}

	@Bean
	public RestTemplate restTemplate() {
		return new RestTemplate();
	}
}