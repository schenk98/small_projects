package com.poe.backend.repo;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.poe.backend.model.MinigameSession;

public interface MinigameSessionRepo extends MongoRepository<MinigameSession, String> {
    Optional<MinigameSession> findByUserIdAndGameCodeAndActiveTrue(String userId, String gameCode);
}
