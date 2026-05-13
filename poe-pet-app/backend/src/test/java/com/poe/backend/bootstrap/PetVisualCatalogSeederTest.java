package com.poe.backend.bootstrap;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.poe.backend.model.PetVisualAsset;
import com.poe.backend.repo.PetVisualAssetRepo;

class PetVisualCatalogSeederTest {
    private final PetVisualAssetRepo petVisualAssetRepo = org.mockito.Mockito.mock(PetVisualAssetRepo.class);
    private final PetVisualCatalogSeeder seeder = new PetVisualCatalogSeeder(petVisualAssetRepo);

    @Test
    void runSavesMissingStarterMoodAssetsIncludingAdditionalSpecies() throws Exception {
        when(petVisualAssetRepo.findByCodeAndActiveTrue(anyString())).thenReturn(Optional.empty());

        seeder.run(null);

        verify(petVisualAssetRepo, times(84)).save(any(PetVisualAsset.class));
        verify(petVisualAssetRepo).save(argThat(asset ->
                "dog_happy_default".equals(asset.code)
                        && "dog".equals(asset.speciesCode)
                        && "happy".equals(asset.moodCode)
                        && "/pet-assets/dog/happy-default.png".equals(asset.imagePath)));
        verify(petVisualAssetRepo).save(argThat(asset ->
                "penguin_happy_default".equals(asset.code)
                        && "penguin".equals(asset.speciesCode)
                        && "happy".equals(asset.moodCode)
                        && "/pet-assets/penguin/happy-default.png".equals(asset.imagePath)));
        verify(petVisualAssetRepo).save(argThat(asset ->
                "penguin_playing_dead_default".equals(asset.code)
                        && "penguin".equals(asset.speciesCode)
                        && "playing_dead".equals(asset.moodCode)
                        && "/pet-assets/penguin/playing-dead-default.png".equals(asset.imagePath)));
        verify(petVisualAssetRepo).save(argThat(asset ->
                "fox_happy_default".equals(asset.code)
                        && "fox".equals(asset.speciesCode)
                        && "happy".equals(asset.moodCode)
                        && "/pet-assets/fox/happy-default.png".equals(asset.imagePath)));
        verify(petVisualAssetRepo).save(argThat(asset ->
                "hamster_hungry_default".equals(asset.code)
                        && "hamster".equals(asset.speciesCode)
                        && "hungry".equals(asset.moodCode)
                        && "/pet-assets/hamster/hungry-default.png".equals(asset.imagePath)));
        verify(petVisualAssetRepo).save(argThat(asset ->
                "tiger_happy_default".equals(asset.code)
                        && "tiger".equals(asset.speciesCode)
                        && "happy".equals(asset.moodCode)
                        && "/pet-assets/tiger/happy-default.png".equals(asset.imagePath)));
        verify(petVisualAssetRepo).save(argThat(asset ->
                "lion_happy_default".equals(asset.code)
                        && "lion".equals(asset.speciesCode)
                        && "happy".equals(asset.moodCode)
                        && "/pet-assets/lion/happy-default.png".equals(asset.imagePath)));
        verify(petVisualAssetRepo).save(argThat(asset ->
                "horse_happy_default".equals(asset.code)
                        && "horse".equals(asset.speciesCode)
                        && "happy".equals(asset.moodCode)
                        && "/pet-assets/horse/happy-default.png".equals(asset.imagePath)));
        verify(petVisualAssetRepo).save(argThat(asset ->
                "parrot_happy_default".equals(asset.code)
                        && "parrot".equals(asset.speciesCode)
                        && "happy".equals(asset.moodCode)
                        && "/pet-assets/parrot/happy-default.png".equals(asset.imagePath)));
        verify(petVisualAssetRepo).save(argThat(asset ->
                "unicorn_happy_default".equals(asset.code)
                        && "unicorn".equals(asset.speciesCode)
                        && "happy".equals(asset.moodCode)
                        && "/pet-assets/unicorn/happy-default.png".equals(asset.imagePath)));
        verify(petVisualAssetRepo).save(argThat(asset ->
                "midnight_cat_happy_default".equals(asset.code)
                        && "midnight_cat".equals(asset.speciesCode)
                        && "happy".equals(asset.moodCode)
                        && "/pet-assets/midnight-cat/happy-default.png".equals(asset.imagePath)));
        verify(petVisualAssetRepo).save(argThat(asset ->
                "panda_happy_default".equals(asset.code)
                        && "panda".equals(asset.speciesCode)
                        && "happy".equals(asset.moodCode)
                        && "/pet-assets/panda/happy-default.png".equals(asset.imagePath)));
        verify(petVisualAssetRepo).save(argThat(asset ->
                "goldfish_happy_default".equals(asset.code)
                        && "goldfish".equals(asset.speciesCode)
                        && "happy".equals(asset.moodCode)
                        && "/pet-assets/goldfish/happy-default.png".equals(asset.imagePath)));
        verify(petVisualAssetRepo).save(argThat(asset ->
                "lizard_happy_default".equals(asset.code)
                        && "lizard".equals(asset.speciesCode)
                        && "happy".equals(asset.moodCode)
                        && "/pet-assets/lizard/happy-default.png".equals(asset.imagePath)));
    }

