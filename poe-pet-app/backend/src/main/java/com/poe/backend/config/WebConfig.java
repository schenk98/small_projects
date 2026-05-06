package com.poe.backend.config;

import java.util.Arrays;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.poe.backend.security.AuthInterceptor;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    private final AuthInterceptor authInterceptor;
    private final String corsAllowedOriginsCsv;

    /**
     * MVC wiring:
     * - registers the auth interceptor (token check)
     * - configures browser CORS policy
     *
     * CORS note:
     * - safe-by-default: allow only explicit origins (dev default is Vite on localhost)
     * - production: set {@code APP_CORS_ALLOWED_ORIGINS} to your deployed frontend origin(s)
     */
    public WebConfig(
            AuthInterceptor authInterceptor,
            @Value("${app.corsAllowedOrigins:http://localhost:5173}") String corsAllowedOriginsCsv
    ) {
        this.authInterceptor = authInterceptor;
        this.corsAllowedOriginsCsv = corsAllowedOriginsCsv;
    }

    @Override
    /** Register the auth interceptor for all MVC routes. */
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor);
    }

    @Override
    /** Configure browser CORS policy (allowlist-based; configured via {@code app.corsAllowedOrigins}). */
    public void addCorsMappings(CorsRegistry registry) {
        // Safe-by-default: only allow explicitly configured browser origins.
        // This prevents "any website can call your API" surprises.
        String[] allowed = Arrays.stream(corsAllowedOriginsCsv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toArray(String[]::new);
        registry.addMapping("/**")
                .allowedOrigins(allowed)
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*");
    }
}
