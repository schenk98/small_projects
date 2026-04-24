package com.thearena.service;

import com.thearena.exception.TooManyRequestsException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

@Service
public class AuthRateLimitService {
    private static final int MAX_ATTEMPTS = 10;
    private static final long WINDOW_SECONDS = 60;

    private final Map<String, AttemptWindow> attempts = new ConcurrentHashMap<>();

    public void checkAllowed(String key) {
        AttemptWindow window = attempts.compute(key, (k, current) -> rotateIfExpired(current));
        if (window.count >= MAX_ATTEMPTS) {
            throw new TooManyRequestsException("AUTH_RATE_LIMIT", "Too many auth attempts. Please wait and try again.");
        }
    }

    public void registerAttempt(String key) {
        attempts.compute(key, (k, current) -> {
            AttemptWindow window = rotateIfExpired(current);
            window.count++;
            return window;
        });
    }

    private AttemptWindow rotateIfExpired(AttemptWindow current) {
        Instant now = Instant.now();
        if (current == null || now.isAfter(current.startedAt.plusSeconds(WINDOW_SECONDS))) {
            return new AttemptWindow(now, 0);
        }
        return current;
    }

    private static class AttemptWindow {
        private final Instant startedAt;
        private int count;

        private AttemptWindow(Instant startedAt, int count) {
            this.startedAt = startedAt;
            this.count = count;
        }
    }
}
