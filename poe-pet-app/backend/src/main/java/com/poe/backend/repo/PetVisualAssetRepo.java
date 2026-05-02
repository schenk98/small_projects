package com.poe.backend.repo;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.poe.backend.model.PetVisualAsset;

public interface PetVisualAssetRepo extends MongoRepository<PetVisualAsset, String> {
    List<PetVisualAsset> findByActiveTrue();
    List<PetVisualAsset> findByAssetTypeAndActiveTrue(String assetType);
    Optional<PetVisualAsset> findByCodeAndActiveTrue(String code);
}