    @Test
    void runSkipsStarterMoodAssetsThatAlreadyExist() throws Exception {
        for (String code : java.util.List.of(
                "dog_happy_default",
                "dog_sad_default",
                "dog_hungry_default",
                "dog_tired_default",
                "dog_thinking_default",
                "dog_playing_dead_default",
                "cat_happy_default",
                "cat_sad_default",
                "cat_hungry_default",
                "cat_tired_default",
                "cat_thinking_default",
                "cat_playing_dead_default",
                "penguin_happy_default",
                "penguin_sad_default",
                "penguin_hungry_default",
                "penguin_tired_default",
                "penguin_thinking_default",
                "penguin_playing_dead_default",
                "fox_happy_default",
                "fox_sad_default",
                "fox_hungry_default",
                "fox_tired_default",
                "fox_thinking_default",
                "fox_playing_dead_default",
                "hamster_happy_default",
                "hamster_sad_default",
                "hamster_hungry_default",
                "hamster_tired_default",
                "hamster_thinking_default",
                "hamster_playing_dead_default",
                "tiger_happy_default",
                "tiger_sad_default",
                "tiger_hungry_default",
                "tiger_tired_default",
                "tiger_thinking_default",
                "tiger_playing_dead_default",
                "lion_happy_default",
                "lion_sad_default",
                "lion_hungry_default",
                "lion_tired_default",
                "lion_thinking_default",
                "lion_playing_dead_default",
                "horse_happy_default",
                "horse_sad_default",
                "horse_hungry_default",
                "horse_tired_default",
                "horse_thinking_default",
                "horse_playing_dead_default",
                "parrot_happy_default",
                "parrot_sad_default",
                "parrot_hungry_default",
                "parrot_tired_default",
                "parrot_thinking_default",
                "parrot_playing_dead_default",
                "unicorn_happy_default",
                "unicorn_sad_default",
                "unicorn_hungry_default",
                "unicorn_tired_default",
                "unicorn_thinking_default",
                "unicorn_playing_dead_default",
                "midnight_cat_happy_default",
                "midnight_cat_sad_default",
                "midnight_cat_hungry_default",
                "midnight_cat_tired_default",
                "midnight_cat_thinking_default",
                "midnight_cat_playing_dead_default",
                "panda_happy_default",
                "panda_sad_default",
                "panda_hungry_default",
                "panda_tired_default",
                "panda_thinking_default",
                "panda_playing_dead_default",
                "goldfish_happy_default",
                "goldfish_sad_default",
                "goldfish_hungry_default",
                "goldfish_tired_default",
                "goldfish_thinking_default",
                "goldfish_playing_dead_default",
                "lizard_happy_default",
                "lizard_sad_default",
                "lizard_hungry_default",
                "lizard_tired_default",
                "lizard_thinking_default",
                "lizard_playing_dead_default")) {
            PetVisualAsset existing = new PetVisualAsset();
            existing.code = code;
            existing.active = true;
            when(petVisualAssetRepo.findByCodeAndActiveTrue(code)).thenReturn(Optional.of(existing));
        }

        seeder.run(null);

        verify(petVisualAssetRepo, never()).save(any(PetVisualAsset.class));
    }
}
