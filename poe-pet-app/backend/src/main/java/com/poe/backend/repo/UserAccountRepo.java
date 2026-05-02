package com.poe.backend.repo;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.poe.backend.model.UserAccount;

public interface UserAccountRepo extends MongoRepository<UserAccount, String> {
    Optional<UserAccount> findByEmail(String email);
}
