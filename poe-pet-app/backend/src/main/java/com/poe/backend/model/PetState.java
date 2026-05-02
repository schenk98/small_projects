package com.poe.backend.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("pets")
public class PetState {
    @Id
    public String id;
    public String userId;
    public double hunger;
    public double happiness;
    public double energy;
    /** Selected base pet species (starter choices: dog/cat). */
    public String speciesCode;
    /** Optional mood slot overrides: mood -> assetCode. */
    public Map<String, String> moodAssetCodes = new HashMap<>();
    /** Cosmetic catalog codes this user has unlocked (backgrounds, foregrounds, alternate mood art). */
    public List<String> ownedVisualAssetCodes = new ArrayList<>();
    /** Equipped scene background asset code, or null for default (CSS / none). */
    public String equippedBackgroundAssetCode;
    /** Equipped foreground overlay asset code, or null. */
    public String equippedForegroundAssetCode;
    public Instant lastSimulationAt;
    public List<ActiveEffect> activeEffects = new ArrayList<>();

    public static class ActiveEffect {
        public String effectKey;
        /** Regen additive (energy) uses ENERGY_REGEN; coin payouts use COIN_MULT. Null treated as ENERGY_REGEN for older records. */
        public String bonusKind;
        public double value;
        public Instant expiresAt;
    }
}
