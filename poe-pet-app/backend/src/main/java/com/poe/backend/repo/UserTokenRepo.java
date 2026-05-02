package com.poe.backend.repo;

import java.time.Instant;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.poe.backend.model.UserToken;

public interface UserTokenRepo extends MongoRepository<UserToken, String> {
    Optional<UserToken> findByTokenAndTypeAndUsedFalse(String token, String type);
    Optional<UserToken> findByTokenAndTypeAndUsedFalseAndExpiresAtAfter(String token, String type, Instant now);
}
