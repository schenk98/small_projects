package com.poe.backend.repo;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.poe.backend.model.MinigameConfig;

public interface MinigameRepo extends MongoRepository<MinigameConfig, String> {
    List<MinigameConfig> findByActiveTrue();

    Optional<MinigameConfig> findByCode(String code);

    Optional<MinigameConfig> findByCodeAndActiveTrue(String code);
}
