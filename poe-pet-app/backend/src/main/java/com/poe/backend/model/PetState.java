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
    /** Owning user id (same id as {@link com.poe.backend.model.UserAccount#id}). */
    public String userId;
    /** Hunger percent 0..100. At <=0, the pet is considered "playing dead". */
    public double hunger;
    /** Happiness percent 0..100. */
    public double happiness;
    /** Energy percent 0..100. Minigames consume energy; time regenerates it. */
    public double energy;
    /** Optional user-facing pet name. Used by the AI chat prefix and some UI labels. */
    public String name;
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
    /** Timed and untimed effects applied by consumables (energy regen multipliers, coin multipliers, etc.). */
    public List<ActiveEffect> activeEffects = new ArrayList<>();

    public static class ActiveEffect {
        /** Groups effects from the same item so they can be overwritten/reset. */
        public String effectKey;
        /** Regen additive (energy) uses ENERGY_REGEN; coin payouts use COIN_MULT. Null treated as ENERGY_REGEN for older records. */
        public String bonusKind;
        /** Effect strength: multiplier factor or additive magnitude depending on kind. */
        public double value;
        public Instant expiresAt;
    }
}
