package com.thearena.persistence.mysql;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "refresh_tokens")
public class RefreshTokenEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 512)
    private String token;

    @Column(nullable = false)
    private Long accountId;

    @Column(nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    private boolean revoked;

    protected RefreshTokenEntity() {}

    public RefreshTokenEntity(String token, Long accountId, Instant expiresAt) {
        this.token = token;
        this.accountId = accountId;
        this.expiresAt = expiresAt;
        this.revoked = false;
    }

    public String getToken() { return token; }
    public Long getAccountId() { return accountId; }
    public Instant getExpiresAt() { return expiresAt; }
    public boolean isRevoked() { return revoked; }
    public void setRevoked(boolean revoked) { this.revoked = revoked; }
}
