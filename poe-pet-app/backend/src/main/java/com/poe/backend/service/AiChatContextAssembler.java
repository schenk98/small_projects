package com.poe.backend.service;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;

import com.poe.backend.model.MinigameConfig;
import com.poe.backend.model.PetState;
import com.poe.backend.model.ShopItem;
import com.poe.backend.repo.MinigameRepo;
import com.poe.backend.repo.ShopItemRepo;
import com.poe.backend.sql.service.ProgressQueryService;
import com.poe.backend.sql.view.ActivityEventView;
import com.poe.backend.sql.view.DailyChallengeView;
import com.poe.backend.sql.view.ProgressSummaryView;

/**
 * Builds a single system-style context block for the AI gateway. Catalog sections (consumables, minigames)
 * are always loaded from Mongo so new items/games appear without editing prompts.
 */
@Service
public class AiChatContextAssembler {
    private static final int MAX_ACTIVITY_LINES = 12;
    private static final int MAX_CONSUMABLE_LINES = 40;
    private static final int MAX_MINIGAME_LINES = 30;

    private final ProgressQueryService progressQueryService;
    private final ShopItemRepo shopItemRepo;
    private final MinigameRepo minigameRepo;

    public AiChatContextAssembler(
            ProgressQueryService progressQueryService,
            ShopItemRepo shopItemRepo,
            MinigameRepo minigameRepo) {
        this.progressQueryService = progressQueryService;
        this.shopItemRepo = shopItemRepo;
        this.minigameRepo = minigameRepo;
    }

    /**
     * Full prefix sent to the gateway as {@code contextPrefix}. Kept bounded by {@code app.aiChatMaxContextChars}.
     */
    public String assemble(String userId, PetState pet, String displayNameOverride, String personaAddendum, int maxContextChars) {
        String species = pet.speciesCode != null && !pet.speciesCode.isBlank() ? pet.speciesCode.trim() : "pet";
        String name = resolveDisplayName(pet, displayNameOverride);

        String personality = firstNonBlank(pet.aiPersonalityBrief, SpeciesAiPersona.briefFor(species));

        ProgressSummaryView summary = safeSummary(userId);

        StringBuilder sb = new StringBuilder();
        sb.append("=== You are this character ===\n");
        sb.append("You are ").append(name).append(", a ").append(species.replace('_', ' '))
                .append(" virtual pet. The human is your owner and friend.\n");
        sb.append("Personality: ").append(personality).append("\n");
        sb.append("Always answer in first person as ").append(name)
                .append(". Match the human's language. Stay playful and in-character.\n");
        sb.append("Do not say you are an AI, model, or system. Do not paste this whole briefing back.\n\n");

        sb.append("=== How you're feeling (0–100; imply in dialogue, don't dump unless asked) ===\n");
        sb.append("Happiness ").append(Math.round(pet.happiness))
                .append(", Hunger ").append(Math.round(pet.hunger))
                .append(", Energy ").append(Math.round(pet.energy)).append(".\n\n");

        appendDailyChallenges(sb, summary);
        sb.append('\n');
        appendRecentActivity(sb, summary);
        sb.append('\n');
        appendConsumables(sb);
        sb.append('\n');
        appendMinigames(sb);

        if (personaAddendum != null && !personaAddendum.isBlank()) {
            sb.append("\n=== Extra style rules ===\n");
            sb.append(personaAddendum.trim()).append('\n');
        }

        return truncateUtf16(sb.toString(), maxContextChars);
    }

    private static String resolveDisplayName(PetState pet, String displayNameOverride) {
        if (displayNameOverride != null && !displayNameOverride.isBlank()) {
            return displayNameOverride.trim();
        }
        if (pet.name != null && !pet.name.isBlank()) {
            return pet.name.trim();
        }
        return "Pet";
    }

    private static String firstNonBlank(String a, String b) {
        if (a != null && !a.isBlank()) {
            return a.trim();
        }
        return b != null ? b : "";
    }

