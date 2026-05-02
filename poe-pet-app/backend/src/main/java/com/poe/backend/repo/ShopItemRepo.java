package com.poe.backend.repo;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.poe.backend.model.ShopItem;

public interface ShopItemRepo extends MongoRepository<ShopItem, String> {
    List<ShopItem> findByActiveTrue();
    Optional<ShopItem> findByCodeAndActiveTrue(String code);
}
