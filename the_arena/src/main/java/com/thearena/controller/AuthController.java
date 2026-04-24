package com.thearena.controller;

import com.thearena.model.request.LoginRequest;
import com.thearena.model.request.RefreshTokenRequest;
import com.thearena.model.request.RegisterRequest;
import com.thearena.model.response.AuthResponse;
import com.thearena.service.AchievementService;
import com.thearena.service.AuthService;
import com.thearena.service.AuthRateLimitService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private final AuthService authService;
    private final AchievementService achievementService;
    private final AuthRateLimitService authRateLimitService;

    public AuthController(
            AuthService authService,
            AchievementService achievementService,
            AuthRateLimitService authRateLimitService
    ) {
        this.authService = authService;
        this.achievementService = achievementService;
        this.authRateLimitService = authRateLimitService;
    }

    @PostMapping("/register")
    public AuthResponse register(@Valid @RequestBody RegisterRequest request, HttpServletRequest servletRequest) {
        String key = key(servletRequest, request.username());
        authRateLimitService.checkAllowed(key);
        authRateLimitService.registerAttempt(key);
        return authService.register(request.username(), request.password());
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request, HttpServletRequest servletRequest) {
        String key = key(servletRequest, request.username());
        authRateLimitService.checkAllowed(key);
        authRateLimitService.registerAttempt(key);
        return authService.login(request.username(), request.password());
    }

    @PostMapping("/refresh")
    public AuthResponse refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return authService.refresh(request.refreshToken());
    }

    @PostMapping("/logout")
    public void logout(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            authService.logout(header.substring(7));
        }
    }

    @GetMapping("/me/achievements")
    public List<String> myAchievements(Authentication authentication) {
        return achievementService.listCodes(authentication.getName());
    }
    private String key(HttpServletRequest request, String username) {
        return request.getRemoteAddr() + ":" + username.toLowerCase();
    }
}
