package com.poe.backend.security;

import java.time.Instant;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import com.poe.backend.model.UserToken;
import com.poe.backend.repo.UserTokenRepo;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class AuthInterceptor implements HandlerInterceptor {
    private final UserTokenRepo userTokenRepo;

    public AuthInterceptor(UserTokenRepo userTokenRepo) {
        this.userTokenRepo = userTokenRepo;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String path = request.getRequestURI();
        if (path.startsWith("/auth/") || path.startsWith("/public/")) {
            return true;
        }

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            response.sendError(HttpStatus.UNAUTHORIZED.value(), "Missing token");
            return false;
        }

        String tokenValue = authHeader.substring(7);
        UserToken token = userTokenRepo
                .findByTokenAndTypeAndUsedFalseAndExpiresAtAfter(tokenValue, "ACCESS", Instant.now())
                .orElse(null);
        if (token == null) {
            response.sendError(HttpStatus.UNAUTHORIZED.value(), "Invalid token");
            return false;
        }
        CurrentUser.set(token.userId);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        CurrentUser.clear();
    }
}
