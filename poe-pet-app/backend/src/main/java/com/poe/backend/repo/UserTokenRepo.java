package com.poe.backend.repo;

import java.time.Instant;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.poe.backend.model.UserToken;

/**
 * Token persistence.
 *
 * Tokens are stored in Mongo so we can:
 * - revoke by marking used
 * - enforce expiration
 * - keep implementation simple for a learning project
 */
public interface UserTokenRepo extends MongoRepository<UserToken, String> {
    /** Find a not-used token of a given type (no expiration check). */
    Optional<UserToken> findByTokenAndTypeAndUsedFalse(String token, String type);
    /** Find a not-used token of a given type that is unexpired at the given time. */
    Optional<UserToken> findByTokenAndTypeAndUsedFalseAndExpiresAtAfter(String token, String type, Instant now);
}
