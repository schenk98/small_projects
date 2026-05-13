package com.poe.backend.bootstrap;

import java.util.List;
import java.util.Optional;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.poe.backend.model.PetVisualAsset;
import com.poe.backend.repo.PetVisualAssetRepo;

/**
 * Ensures starter pet mood visuals that may be missing from older Mongo seeds.
 *
 * This keeps new frontend mood states usable even when the developer already has
 * an existing local Mongo volume and does not re-run the full seed script.
 */
@Component
@Order(40)
@ConditionalOnProperty(name = "app.bootstrap.petVisuals.enabled", havingValue = "true", matchIfMissing = true)
public class PetVisualCatalogSeeder implements ApplicationRunner {
    private final PetVisualAssetRepo petVisualAssetRepo;

    public PetVisualCatalogSeeder(PetVisualAssetRepo petVisualAssetRepo) {
        this.petVisualAssetRepo = petVisualAssetRepo;
    }

    @Override
    public void run(ApplicationArguments args) {
        for (PetVisualAsset asset : starterMoodAssetsToEnsure()) {
            Optional<PetVisualAsset> existing = petVisualAssetRepo.findByCodeAndActiveTrue(asset.code);
            if (existing.isEmpty()) {
                petVisualAssetRepo.save(asset);
            }
        }
    }

