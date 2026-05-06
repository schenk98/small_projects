package com.poe.backend.repo;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.poe.backend.model.MinigameConfig;

/** Minigame configuration persistence (energy costs, reward strategies, descriptions). */
public interface MinigameRepo extends MongoRepository<MinigameConfig, String> {
    /** List active minigames. */
    List<MinigameConfig> findByActiveTrue();

    /** Find minigame config by code, regardless of active flag. */
    Optional<MinigameConfig> findByCode(String code);

    /** Find active minigame config by code. */
    Optional<MinigameConfig> findByCodeAndActiveTrue(String code);
}
