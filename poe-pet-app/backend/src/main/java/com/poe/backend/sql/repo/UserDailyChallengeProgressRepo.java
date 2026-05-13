package com.poe.backend.sql.repo;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.poe.backend.sql.model.UserDailyChallengeProgress;

/** Repository for player progress rows against generated daily challenges. */
public interface UserDailyChallengeProgressRepo extends JpaRepository<UserDailyChallengeProgress, Long> {
    /** Load all challenge progress rows for one user and a fixed set of definitions. */
    List<UserDailyChallengeProgress> findByUserIdAndChallengeDefinitionIdIn(String userId, List<Long> challengeDefinitionIds);

    /** Load a single progress row for incremental updates while processing events. */
    Optional<UserDailyChallengeProgress> findByUserIdAndChallengeDefinitionId(String userId, Long challengeDefinitionId);
}
