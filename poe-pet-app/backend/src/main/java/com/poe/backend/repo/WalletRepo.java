package com.poe.backend.repo;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.poe.backend.model.Wallet;

public interface WalletRepo extends MongoRepository<Wallet, String> {
    Optional<Wallet> findByUserId(String userId);
}
