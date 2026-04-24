package com.thearena.persistence.mysql;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "players")
public class PlayerEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false)
    private int level;

    @Column(nullable = false)
    private int xp;

    @Column(nullable = false)
    private Instant createdAt;

    public PlayerEntity() {
    }

    public PlayerEntity(String username, int level, int xp, Instant createdAt) {
        this.username = username;
        this.level = level;
        this.xp = xp;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public int getXp() {
        return xp;
    }

    public void setXp(int xp) {
        this.xp = xp;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