    private static List<PetVisualAsset> starterMoodAssetsToEnsure() {
        return List.of(
                petMood("dog_happy_default", "dog", "happy", "Dog Happy (Default)", "/pet-assets/dog/happy-default.png"),
                petMood("dog_sad_default", "dog", "sad", "Dog Sad (Default)", "/pet-assets/dog/sad-default.png"),
                petMood("dog_hungry_default", "dog", "hungry", "Dog Hungry (Default)", "/pet-assets/dog/hungry-default.png"),
                petMood("dog_tired_default", "dog", "tired", "Dog Tired (Default)", "/pet-assets/dog/tired-default.png"),
                petMood("dog_thinking_default", "dog", "thinking", "Dog Thinking (Default)", "/pet-assets/dog/thinking-default.png"),
                petMood("dog_playing_dead_default", "dog", "playing_dead", "Dog Playing Dead (Default)", "/pet-assets/dog/playing-dead-default.png"),
                petMood("cat_happy_default", "cat", "happy", "Cat Happy (Default)", "/pet-assets/cat/happy-default.png"),
                petMood("cat_sad_default", "cat", "sad", "Cat Sad (Default)", "/pet-assets/cat/sad-default.png"),
                petMood("cat_hungry_default", "cat", "hungry", "Cat Hungry (Default)", "/pet-assets/cat/hungry-default.png"),
                petMood("cat_tired_default", "cat", "tired", "Cat Tired (Default)", "/pet-assets/cat/tired-default.png"),
                petMood("cat_thinking_default", "cat", "thinking", "Cat Thinking (Default)", "/pet-assets/cat/thinking-default.png"),
                petMood("cat_playing_dead_default", "cat", "playing_dead", "Cat Playing Dead (Default)", "/pet-assets/cat/playing-dead-default.png"),
                petMood("penguin_happy_default", "penguin", "happy", "Penguin Happy (Default)", "/pet-assets/penguin/happy-default.png"),
                petMood("penguin_sad_default", "penguin", "sad", "Penguin Sad (Default)", "/pet-assets/penguin/sad-default.png"),
                petMood("penguin_hungry_default", "penguin", "hungry", "Penguin Hungry (Default)", "/pet-assets/penguin/hungry-default.png"),
                petMood("penguin_tired_default", "penguin", "tired", "Penguin Tired (Default)", "/pet-assets/penguin/tired-default.png"),
                petMood("penguin_thinking_default", "penguin", "thinking", "Penguin Thinking (Default)", "/pet-assets/penguin/thinking-default.png"),
                petMood("penguin_playing_dead_default", "penguin", "playing_dead", "Penguin Playing Dead (Default)", "/pet-assets/penguin/playing-dead-default.png"),
                petMood("fox_happy_default", "fox", "happy", "Fox Happy (Default)", "/pet-assets/fox/happy-default.png"),
                petMood("fox_sad_default", "fox", "sad", "Fox Sad (Default)", "/pet-assets/fox/sad-default.png"),
                petMood("fox_hungry_default", "fox", "hungry", "Fox Hungry (Default)", "/pet-assets/fox/hungry-default.png"),
                petMood("fox_tired_default", "fox", "tired", "Fox Tired (Default)", "/pet-assets/fox/tired-default.png"),
                petMood("fox_thinking_default", "fox", "thinking", "Fox Thinking (Default)", "/pet-assets/fox/thinking-default.png"),
                petMood("fox_playing_dead_default", "fox", "playing_dead", "Fox Playing Dead (Default)", "/pet-assets/fox/playing-dead-default.png"),
                petMood("hamster_happy_default", "hamster", "happy", "Hamster Happy (Default)", "/pet-assets/hamster/happy-default.png"),
                petMood("hamster_sad_default", "hamster", "sad", "Hamster Sad (Default)", "/pet-assets/hamster/sad-default.png"),
                petMood("hamster_hungry_default", "hamster", "hungry", "Hamster Hungry (Default)", "/pet-assets/hamster/hungry-default.png"),
                petMood("hamster_tired_default", "hamster", "tired", "Hamster Tired (Default)", "/pet-assets/hamster/tired-default.png"),
                petMood("hamster_thinking_default", "hamster", "thinking", "Hamster Thinking (Default)", "/pet-assets/hamster/thinking-default.png"),
                petMood("hamster_playing_dead_default", "hamster", "playing_dead", "Hamster Playing Dead (Default)", "/pet-assets/hamster/playing-dead-default.png"),
                petMood("tiger_happy_default", "tiger", "happy", "Tiger Happy (Default)", "/pet-assets/tiger/happy-default.png"),
                petMood("tiger_sad_default", "tiger", "sad", "Tiger Sad (Default)", "/pet-assets/tiger/sad-default.png"),
                petMood("tiger_hungry_default", "tiger", "hungry", "Tiger Hungry (Default)", "/pet-assets/tiger/hungry-default.png"),
                petMood("tiger_tired_default", "tiger", "tired", "Tiger Tired (Default)", "/pet-assets/tiger/tired-default.png"),
                petMood("tiger_thinking_default", "tiger", "thinking", "Tiger Thinking (Default)", "/pet-assets/tiger/thinking-default.png"),
                petMood("tiger_playing_dead_default", "tiger", "playing_dead", "Tiger Playing Dead (Default)", "/pet-assets/tiger/playing-dead-default.png"),
                petMood("lion_happy_default", "lion", "happy", "Lion Happy (Default)", "/pet-assets/lion/happy-default.png"),
                petMood("lion_sad_default", "lion", "sad", "Lion Sad (Default)", "/pet-assets/lion/sad-default.png"),
                petMood("lion_hungry_default", "lion", "hungry", "Lion Hungry (Default)", "/pet-assets/lion/hungry-default.png"),
                petMood("lion_tired_default", "lion", "tired", "Lion Tired (Default)", "/pet-assets/lion/tired-default.png"),
                petMood("lion_thinking_default", "lion", "thinking", "Lion Thinking (Default)", "/pet-assets/lion/thinking-default.png"),
                petMood("lion_playing_dead_default", "lion", "playing_dead", "Lion Playing Dead (Default)", "/pet-assets/lion/playing-dead-default.png"),
                petMood("horse_happy_default", "horse", "happy", "Horse Happy (Default)", "/pet-assets/horse/happy-default.png"),
                petMood("horse_sad_default", "horse", "sad", "Horse Sad (Default)", "/pet-assets/horse/sad-default.png"),
                petMood("horse_hungry_default", "horse", "hungry", "Horse Hungry (Default)", "/pet-assets/horse/hungry-default.png"),
                petMood("horse_tired_default", "horse", "tired", "Horse Tired (Default)", "/pet-assets/horse/tired-default.png"),
                petMood("horse_thinking_default", "horse", "thinking", "Horse Thinking (Default)", "/pet-assets/horse/thinking-default.png"),
                petMood("horse_playing_dead_default", "horse", "playing_dead", "Horse Playing Dead (Default)", "/pet-assets/horse/playing-dead-default.png"),
                petMood("parrot_happy_default", "parrot", "happy", "Parrot Happy (Default)", "/pet-assets/parrot/happy-default.png"),
                petMood("parrot_sad_default", "parrot", "sad", "Parrot Sad (Default)", "/pet-assets/parrot/sad-default.png"),
                petMood("parrot_hungry_default", "parrot", "hungry", "Parrot Hungry (Default)", "/pet-assets/parrot/hungry-default.png"),
                petMood("parrot_tired_default", "parrot", "tired", "Parrot Tired (Default)", "/pet-assets/parrot/tired-default.png"),
                petMood("parrot_thinking_default", "parrot", "thinking", "Parrot Thinking (Default)", "/pet-assets/parrot/thinking-default.png"),
                petMood("parrot_playing_dead_default", "parrot", "playing_dead", "Parrot Playing Dead (Default)", "/pet-assets/parrot/playing-dead-default.png"),
                petMood("unicorn_happy_default", "unicorn", "happy", "Unicorn Happy (Default)", "/pet-assets/unicorn/happy-default.png"),
                petMood("unicorn_sad_default", "unicorn", "sad", "Unicorn Sad (Default)", "/pet-assets/unicorn/sad-default.png"),
                petMood("unicorn_hungry_default", "unicorn", "hungry", "Unicorn Hungry (Default)", "/pet-assets/unicorn/hungry-default.png"),
                petMood("unicorn_tired_default", "unicorn", "tired", "Unicorn Tired (Default)", "/pet-assets/unicorn/tired-default.png"),
                petMood("unicorn_thinking_default", "unicorn", "thinking", "Unicorn Thinking (Default)", "/pet-assets/unicorn/thinking-default.png"),
                petMood("unicorn_playing_dead_default", "unicorn", "playing_dead", "Unicorn Playing Dead (Default)", "/pet-assets/unicorn/playing-dead-default.png"),
                petMood("midnight_cat_happy_default", "midnight_cat", "happy", "Midnight Cat Happy (Default)", "/pet-assets/midnight-cat/happy-default.png"),
                petMood("midnight_cat_sad_default", "midnight_cat", "sad", "Midnight Cat Sad (Default)", "/pet-assets/midnight-cat/sad-default.png"),
                petMood("midnight_cat_hungry_default", "midnight_cat", "hungry", "Midnight Cat Hungry (Default)", "/pet-assets/midnight-cat/hungry-default.png"),
                petMood("midnight_cat_tired_default", "midnight_cat", "tired", "Midnight Cat Tired (Default)", "/pet-assets/midnight-cat/tired-default.png"),
                petMood("midnight_cat_thinking_default", "midnight_cat", "thinking", "Midnight Cat Thinking (Default)", "/pet-assets/midnight-cat/thinking-default.png"),
                petMood("midnight_cat_playing_dead_default", "midnight_cat", "playing_dead", "Midnight Cat Playing Dead (Default)", "/pet-assets/midnight-cat/playing-dead-default.png"),
                petMood("panda_happy_default", "panda", "happy", "Panda Happy (Default)", "/pet-assets/panda/happy-default.png"),
                petMood("panda_sad_default", "panda", "sad", "Panda Sad (Default)", "/pet-assets/panda/sad-default.png"),
                petMood("panda_hungry_default", "panda", "hungry", "Panda Hungry (Default)", "/pet-assets/panda/hungry-default.png"),
                petMood("panda_tired_default", "panda", "tired", "Panda Tired (Default)", "/pet-assets/panda/tired-default.png"),
                petMood("panda_thinking_default", "panda", "thinking", "Panda Thinking (Default)", "/pet-assets/panda/thinking-default.png"),
                petMood("panda_playing_dead_default", "panda", "playing_dead", "Panda Playing Dead (Default)", "/pet-assets/panda/playing-dead-default.png"),
                petMood("goldfish_happy_default", "goldfish", "happy", "Goldfish Happy (Default)", "/pet-assets/goldfish/happy-default.png"),
                petMood("goldfish_sad_default", "goldfish", "sad", "Goldfish Sad (Default)", "/pet-assets/goldfish/sad-default.png"),
                petMood("goldfish_hungry_default", "goldfish", "hungry", "Goldfish Hungry (Default)", "/pet-assets/goldfish/hungry-default.png"),
                petMood("goldfish_tired_default", "goldfish", "tired", "Goldfish Tired (Default)", "/pet-assets/goldfish/tired-default.png"),
                petMood("goldfish_thinking_default", "goldfish", "thinking", "Goldfish Thinking (Default)", "/pet-assets/goldfish/thinking-default.png"),
                petMood("goldfish_playing_dead_default", "goldfish", "playing_dead", "Goldfish Playing Dead (Default)", "/pet-assets/goldfish/playing-dead-default.png"),
                petMood("lizard_happy_default", "lizard", "happy", "Lizard Happy (Default)", "/pet-assets/lizard/happy-default.png"),
                petMood("lizard_sad_default", "lizard", "sad", "Lizard Sad (Default)", "/pet-assets/lizard/sad-default.png"),
                petMood("lizard_hungry_default", "lizard", "hungry", "Lizard Hungry (Default)", "/pet-assets/lizard/hungry-default.png"),
                petMood("lizard_tired_default", "lizard", "tired", "Lizard Tired (Default)", "/pet-assets/lizard/tired-default.png"),
                petMood("lizard_thinking_default", "lizard", "thinking", "Lizard Thinking (Default)", "/pet-assets/lizard/thinking-default.png"),
                petMood("lizard_playing_dead_default", "lizard", "playing_dead", "Lizard Playing Dead (Default)", "/pet-assets/lizard/playing-dead-default.png"),
                visualLayer("bg_aurora_default", "BACKGROUND", "Aurora Hills", "/cosmetic-staging/backgrounds/theme-aurora.svg", false),
                visualLayer("bg_candy_default", "BACKGROUND", "Candy Dream", "/cosmetic-staging/backgrounds/theme-candy.svg", false),
                visualLayer("bg_dusk_default", "BACKGROUND", "Dusk Glow", "/cosmetic-staging/backgrounds/theme-dusk.svg", false),
                visualLayer("bg_underwater_default", "BACKGROUND", "Underwater", "/cosmetic-staging/backgrounds/theme-underwater.svg", false),
                visualLayer("fg_soft_vignette_default", "FOREGROUND", "Soft vignette (free)", "/cosmetic-staging/foregrounds/soft-vignette.svg", true),
                visualLayer("fg_sparkle_default", "FOREGROUND", "Sparkle frame", "/cosmetic-staging/foregrounds/sparkle-frame.svg", false));
    }

    private static PetVisualAsset petMood(String code, String speciesCode, String moodCode, String label, String imagePath) {
        PetVisualAsset asset = new PetVisualAsset();
        asset.code = code;
        asset.assetType = "PET_MOOD";
        asset.speciesCode = speciesCode;
        asset.moodCode = moodCode;
        asset.label = label;
        asset.imagePath = imagePath;
        asset.starter = true;
        asset.active = true;
        return asset;
    }

    private static PetVisualAsset visualLayer(String code, String assetType, String label, String imagePath, boolean starter) {
        PetVisualAsset asset = new PetVisualAsset();
        asset.code = code;
        asset.assetType = assetType;
        asset.speciesCode = "all";
        asset.moodCode = "";
        asset.label = label;
        asset.imagePath = imagePath;
        asset.starter = starter;
        asset.active = true;
        return asset;
    }
}
