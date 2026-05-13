package com.poe.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Spring Boot application entrypoint.
 *
 * This project is a small, learning-oriented backend:
 * - persistence: MongoDB for live gameplay state plus PostgreSQL for history/achievements/notification prefs
 * - auth: a minimal token-based interceptor (see {@code security/AuthInterceptor})
 * - API: JSON endpoints consumed by the Vite/React frontend
 */
@SpringBootApplication
@EnableScheduling
public class BackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(BackendApplication.class, args);
	}

}
