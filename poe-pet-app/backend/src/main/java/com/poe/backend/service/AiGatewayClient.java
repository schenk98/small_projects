package com.poe.backend.service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Small client for the standalone Local SLM Gateway side project.
 *
 * We keep this as an optional dependency:
 * - if base URL is unset, we treat the gateway as "disabled"
 * - API key stays on the server (frontend never talks to the gateway directly)
 */
@Component
public class AiGatewayClient {
    private final ObjectMapper objectMapper;
    private final HttpClient http;
    private final AtomicReference<String> baseUrl;
    private final AtomicReference<String> apiKey;
    private final int chatTimeoutSeconds;

    public AiGatewayClient(
            ObjectMapper objectMapper,
            @Value("${app.aiGatewayBaseUrl:}") String baseUrl,
            @Value("${app.aiGatewayApiKey:}") String apiKey,
            @Value("${app.aiGatewayConnectTimeoutSeconds:10}") int connectTimeoutSeconds,
            @Value("${app.aiGatewayChatTimeoutSeconds:300}") int chatTimeoutSeconds) {
        this.objectMapper = objectMapper;
        this.chatTimeoutSeconds = Math.max(30, chatTimeoutSeconds);
        int connectSec = Math.max(1, connectTimeoutSeconds);
        // Force HTTP/1.1 to avoid any HTTP/2 negotiation weirdness with local dev servers.
        this.http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(connectSec))
                .version(HttpClient.Version.HTTP_1_1)
                .build();
        this.baseUrl = new AtomicReference<>(baseUrl != null ? baseUrl.trim() : "");
        this.apiKey = new AtomicReference<>(apiKey != null ? apiKey.trim() : "");
    }

    /** Update runtime configuration (intended for privileged developer usage). */
    public void setConfig(String nextBaseUrl, String nextApiKey) {
        this.baseUrl.set(nextBaseUrl != null ? nextBaseUrl.trim() : "");
        this.apiKey.set(nextApiKey != null ? nextApiKey.trim() : "");
    }

    /** Return effective config for debugging. Never expose the full API key. */
    public Map<String, Object> getConfig() {
        String k = apiKey.get();
        String redacted = k == null || k.isBlank() ? "" : (k.substring(0, Math.min(4, k.length())) + "…");
        return Map.of("baseUrl", baseUrl.get(), "apiKeyPrefix", redacted);
    }

    /** True if a base URL is configured. */
    public boolean isEnabled() {
        return baseUrl.get() != null && !baseUrl.get().isBlank();
    }

    public Map<String, Object> health() throws IOException, InterruptedException {
        return getJson("/health");
    }

    public Map<String, Object> ready() throws IOException, InterruptedException {
        return getJson("/ready");
    }

    public Map<String, Object> version() throws IOException, InterruptedException {
        return getJson("/version");
    }

    public Map<String, Object> chat(Map<String, Object> payload) throws IOException, InterruptedException {
        return postJson("/v1/chat", payload);
    }

    private URI uri(String path) {
        String b = baseUrl.get();
        if (b == null || b.isBlank()) {
            throw new IllegalStateException("AI gateway is not configured (app.aiGatewayBaseUrl is empty)");
        }
        String base = b.endsWith("/") ? b.substring(0, b.length() - 1) : b;
        String p = path.startsWith("/") ? path : "/" + path;
        return URI.create(base + p);
    }

    private Map<String, Object> getJson(String path) throws IOException, InterruptedException {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(uri(path))
                .timeout(Duration.ofSeconds(15))
                .GET()
                .build();
        HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
        if (res.statusCode() < 200 || res.statusCode() >= 300) {
            throw new IOException("AI gateway request failed (" + res.statusCode() + "): " + path);
        }
        return objectMapper.readValue(res.body(), new TypeReference<Map<String, Object>>() {});
    }

    private Map<String, Object> postJson(String path, Map<String, Object> payload) throws IOException, InterruptedException {
        String k = apiKey.get();
        if (k == null || k.isBlank()) {
            throw new IllegalStateException("AI gateway API key is not configured (app.aiGatewayApiKey is empty)");
        }
        String body = objectMapper.writeValueAsString(payload);
        if (body == null || body.isBlank()) {
            throw new IllegalStateException("AI gateway payload serialized to empty body (unexpected)");
        }
        HttpRequest req = HttpRequest.newBuilder()
                .uri(uri(path))
                .timeout(Duration.ofSeconds(chatTimeoutSeconds))
                .header("Authorization", "Bearer " + k)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
        if (res.statusCode() < 200 || res.statusCode() >= 300) {
            // Include request body length to quickly diagnose "missing body" errors (422).
            throw new IOException("AI gateway request failed (" + res.statusCode() + "): " + path
                    + " (bodyChars=" + body.length() + ") -> " + res.body());
        }
        return objectMapper.readValue(res.body(), new TypeReference<Map<String, Object>>() {});
    }
}

