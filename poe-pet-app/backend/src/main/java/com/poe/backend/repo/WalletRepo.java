package com.poe.backend.repo;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.poe.backend.model.Wallet;

/** Wallet persistence (coins balance per user). */
public interface WalletRepo extends MongoRepository<Wallet, String> {
    /** Find a user's wallet document by user id. */
    Optional<Wallet> findByUserId(String userId);
}
