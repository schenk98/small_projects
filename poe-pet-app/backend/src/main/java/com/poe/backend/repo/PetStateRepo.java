package com.poe.backend.repo;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.poe.backend.model.PetState;

public interface PetStateRepo extends MongoRepository<PetState, String> {
    Optional<PetState> findByUserId(String userId);
}
