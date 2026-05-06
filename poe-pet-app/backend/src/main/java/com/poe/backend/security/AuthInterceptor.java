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

    /**
     * Minimal auth gate for the JSON API.
     *
     * Policy:
     * - allow unauthenticated access to `/auth/*` and `/public/*`
     * - require `Authorization: Bearer <token>` everywhere else
     * - look up the token in MongoDB and expose the user id via {@link CurrentUser}
     *
     * Implementation note:
     * This is intentionally small and explicit (good for a learning project), but it is not a full Spring Security setup.
     */
    public AuthInterceptor(UserTokenRepo userTokenRepo) {
        this.userTokenRepo = userTokenRepo;
    }

    @Override
    /** Check request auth and set {@link CurrentUser} when valid. */
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String path = request.getRequestURI();
        // Auth routes are mostly public (register/login/verify/reset). `/auth/me` is the exception.
        if ((path.startsWith("/auth/") && !path.equals("/auth/me")) || path.startsWith("/public/")) {
            return true;
        }

        // All non-auth routes must provide an access token.
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
        // Make the user id available to controller/service code for this request only.
        CurrentUser.set(token.userId);
        return true;
    }

    @Override
    /** Clear {@link CurrentUser} after request completion. */
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        CurrentUser.clear();
    }
}
