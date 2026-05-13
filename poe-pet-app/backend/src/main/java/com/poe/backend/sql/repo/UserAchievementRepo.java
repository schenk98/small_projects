package com.poe.backend.sql.repo;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.poe.backend.sql.model.UserAchievement;

/** Relational repository for per-user achievement progress. */
public interface UserAchievementRepo extends JpaRepository<UserAchievement, Long> {
    /** Load all progress rows for a given user. */
    List<UserAchievement> findByUserIdOrderByUpdatedAtDesc(String userId);

    /** Load a single progress row by its natural key. */
    Optional<UserAchievement> findByUserIdAndAchievementCode(String userId, String achievementCode);
}
