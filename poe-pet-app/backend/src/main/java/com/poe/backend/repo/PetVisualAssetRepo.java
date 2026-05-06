package com.poe.backend.repo;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.poe.backend.model.PetVisualAsset;

/** Cosmetic catalog persistence (pet mood assets + scene layers). */
public interface PetVisualAssetRepo extends MongoRepository<PetVisualAsset, String> {
    /** List active visual assets. */
    List<PetVisualAsset> findByActiveTrue();
    /** List active visual assets filtered by asset type (PET_MOOD/BACKGROUND/FOREGROUND). */
    List<PetVisualAsset> findByAssetTypeAndActiveTrue(String assetType);
    /** Find an active visual asset by code. */
    Optional<PetVisualAsset> findByCodeAndActiveTrue(String code);
}
