package com.poe.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import com.poe.backend.model.PetState;
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

class AppServiceAiChatTest {

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
    private final AiGatewayClient aiGatewayClient = mock(AiGatewayClient.class);
    private final AiChatContextAssembler aiChatContextAssembler = mock(AiChatContextAssembler.class);

    private AppService appService;

    @BeforeEach
    void init() {
        appService = new AppService(
                userAccountRepo,
                userTokenRepo,
                petStateRepo,
                walletRepo,
                shopItemRepo,
                inventoryRepo,
                minigameRepo,
                minigameSessionRepo,
                petVisualAssetRepo,
                mock(JavaMailSender.class),
                aiGatewayClient,
                activityHistoryService,
                notificationPreferenceService,
                aiChatContextAssembler);
        ReflectionTestUtils.setField(appService, "webBaseUrl", "http://web.test");
        ReflectionTestUtils.setField(appService, "apiBaseUrl", "http://api.test");
        ReflectionTestUtils.setField(appService, "aiPersonaAddendum", "Stay tiny.");
        ReflectionTestUtils.setField(appService, "aiChatMaxUserMessageChars", 50);
        ReflectionTestUtils.setField(appService, "aiChatMaxConversationTurns", 2);
        ReflectionTestUtils.setField(appService, "aiChatMaxAssistantChars", 20);
        ReflectionTestUtils.setField(appService, "aiChatMaxContextChars", 6000);

        when(aiChatContextAssembler.assemble(anyString(), any(PetState.class), nullable(String.class), anyString(), anyInt()))
                .thenReturn("prefix");

        PetState pet = new PetState();
        pet.userId = "u1";
        pet.speciesCode = "dog";
        pet.name = "Rex";
        pet.hunger = 40;
        pet.happiness = 50;
        pet.energy = 60;
        pet.lastSimulationAt = Instant.now();
        pet.activeEffects = new ArrayList<>();
        when(petStateRepo.findByUserId("u1")).thenReturn(Optional.of(pet));
        when(petStateRepo.save(any(PetState.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Wallet wallet = new Wallet();
        wallet.userId = "u1";
        wallet.coins = 100;
        when(walletRepo.findByUserId("u1")).thenReturn(Optional.of(wallet));
    }

    @Test
    void aiChatClampsAssistantText() throws Exception {
        when(aiGatewayClient.isEnabled()).thenReturn(true);
        when(aiGatewayClient.chat(any())).thenReturn(Map.of("assistantText", "a".repeat(200)));

        Map<String, Object> res = appService.aiChat("u1", List.of(), "hello");

        String text = (String) res.get("assistantText");
        assertEquals(20, text.length());
        assertTrue(text.endsWith("…"));
    }

    @Test
    void aiChatTruncatesUserMessageInGatewayPayload() throws Exception {
        when(aiGatewayClient.isEnabled()).thenReturn(true);
        when(aiGatewayClient.chat(any())).thenReturn(Map.of("assistantText", "woof"));

        appService.aiChat("u1", List.of(), "x".repeat(100));

        verify(aiChatContextAssembler).assemble(eq("u1"), any(PetState.class), isNull(), eq("Stay tiny."), eq(6000));
        verify(aiGatewayClient).chat(argThat(m -> {
            Object msg = m.get("message");
            Object cp = m.get("contextPrefix");
            return msg instanceof String s && s.length() == 50 && s.endsWith("…")
                    && "prefix".equals(cp);
        }));
    }

    @Test
    void aiChatBlankMessageReturnsFallback() {
        when(aiGatewayClient.isEnabled()).thenReturn(true);

        Map<String, Object> res = appService.aiChat("u1", List.of(), "   ");

        assertEquals(true, res.get("fallbackUsed"));
        assertEquals("empty_message", res.get("fallbackReason"));
    }

    @Test
    void aiChatMarksFallbackWhenModelReturnsExactSpeciesNoise() throws Exception {
        when(aiGatewayClient.isEnabled()).thenReturn(true);
        when(aiGatewayClient.chat(any())).thenReturn(Map.of("assistantText", "*wags tail*", "usage", Map.of("latencyMs", 1)));

        Map<String, Object> res = appService.aiChat("u1", List.of(), "explain yourself");

        assertEquals(true, res.get("fallbackUsed"));
        assertEquals("model_short_reply", res.get("fallbackReason"));
        assertEquals("*wags tail*", res.get("assistantText"));
    }
}
