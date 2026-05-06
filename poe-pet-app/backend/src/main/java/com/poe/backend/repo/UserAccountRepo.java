package com.poe.backend.repo;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.poe.backend.model.UserAccount;

/** User account persistence. */
public interface UserAccountRepo extends MongoRepository<UserAccount, String> {
    /** Find user by normalized email. */
    Optional<UserAccount> findByEmail(String email);
}
