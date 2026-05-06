package com.poe.backend.repo;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.poe.backend.model.PetState;

/** Pet persistence (virtual pet simulation state per user). */
public interface PetStateRepo extends MongoRepository<PetState, String> {
    /** Find a user's pet state document by user id. */
    Optional<PetState> findByUserId(String userId);
}
