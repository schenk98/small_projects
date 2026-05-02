package com.poe.backend.repo;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.poe.backend.model.InventoryItem;

public interface InventoryRepo extends MongoRepository<InventoryItem, String> {
    List<InventoryItem> findByUserId(String userId);
    Optional<InventoryItem> findByUserIdAndItemCode(String userId, String itemCode);
}
