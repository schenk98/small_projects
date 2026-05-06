package com.poe.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot application entrypoint.
 *
 * This project is a small, learning-oriented backend:
 * - persistence: MongoDB via Spring Data repositories
 * - auth: a minimal token-based interceptor (see {@code security/AuthInterceptor})
 * - API: JSON endpoints consumed by the Vite/React frontend
 */
@SpringBootApplication
public class BackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(BackendApplication.class, args);
	}

}