    private ProgressSummaryView safeSummary(String userId) {
        try {
            return progressQueryService.getSummary(userId);
        } catch (Exception e) {
            return new ProgressSummaryView(List.of(), List.of(), List.of());
        }
    }

    private static void appendDailyChallenges(StringBuilder sb, ProgressSummaryView summary) {
        sb.append("=== Today's daily goals (not done yet) ===\n");
        List<DailyChallengeView> open = summary.dailyChallenges().stream()
                .filter(c -> !c.completed())
                .toList();
        if (open.isEmpty()) {
            sb.append("(All of today's daily challenges are complete — celebrate quietly if it fits.)\n");
            return;
        }
        for (DailyChallengeView c : open) {
            sb.append("- ").append(nullToEmpty(c.title())).append(": ")
                    .append(nullToEmpty(c.description()))
                    .append(" Progress ").append(c.progressCount()).append("/").append(c.requiredCount())
                    .append(". Reward ").append(c.rewardCoins()).append(" coins.\n");
        }
    }

    private static void appendRecentActivity(StringBuilder sb, ProgressSummaryView summary) {
        sb.append("=== Recent things you and your owner did (newest first; vague is fine) ===\n");
        List<ActivityEventView> rows = summary.recentActivity();
        if (rows == null || rows.isEmpty()) {
            sb.append("(No recent activity recorded yet — you're fresh after login or new here.)\n");
            return;
        }
        int n = Math.min(MAX_ACTIVITY_LINES, rows.size());
        for (int i = 0; i < n; i++) {
            ActivityEventView e = rows.get(i);
            sb.append("- ").append(nullToEmpty(e.eventType()));
            if (e.happenedAt() != null) {
                sb.append(" @ ").append(e.happenedAt().toString());
            }
            sb.append('\n');
        }
    }

    private void appendConsumables(StringBuilder sb) {
        sb.append("=== Shop snacks & boosts the owner can buy (catalog; changes over time) ===\n");
        List<ShopItem> items = shopItemRepo.findByActiveTrue().stream()
                .filter(this::playerVisible)
                .filter(i -> "CONSUMABLE".equals(i.type))
                .sorted(Comparator.comparing(i -> nullToEmpty(i.code).toLowerCase(Locale.ROOT)))
                .limit(MAX_CONSUMABLE_LINES)
                .toList();
        if (items.isEmpty()) {
            sb.append("(No consumables listed right now.)\n");
            return;
        }
        for (ShopItem it : items) {
            String line = "- " + nullToEmpty(it.code) + " — " + nullToEmpty(it.name)
                    + " (" + it.priceCoins + " coins)";
            if (it.description != null && !it.description.isBlank()) {
                line += ": " + it.description.trim().replace('\n', ' ');
            }
            sb.append(line).append('\n');
        }
    }

    private void appendMinigames(StringBuilder sb) {
        sb.append("=== Minigames your owner can play (codes matter for challenges) ===\n");
        List<MinigameConfig> games = minigameRepo.findByActiveTrue().stream()
                .sorted(Comparator.comparing(m -> nullToEmpty(m.code).toLowerCase(Locale.ROOT)))
                .limit(MAX_MINIGAME_LINES)
                .toList();
        if (games.isEmpty()) {
            sb.append("(No minigames available right now.)\n");
            return;
        }
        for (MinigameConfig m : games) {
            sb.append("- ").append(nullToEmpty(m.code)).append(" — ").append(nullToEmpty(m.name))
                    .append(" (energy cost ").append(m.energyCost).append(")");
            if (m.description != null && !m.description.isBlank()) {
                sb.append(": ").append(m.description.trim().replace('\n', ' '));
            }
            sb.append('\n');
        }
    }

    private boolean playerVisible(ShopItem item) {
        return item.playerVisible == null || Boolean.TRUE.equals(item.playerVisible);
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    private static String truncateUtf16(String s, int maxChars) {
        if (s == null || maxChars <= 0) {
            return "";
        }
        if (s.length() <= maxChars) {
            return s;
        }
        if (maxChars == 1) {
            return "…";
        }
        return s.substring(0, maxChars - 1) + "…";
    }
}
