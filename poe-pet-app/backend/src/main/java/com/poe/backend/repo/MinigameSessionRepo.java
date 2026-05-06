package com.poe.backend.repo;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.poe.backend.model.MinigameSession;

/** Session persistence for session-based minigames (currently Higher/Lower). */
public interface MinigameSessionRepo extends MongoRepository<MinigameSession, String> {
    /** Find the active session for a user+game pair, if any. */
    Optional<MinigameSession> findByUserIdAndGameCodeAndActiveTrue(String userId, String gameCode);
}
