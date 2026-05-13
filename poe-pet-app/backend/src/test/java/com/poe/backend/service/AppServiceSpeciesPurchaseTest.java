package com.poe.backend.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.poe.backend.model.PetState;
import com.poe.backend.model.ShopItem;
import com.poe.backend.model.Wallet;
import com.poe.backend.repo.InventoryRepo;
import com.poe.backend.repo.MinigameRepo;
import com.poe.backend.repo.MinigameSessionRepo;
import com.poe.backend.repo.PetStateRepo;
import com.poe.backend.repo.PetVisualAssetRepo;
import com.poe.backend.repo.ShopItemRepo;
import com.poe.backend.repo.UserAccountRepo;
import com.poe.backend.repo.UserTokenRepo;
import com.poe.backend.repo.WalletRepo;
import com.poe.backend.sql.service.ActivityHistoryService;
import com.poe.backend.sql.service.NotificationPreferenceService;

class AppServiceSpeciesPurchaseTest {
    private final UserAccountRepo userAccountRepo = mock(UserAccountRepo.class);
    private final UserTokenRepo userTokenRepo = mock(UserTokenRepo.class);
    private final PetStateRepo petStateRepo = mock(PetStateRepo.class);
    private final WalletRepo walletRepo = mock(WalletRepo.class);
    private final ShopItemRepo shopItemRepo = mock(ShopItemRepo.class);
    private final InventoryRepo inventoryRepo = mock(InventoryRepo.class);
    private final MinigameRepo minigameRepo = mock(MinigameRepo.class);
    private final MinigameSessionRepo minigameSessionRepo = mock(MinigameSessionRepo.class);
    private final PetVisualAssetRepo petVisualAssetRepo = mock(PetVisualAssetRepo.class);
    private final ActivityHistoryService activityHistoryService = mock(ActivityHistoryService.class);
    private final NotificationPreferenceService notificationPreferenceService = mock(NotificationPreferenceService.class);

    private final AppService appService = new AppService(
            userAccountRepo,
            userTokenRepo,
            petStateRepo,
            walletRepo,
            shopItemRepo,
            inventoryRepo,
            minigameRepo,
            minigameSessionRepo,
            petVisualAssetRepo,
            mock(org.springframework.mail.javamail.JavaMailSender.class),
            mock(AiGatewayClient.class),
            activityHistoryService,
            notificationPreferenceService,
            mock(AiChatContextAssembler.class));

    @Test
    void purchaseSpeciesUnlocksPetAndChargesCoins() {
        PetState pet = pet("dog", "dog", "cat");
        Wallet wallet = wallet(1200);
        ShopItem item = speciesItem("species_goldfish", "goldfish", 1000);
        when(shopItemRepo.findByCodeAndActiveTrue("species_goldfish")).thenReturn(Optional.of(item));
        when(walletRepo.findByUserId("u1")).thenReturn(Optional.of(wallet));
        when(petStateRepo.findByUserId("u1")).thenReturn(Optional.of(pet));

        Map<String, Object> result = appService.purchase("u1", "species_goldfish");

        assertTrue(Boolean.TRUE.equals(result.get("ok")));
        assertTrue(pet.ownedSpeciesCodes.contains("goldfish"));
        assertTrue(wallet.coins == 200);
        verify(walletRepo).save(wallet);
        verify(petStateRepo).save(pet);
    }

    @Test
    void setSpeciesRejectsLockedPet() {
        PetState pet = pet("dog", "dog", "cat");
        when(petStateRepo.findByUserId("u1")).thenReturn(Optional.of(pet));

        assertThrows(RuntimeException.class, () -> appService.setSpecies("u1", "goldfish"));
    }

    @Test
    void setSpeciesAllowsPurchasedPet() {
        PetState pet = pet("dog", "dog", "cat", "goldfish");
        Wallet wallet = wallet(0);
        when(petStateRepo.findByUserId("u1")).thenReturn(Optional.of(pet));
        when(walletRepo.findByUserId("u1")).thenReturn(Optional.of(wallet));

        appService.setSpecies("u1", "goldfish");

        assertTrue("goldfish".equals(pet.speciesCode));
        verify(petStateRepo).save(pet);
    }

    private static PetState pet(String speciesCode, String... ownedSpeciesCodes) {
        PetState pet = new PetState();
        pet.userId = "u1";
        pet.speciesCode = speciesCode;
        pet.ownedSpeciesCodes = new ArrayList<>(List.of(ownedSpeciesCodes));
        pet.ownedVisualAssetCodes = new ArrayList<>();
        pet.lastSimulationAt = Instant.now();
        return pet;
    }

    private static Wallet wallet(int coins) {
        Wallet wallet = new Wallet();
        wallet.userId = "u1";
        wallet.coins = coins;
        return wallet;
    }

    private static ShopItem speciesItem(String code, String speciesCode, int priceCoins) {
        ShopItem item = new ShopItem();
        item.code = code;
        item.type = "SPECIES";
        item.priceCoins = priceCoins;
        item.active = true;
        item.playerVisible = true;
        item.effects = List.of(Map.of("kind", "GRANT_SPECIES", "speciesCode", speciesCode));
        return item;
    }
}
