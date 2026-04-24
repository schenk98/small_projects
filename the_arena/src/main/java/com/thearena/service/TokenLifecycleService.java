package com.thearena.service;

import com.thearena.exception.UnauthorizedException;
import com.thearena.persistence.mysql.RefreshTokenEntity;
import com.thearena.persistence.mysql.RefreshTokenRepository;
import com.thearena.persistence.mysql.RevokedAccessTokenEntity;
import com.thearena.persistence.mysql.RevokedAccessTokenRepository;
import com.thearena.persistence.mysql.UserAccountEntity;
import com.thearena.persistence.mysql.UserAccountRepository;
import com.thearena.model.response.TokenPairResponse;
import com.thearena.security.JwtService;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TokenLifecycleService {
    private static final long REFRESH_EXPIRATION_SECONDS = 7 * 24 * 3600;
    private final SecureRandom secureRandom = new SecureRandom();

    private final RefreshTokenRepository refreshTokenRepository;
    private final RevokedAccessTokenRepository revokedAccessTokenRepository;
    private final UserAccountRepository userAccountRepository;
    private final JwtService jwtService;

    public TokenLifecycleService(
            RefreshTokenRepository refreshTokenRepository,
            RevokedAccessTokenRepository revokedAccessTokenRepository,
            UserAccountRepository userAccountRepository,
            JwtService jwtService
    ) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.revokedAccessTokenRepository = revokedAccessTokenRepository;
        this.userAccountRepository = userAccountRepository;
        this.jwtService = jwtService;
    }

    @Transactional
    public String issueRefreshToken(Long accountId) {
        byte[] bytes = new byte[48];
        secureRandom.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        refreshTokenRepository.save(new RefreshTokenEntity(
                token,
                accountId,
                Instant.now().plusSeconds(REFRESH_EXPIRATION_SECONDS)
        ));
        return token;
    }

    @Transactional(readOnly = true)
    public boolean isAccessTokenRevoked(String accessToken) {
        String jti = jwtService.extractTokenId(accessToken);
        return jti != null && revokedAccessTokenRepository.existsByJti(jti);
    }

    @Transactional
    public void revokeAccessToken(String accessToken) {
        String jti = jwtService.extractTokenId(accessToken);
        if (jti != null && !revokedAccessTokenRepository.existsByJti(jti)) {
            revokedAccessTokenRepository.save(new RevokedAccessTokenEntity(jti, jwtService.extractExpiration(accessToken)));
        }
    }

    @Transactional
    public TokenPairResponse rotateRefreshAndIssueAccess(String refreshToken) {
        RefreshTokenEntity token = refreshTokenRepository.findByToken(refreshToken)
                .orElseThrow(() -> new UnauthorizedException("INVALID_REFRESH_TOKEN", "Refresh token is invalid."));

        if (token.isRevoked() || Instant.now().isAfter(token.getExpiresAt())) {
            throw new UnauthorizedException("EXPIRED_REFRESH_TOKEN", "Refresh token expired or revoked.");
        }

        token.setRevoked(true);
        refreshTokenRepository.save(token);

        UserAccountEntity account = userAccountRepository.findById(token.getAccountId())
                .orElseThrow(() -> new UnauthorizedException("ACCOUNT_NOT_FOUND", "Account not found for refresh token."));

        String newAccessToken = jwtService.createToken(account.getUsername());
        String newRefreshToken = issueRefreshToken(account.getId());
        return new TokenPairResponse(newAccessToken, newRefreshToken);
    }
}
