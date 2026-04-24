package com.thearena.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.thearena.model.response.AuthResponse;
import com.thearena.model.response.BattleReplayResponse;
import com.thearena.model.response.BattleResultResponse;
import com.thearena.model.response.StartArenaResponse;
import com.thearena.model.response.TurnResponse;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ArenaIntegrationTest {
    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4")
            .withDatabaseName("the_arena")
            .withUsername("arena_user")
            .withPassword("arena_password");

    @Container
    static final MongoDBContainer MONGO = new MongoDBContainer("mongo:7");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "update");
        registry.add("spring.data.mongodb.uri", MONGO::getReplicaSetUrl);
    }

    @LocalServerPort
    int port;

    @Autowired
    TestRestTemplate restTemplate;

    @Test
    void fullFlowAuthBattleHistoryAndOwnership() {
        String user1Token = registerAndLogin("user1", "secret123");
        String user2Token = registerAndLogin("user2", "secret123");

        StartArenaResponse start = post("/arena/start", Map.of(), user1Token, StartArenaResponse.class);
        assertNotNull(start.sessionId());

        TurnResponse turn = patch("/arena/turn", Map.of("sessionId", start.sessionId(), "action", "attack"), user1Token, TurnResponse.class);
        assertNotNull(turn.message());

        BattleResultResponse result = get("/arena/history/" + start.sessionId() + "/result", user1Token, BattleResultResponse.class);
        assertEquals(start.sessionId(), result.battleId());

        BattleReplayResponse replay = post("/arena/history/" + start.sessionId() + "/replay", null, user1Token, BattleReplayResponse.class);
        assertNotNull(replay.initialSnapshot());
        assertTrue(replay.turns().size() >= 1);
        assertTrue(replay.initialSnapshot().payload().containsKey("initialPlayerEquipment"));
        assertTrue(replay.initialSnapshot().payload().containsKey("initialEnemyEquipment"));
        @SuppressWarnings("unchecked")
        java.util.Map<String, Object> playerEquip =
                (java.util.Map<String, Object>) replay.initialSnapshot().payload().get("initialPlayerEquipment");
        assertNotNull(playerEquip);
        assertEquals("user1", playerEquip.get("name"));

        ResponseEntity<String> forbidden = exchangeRaw(
                HttpMethod.GET,
                "/arena/history/" + start.sessionId() + "/result",
                user2Token,
                null
        );
        assertEquals(HttpStatus.BAD_REQUEST, forbidden.getStatusCode());
        assertTrue(forbidden.getBody() != null && forbidden.getBody().contains("Access denied"));
    }

    private String registerAndLogin(String username, String password) {
        post("/auth/register", Map.of("username", username, "password", password), null, AuthResponse.class);
        AuthResponse login = post("/auth/login", Map.of("username", username, "password", password), null, AuthResponse.class);
        return login.accessToken();
    }

    private <T> T get(String path, String token, Class<T> responseType) {
        HttpHeaders headers = authHeaders(token);
        ResponseEntity<T> response = restTemplate.exchange(url(path), HttpMethod.GET, new HttpEntity<>(headers), responseType);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        return response.getBody();
    }

    private <T> T post(String path, Object body, String token, Class<T> responseType) {
        HttpHeaders headers = authHeaders(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<T> response = restTemplate.exchange(url(path), HttpMethod.POST, new HttpEntity<>(body, headers), responseType);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        return response.getBody();
    }

    private <T> T patch(String path, Object body, String token, Class<T> responseType) {
        HttpHeaders headers = authHeaders(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<T> response = restTemplate.exchange(url(path), HttpMethod.PATCH, new HttpEntity<>(body, headers), responseType);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        return response.getBody();
    }

    private ResponseEntity<String> exchangeRaw(HttpMethod method, String path, String token, Object body) {
        HttpHeaders headers = authHeaders(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return restTemplate.exchange(url(path), method, new HttpEntity<>(body, headers), String.class);
    }

    private HttpHeaders authHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        if (token != null && !token.isBlank()) {
            headers.setBearerAuth(token);
        }
        return headers;
    }

    private String url(String path) {
        return "http://localhost:" + port + path;
    }
}
