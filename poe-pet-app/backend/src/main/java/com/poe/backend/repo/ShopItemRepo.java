package com.poe.backend.repo;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.poe.backend.model.ShopItem;

/** Shop catalog persistence. */
public interface ShopItemRepo extends MongoRepository<ShopItem, String> {
    /** List active shop items (may include non-player-visible staging items). */
    List<ShopItem> findByActiveTrue();
    /** Find an active shop item by its code. */
    Optional<ShopItem> findByCodeAndActiveTrue(String code);
}
