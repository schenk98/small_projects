package com.thearena.service;

import com.thearena.exception.ConflictException;
import com.thearena.exception.UnauthorizedException;
import com.thearena.model.response.AuthResponse;
import com.thearena.persistence.mysql.UserAccountEntity;
import com.thearena.persistence.mysql.UserAccountRepository;
import com.thearena.security.JwtService;
import java.time.Instant;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AchievementService achievementService;
    private final TokenLifecycleService tokenLifecycleService;
    private final PlayerProgressionService playerProgressionService;
    private final InventoryService inventoryService;

    public AuthService(
            UserAccountRepository userAccountRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            AchievementService achievementService,
            TokenLifecycleService tokenLifecycleService,
            PlayerProgressionService playerProgressionService,
            InventoryService inventoryService
    ) {
        this.userAccountRepository = userAccountRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.achievementService = achievementService;
        this.tokenLifecycleService = tokenLifecycleService;
        this.playerProgressionService = playerProgressionService;
        this.inventoryService = inventoryService;
    }

    @Transactional
    public AuthResponse register(String username, String password) {
        if (userAccountRepository.existsByUsername(username)) {
            throw new ConflictException("USERNAME_TAKEN", "Username already exists.");
        }
        UserAccountEntity account = new UserAccountEntity(username, passwordEncoder.encode(password), Instant.now());
        userAccountRepository.save(account);
        playerProgressionService.ensurePlayer(username);
        inventoryService.ensureStarterGear(username);
        String accessToken = jwtService.createToken(username);
        String refreshToken = tokenLifecycleService.issueRefreshToken(account.getId());
        return new AuthResponse(accessToken, refreshToken, username);
    }

    @Transactional
    public AuthResponse login(String username, String password) {
        UserAccountEntity account = userAccountRepository.findByUsername(username)
                .orElseThrow(() -> new UnauthorizedException("INVALID_CREDENTIALS", "Invalid username or password."));

        if (!passwordEncoder.matches(password, account.getPasswordHash())) {
            throw new UnauthorizedException("INVALID_CREDENTIALS", "Invalid username or password.");
        }

        account.setLastLoginAt(Instant.now());
        userAccountRepository.save(account);
        achievementService.unlock(username, AchievementService.FIRST_LOGIN);
        playerProgressionService.ensurePlayer(username);
        inventoryService.ensureStarterGear(username);

        String accessToken = jwtService.createToken(username);
        String refreshToken = tokenLifecycleService.issueRefreshToken(account.getId());
        return new AuthResponse(accessToken, refreshToken, username);
    }

    @Transactional
    public AuthResponse refresh(String refreshToken) {
        var pair = tokenLifecycleService.rotateRefreshAndIssueAccess(refreshToken);
        String accessToken = pair.accessToken();
        String username = jwtService.extractUsername(accessToken);
        return new AuthResponse(accessToken, pair.refreshToken(), username);
    }

    @Transactional
    public void logout(String accessToken) {
        tokenLifecycleService.revokeAccessToken(accessToken);
    }
}
