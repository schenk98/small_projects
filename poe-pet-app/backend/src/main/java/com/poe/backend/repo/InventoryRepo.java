package com.poe.backend.repo;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.poe.backend.model.InventoryItem;

/**
 * Inventory persistence.
 *
 * Each inventory document represents an owned quantity of a shop item code for a user.
 */
public interface InventoryRepo extends MongoRepository<InventoryItem, String> {
    /** List all inventory rows owned by a user. */
    List<InventoryItem> findByUserId(String userId);
    /** Look up a specific inventory row by user and item code. */
    Optional<InventoryItem> findByUserIdAndItemCode(String userId, String itemCode);
}
