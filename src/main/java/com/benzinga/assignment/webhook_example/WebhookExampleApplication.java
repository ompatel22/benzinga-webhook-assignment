package com.benzinga.assignment.webhook_example;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@Slf4j
public class WebhookExampleApplication {
	public static void main(String[] args) {
		SpringApplication.run(WebhookExampleApplication.class, args);
		log.info("Webhook Example application started successfully.");
	}
}
