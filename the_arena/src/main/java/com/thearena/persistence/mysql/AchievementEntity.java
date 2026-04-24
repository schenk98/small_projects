package com.thearena.persistence.mysql;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;

@Entity
@Table(
        name = "achievements",
        uniqueConstraints = @UniqueConstraint(name = "uk_achievement_account_code", columnNames = {"accountId", "code"})
)
public class AchievementEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long accountId;

    @Column(nullable = false)
    private String code;

    @Column(nullable = false)
    private Instant unlockedAt;

    protected AchievementEntity() {
    }

    public AchievementEntity(Long accountId, String code, Instant unlockedAt) {
        this.accountId = accountId;
        this.code = code;
        this.unlockedAt = unlockedAt;
    }

    public String getCode() {
        return code;
    }
}
