package com.poe.backend.bootstrap;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.poe.backend.model.ShopItem;
import com.poe.backend.repo.ShopItemRepo;

/**
 * Ensures shop catalog rows exist when Mongo was created without docker init seeds
 * (same catalog as {@code mongodb/seed-shop-items.json}, mirrored under {@code classpath:bootstrap/shop-catalog.json}).
 */
@Component
@Order(45)
@ConditionalOnProperty(name = "app.bootstrap.shop.enabled", havingValue = "true", matchIfMissing = true)
public class ShopCatalogSeeder implements ApplicationRunner {
    private static final String CATALOG_RESOURCE = "bootstrap/shop-catalog.json";

    private final ShopItemRepo shopItemRepo;
    private final ObjectMapper objectMapper;

    public ShopCatalogSeeder(ShopItemRepo shopItemRepo, ObjectMapper objectMapper) {
        this.shopItemRepo = shopItemRepo;
        this.objectMapper = objectMapper.copy().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    @Override
    public void run(ApplicationArguments args) throws IOException {
        ClassPathResource resource = new ClassPathResource(CATALOG_RESOURCE);
        if (!resource.exists()) {
            return;
        }
        try (InputStream in = resource.getInputStream()) {
            List<ShopItem> items = objectMapper.readValue(in, new TypeReference<>() {});
            for (ShopItem row : items) {
                if (row.code == null || row.code.isBlank()) {
                    continue;
                }
                row.id = null;
                Optional<ShopItem> existing = shopItemRepo.findByCode(row.code);
                if (existing.isEmpty()) {
                    shopItemRepo.save(row);
                }
            }
        }
    }
}
