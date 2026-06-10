package com.hotel.pricing;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

/**
 * Spring Boot entry point for the Hotel Pricing System ADD 3.0
 * Multi-Agent Architecture Design application.
 * <p>
 * Uses Spring AI Alibaba Agent Framework with GPT-5.4 to execute
 * the Attribute-Driven Design method across four iterations.
 */
@SpringBootApplication
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Bean
    public ApplicationListener<ApplicationReadyEvent> startupMessage(Environment env) {
        return event -> {
            String port = env.getProperty("server.port", "8080");
            String ctx = env.getProperty("server.servlet.context-path", "");
            String studioUrl = "http://localhost:" + port + ctx + "/chatui/index.html";

            System.out.println("\n================================================");
            System.out.println("  Hotel Pricing System — ADD 3.0 Multi-Agent");
            System.out.println("  Model: GPT-5.4 | Paradigm: Multi-Agent");
            System.out.println("  Studio UI: " + studioUrl);
            System.out.println();
            System.out.println("  Iterations will run on startup if:");
            System.out.println("    hotelpricing.run-iterations=true");
            System.out.println("================================================\n");
        };
    }
}
