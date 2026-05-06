package com.poe.backend.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("pet_visual_assets")
public class PetVisualAsset {
    @Id
    public String id;
    /** Stable catalog identifier referenced from shop effects and pet ownership fields. */
    public String code;
    /** PET_MOOD, BACKGROUND, FOREGROUND (latter two planned). */
    public String assetType;
    /** dog, cat, ... */
    public String speciesCode;
    /** For PET_MOOD: happy, sad, hungry, tired, playing_dead. Empty or null for BACKGROUND / FOREGROUND. */
    public String moodCode;
    /** Human-friendly label shown in UI. */
    public String label;
    /** Frontend-relative path (e.g. /pet-assets/dog/happy-default.png). */
    public String imagePath;
    /** Starter assets are free/available for all users without purchase. */
    public boolean starter;
    public boolean active;
}
