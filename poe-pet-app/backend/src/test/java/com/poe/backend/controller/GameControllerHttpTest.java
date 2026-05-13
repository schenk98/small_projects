package com.poe.backend.controller;

import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.poe.backend.security.CurrentUser;
import com.poe.backend.service.AppService;
import com.poe.backend.sql.service.NotificationPreferenceService;
import com.poe.backend.sql.service.ProgressQueryService;
import com.poe.backend.sql.view.AchievementProgressView;
import com.poe.backend.sql.view.ActivityEventView;
import com.poe.backend.sql.view.DailyChallengeView;
import com.poe.backend.sql.view.NotificationPreferenceView;
import com.poe.backend.sql.view.ProgressSummaryView;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;

class GameControllerHttpTest {
    private final AppService appService = org.mockito.Mockito.mock(AppService.class);
    private final ProgressQueryService progressQueryService = org.mockito.Mockito.mock(ProgressQueryService.class);
    private final NotificationPreferenceService notificationPreferenceService =
            org.mockito.Mockito.mock(NotificationPreferenceService.class);

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        GameController controller = new GameController(appService, progressQueryService, notificationPreferenceService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new ApiExceptionHandler())
                .addFilters(currentUserFilter("u1"))
                .build();
    }

    @AfterEach
    void tearDown() {
        CurrentUser.clear();
    }

    @Test
    void progressSummaryEndpointReturnsAchievementAndActivityPayload() throws Exception {
        when(progressQueryService.getSummary("u1")).thenReturn(new ProgressSummaryView(
                List.of(new DailyChallengeView(
                        3L,
                        java.time.LocalDate.parse("2026-05-12"),
                        1,
                        "FINISH_MINIGAME",
                        "Puzzle refresh",
                        "Finish Puzzle Swap once today.",
                        "puzzle_swap",
                        1,
                        0,
                        0,
                        false,
                        null,
                        20,
                        false,
                        null,
                        null)),
                List.of(new AchievementProgressView(
                        "pet_namer",
                        "Name Tag",
                        "Give your pet a custom name.",
                        "pet",
                        1,
                        1,
                        100,
                        true,
                        Instant.parse("2026-05-12T12:00:00Z"),
                        Instant.parse("2026-05-12T12:00:00Z"))),
                List.of(new ActivityEventView(
                        8L,
                        "SHOP_PURCHASED",
                        "shop",
                        Instant.parse("2026-05-12T12:05:00Z"),
                        "Miki",
                        "dog",
                        80.0,
                        90.0,
                        70.0,
                        210,
                        java.util.Map.of("itemCode", "apple")))));

        mockMvc.perform(get("/api/progress/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dailyChallenges[0].title").value("Puzzle refresh"))
                .andExpect(jsonPath("$.achievements[0].code").value("pet_namer"))
                .andExpect(jsonPath("$.achievements[0].unlocked").value(true))
                .andExpect(jsonPath("$.recentActivity[0].eventType").value("SHOP_PURCHASED"))
                .andExpect(jsonPath("$.recentActivity[0].details.itemCode").value("apple"));
    }

    @Test
    void notificationPreferencesGetReturnsCurrentToggles() throws Exception {
        when(notificationPreferenceService.getForUser("u1")).thenReturn(
                new NotificationPreferenceView(true, false, Instant.parse("2026-05-12T12:10:00Z")));

        mockMvc.perform(get("/api/notification-preferences"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lowHungerEnabled").value(true))
                .andExpect(jsonPath("$.dailyAiSummaryEnabled").value(false));
    }

    @Test
    void notificationPreferencesPostUpdatesToggles() throws Exception {
        when(notificationPreferenceService.updateForUser("u1", true, true)).thenReturn(
                new NotificationPreferenceView(true, true, Instant.parse("2026-05-12T12:15:00Z")));

        mockMvc.perform(post("/api/notification-preferences")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"lowHungerEnabled":true,"dailyAiSummaryEnabled":true}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lowHungerEnabled").value(true))
                .andExpect(jsonPath("$.dailyAiSummaryEnabled").value(true));
    }

    @Test
    void setSpeciesEndpointAcceptsFoxPayload() throws Exception {
        when(appService.setSpecies("u1", "fox")).thenReturn(java.util.Map.of("ok", true, "speciesCode", "fox"));

        mockMvc.perform(post("/api/pet-visuals/species")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"speciesCode":"fox"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.speciesCode").value("fox"));

        verify(appService).setSpecies("u1", "fox");
    }

    @Test
    void setSpeciesEndpointAcceptsPandaPayload() throws Exception {
        when(appService.setSpecies("u1", "panda")).thenReturn(java.util.Map.of("ok", true, "speciesCode", "panda"));

        mockMvc.perform(post("/api/pet-visuals/species")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"speciesCode":"panda"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.speciesCode").value("panda"));

        verify(appService).setSpecies("u1", "panda");
    }

    @Test
    void setSpeciesEndpointAcceptsGoldfishPayload() throws Exception {
        when(appService.setSpecies("u1", "goldfish")).thenReturn(java.util.Map.of("ok", true, "speciesCode", "goldfish"));

        mockMvc.perform(post("/api/pet-visuals/species")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"speciesCode":"goldfish"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.speciesCode").value("goldfish"));

        verify(appService).setSpecies("u1", "goldfish");
    }

    @Test
    void setSpeciesEndpointAcceptsLizardPayload() throws Exception {
        when(appService.setSpecies("u1", "lizard")).thenReturn(java.util.Map.of("ok", true, "speciesCode", "lizard"));

        mockMvc.perform(post("/api/pet-visuals/species")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"speciesCode":"lizard"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.speciesCode").value("lizard"));

        verify(appService).setSpecies("u1", "lizard");
    }

    @Test
    void aiInfoEndpointDelegatesToAppService() throws Exception {
        when(appService.getAiChatInfo()).thenReturn(java.util.Map.of(
                "gatewayConfigured", false,
                "maxUserMessageChars", 1800));

        mockMvc.perform(get("/api/ai/info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.gatewayConfigured").value(false))
                .andExpect(jsonPath("$.maxUserMessageChars").value(1800));

        verify(appService).getAiChatInfo();
    }

    private Filter currentUserFilter(String userId) {
        return new Filter() {
            @Override
            public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
                    throws IOException, ServletException {
                CurrentUser.set(userId);
                try {
                    chain.doFilter(request, response);
                } finally {
                    CurrentUser.clear();
                }
            }
        };
    }
}
