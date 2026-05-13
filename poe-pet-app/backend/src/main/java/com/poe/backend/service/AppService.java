package com.poe.backend.service;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import com.poe.backend.controller.ApiException;
import com.poe.backend.model.InventoryItem;
import com.poe.backend.model.MinigameConfig;
import com.poe.backend.model.MinigameSession;
import com.poe.backend.model.PetState;
import com.poe.backend.model.PetVisualAsset;
import com.poe.backend.model.ShopItem;
import com.poe.backend.model.UserAccount;
import com.poe.backend.model.UserToken;
import com.poe.backend.model.Wallet;
import org.springframework.http.HttpStatus;
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

@Service
public class AppService {
    private static final int DEFAULT_SHIFTED_FIBONACCI_CAP = 72;
    private static final int[] DEFAULT_PUZZLE_PREVIEW_SCORES = new int[] { 8, 18, 36, 72 };
    private static final List<String> STARTER_SPECIES = List.of("dog", "cat");
    private static final List<String> SUPPORTED_SPECIES = List.of(
            "dog", "cat", "penguin", "fox", "hamster", "tiger", "lion", "horse", "parrot", "unicorn",
            "midnight_cat", "panda", "goldfish", "lizard");
    /**
     * Central service containing most game logic.
     *
     * Historical note:
     * This class started as a single "app service" for a small project. As features grew (auth, shop,
     * inventory, visuals, multiple minigames), it became a large aggregation point. For readability,
     * the long-term direction is to split this into focused services (AuthService, ShopService, etc.).
     */
    private final UserAccountRepo userAccountRepo;
    private final UserTokenRepo userTokenRepo;
    private final PetStateRepo petStateRepo;
    private final WalletRepo walletRepo;
    private final ShopItemRepo shopItemRepo;
    private final InventoryRepo inventoryRepo;
    private final MinigameRepo minigameRepo;
    private final MinigameSessionRepo minigameSessionRepo;
    private final PetVisualAssetRepo petVisualAssetRepo;
    private final JavaMailSender mailSender;
    private final AiGatewayClient aiGatewayClient;
    private final ActivityHistoryService activityHistoryService;
    private final NotificationPreferenceService notificationPreferenceService;
    private final AiChatContextAssembler aiChatContextAssembler;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final SecureRandom random = new SecureRandom();

    @Value("${app.webBaseUrl}")
    private String webBaseUrl;
    @Value("${app.apiBaseUrl}")
    private String apiBaseUrl;
    @Value("${app.accessTokenHours}")
    private long accessTokenHours;
    @Value("${app.refreshTokenDays}")
    private long refreshTokenDays;
    /** Comma-separated developer emails (see application.yml). */
    @Value("${app.privilegedEmails:}")
    private String privilegedEmailsCsv;
    @Value("${app.aiPersonaAddendum:}")
    private String aiPersonaAddendum;
    @Value("${app.aiChatMaxUserMessageChars:1800}")
    private int aiChatMaxUserMessageChars;
    @Value("${app.aiChatMaxConversationTurns:6}")
    private int aiChatMaxConversationTurns;
    @Value("${app.aiChatMaxAssistantChars:3500}")
    private int aiChatMaxAssistantChars;
    @Value("${app.aiChatMaxContextChars:6000}")
    private int aiChatMaxContextChars;
    @Value("${app.skipEmailVerification:false}")
    private boolean skipEmailVerification;

    public AppService(UserAccountRepo userAccountRepo, UserTokenRepo userTokenRepo, PetStateRepo petStateRepo,
            WalletRepo walletRepo, ShopItemRepo shopItemRepo, InventoryRepo inventoryRepo,
            MinigameRepo minigameRepo, MinigameSessionRepo minigameSessionRepo, PetVisualAssetRepo petVisualAssetRepo,
            JavaMailSender mailSender, AiGatewayClient aiGatewayClient,
            ActivityHistoryService activityHistoryService,
            NotificationPreferenceService notificationPreferenceService,
            AiChatContextAssembler aiChatContextAssembler) {
        this.userAccountRepo = userAccountRepo;
        this.userTokenRepo = userTokenRepo;
        this.petStateRepo = petStateRepo;
        this.walletRepo = walletRepo;
        this.shopItemRepo = shopItemRepo;
        this.inventoryRepo = inventoryRepo;
        this.minigameRepo = minigameRepo;
        this.minigameSessionRepo = minigameSessionRepo;
        this.petVisualAssetRepo = petVisualAssetRepo;
        this.mailSender = mailSender;
        this.aiGatewayClient = aiGatewayClient;
        this.activityHistoryService = activityHistoryService;
        this.notificationPreferenceService = notificationPreferenceService;
        this.aiChatContextAssembler = aiChatContextAssembler;
    }

    /**
     * Developer-only AI chat test. Uses the current user's simulated pet stats to build a fixed context prefix,
     * then calls the standalone Local SLM Gateway.
     *
     * Security: privileged users only (enforced by caller).
     */
    public Map<String, Object> devAiChatTest(String userId, String petName, String message) {
        if (!aiGatewayClient.isEnabled()) {
            return Map.of("ok", false, "error", "AI gateway not configured (app.aiGatewayBaseUrl)");
        }
        PetState pet = getPet(userId);
        String prefix = aiChatContextAssembler.assemble(userId, pet, petName, aiPersonaAddendum, aiChatMaxContextChars);

        String safeMessage = truncateUtf16(message != null ? message : "", aiChatMaxUserMessageChars);
        if (safeMessage.isBlank()) {
            return Map.of("ok", false, "error", "message_empty");
        }

        Map<String, Object> payload = Map.of(
                "userId", userId,
                "contextPrefix", prefix,
                "conversation", List.of(),
                "message", safeMessage);
        try {
            Map<String, Object> res = aiGatewayClient.chat(payload);
            return Map.of("ok", true, "result", enforceAssistantLength(res));
        } catch (Exception e) {
            return Map.of("ok", false, "error", "AI gateway error: " + e.getMessage());
        }
    }

    /**
     * Create a new account and initialize baseline game state.
     * <p>Normal mode: sends verification email; {@link #login} requires {@code emailVerified} until
     * {@link #verifyEmail} succeeds.</p>
     * <p>When {@code app.skipEmailVerification} is true (e.g. {@code APP_SKIP_EMAIL_VERIFICATION=true} without SMTP):
     * sets {@code emailVerified} immediately, skips mail, user can log in at once.</p>
     */
    public Map<String, Object> register(String email, String password) {
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        if (!passwordValid(password)) {
            throw new RuntimeException("Password must be >=5 chars and include at least one digit OR one uppercase.");
        }
        if (userAccountRepo.findByEmail(normalizedEmail).isPresent()) {
            throw new RuntimeException("Email already used.");
        }

        UserAccount user = new UserAccount();
        user.email = normalizedEmail;
        user.passwordHash = passwordEncoder.encode(password);
        user.emailVerified = skipEmailVerification;
        user.createdAt = Instant.now();
        user = userAccountRepo.save(user);

        initializeUserGameData(user.id);
        notificationPreferenceService.ensureDefaults(user.id);
        recordActivity(user.id, "ACCOUNT_REGISTERED", "auth", getPet(user.id), walletRepo.findByUserId(user.id).orElseThrow(),
                Map.of("verificationRequired", !skipEmailVerification));
        if (!skipEmailVerification) {
            sendVerificationMail(user);
            return Map.of("message", "Account created. Verify your email.");
        }
        return Map.of("message", "Account created. You can log in now.");
    }

    /** Mark the user's email as verified, consuming the token. */
    public Map<String, Object> verifyEmail(String tokenValue) {
        UserToken token = userTokenRepo.findByTokenAndTypeAndUsedFalseAndExpiresAtAfter(tokenValue, "VERIFY_EMAIL", Instant.now())
                .orElseThrow(() -> new RuntimeException("Invalid verification token."));
        UserAccount user = userAccountRepo.findById(token.userId).orElseThrow();
        user.emailVerified = true;
        userAccountRepo.save(user);
        token.used = true;
        userTokenRepo.save(token);
        return Map.of("message", "Email verified.");
    }

    /** Login: checks password (+ email verified unless {@code app.skipEmailVerification}). */
    public Map<String, Object> login(String email, String password) {
        UserAccount user = userAccountRepo.findByEmail(email.trim().toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new RuntimeException("Invalid credentials."));
        if (!skipEmailVerification && !user.emailVerified) {
            throw new RuntimeException("Email not verified.");
        }
        if (!passwordEncoder.matches(password, user.passwordHash)) {
            throw new RuntimeException("Invalid credentials.");
        }
        Map<String, Object> tokens = issueAuthTokens(user.id, user.email);
        recordActivity(user.id, "USER_LOGGED_IN", "auth", getPet(user.id), walletRepo.findByUserId(user.id).orElseThrow(),
                Map.of("emailVerified", true));
        return tokens;
    }

    /** Rotate tokens using a valid refresh token. */
    public Map<String, Object> refresh(String refreshToken) {
        UserToken token = userTokenRepo.findByTokenAndTypeAndUsedFalseAndExpiresAtAfter(refreshToken, "REFRESH", Instant.now())
                .orElseThrow(() -> new RuntimeException("Invalid refresh token."));
        return issueAuthTokens(token.userId, userAccountRepo.findById(token.userId).orElseThrow().email);
    }

    /**
     * Request a password reset email.
     *
     * Security: always returns a generic success message to avoid account enumeration.
     */
    public Map<String, Object> forgotPassword(String email) {
        Optional<UserAccount> userOpt = userAccountRepo.findByEmail(email.trim().toLowerCase(Locale.ROOT));
        if (userOpt.isEmpty()) {
            return Map.of("message", "If account exists, reset mail has been sent.");
        }
        UserAccount user = userOpt.get();
        UserToken token = makeToken(user.id, "RESET_PASSWORD", Duration.ofHours(2));
        sendMail(user.email, "Reset password",
                "Open this reset link: " + webBaseUrl + "/reset-password?token=" + token.token);
        return Map.of("message", "If account exists, reset mail has been sent.");
    }

    /**
     * Complete a password reset using a RESET_PASSWORD token.
     *
     * This consumes the token (marks used=true) and updates the user's password hash.
     */
    public Map<String, Object> resetPassword(String tokenValue, String newPassword) {
        if (!passwordValid(newPassword)) {
            throw new RuntimeException("Password policy failed.");
        }
        UserToken token = userTokenRepo.findByTokenAndTypeAndUsedFalseAndExpiresAtAfter(tokenValue, "RESET_PASSWORD", Instant.now())
                .orElseThrow(() -> new RuntimeException("Invalid reset token."));
        UserAccount user = userAccountRepo.findById(token.userId).orElseThrow();
        user.passwordHash = passwordEncoder.encode(newPassword);
        userAccountRepo.save(user);
        token.used = true;
        userTokenRepo.save(token);
        return Map.of("message", "Password reset done.");
    }

    /**
     * Return a minimal "who am I" payload for authenticated clients.
     *
     * Note: this returns account fields only (not pet/wallet).
     */
    public Map<String, Object> me(String userId) {
        UserAccount user = userAccountRepo.findById(userId).orElseThrow();
        return Map.of("id", user.id, "email", user.email, "emailVerified", user.emailVerified);
    }

    /**
     * Fetch the pet state and apply time-based simulation before returning.
     *
     * Side effect: the simulated pet is persisted so the DB reflects the latest state.
     */
    public PetState getPet(String userId) {
        PetState pet = simulate(userId);
        petStateRepo.save(pet);
        return pet;
    }

    /**
     * Build the main dashboard response consumed by the frontend app shell.
     *
     * Includes pet, wallet, developer flag, and reward preview.
     */
    public Map<String, Object> getDashboard(String userId) {
        PetState pet = getPet(userId);
        migratePetNameIfMissing(pet);
        Wallet wallet = walletRepo.findByUserId(userId).orElseThrow();
        Map<String, Object> res = new HashMap<>();
        res.put("pet", pet);
        res.put("wallet", wallet);
        res.put("privileged", isPrivileged(userId));
        res.put("rewardPreview", buildRewardPreview(userId));
        return res;
    }

    /**
     * Lightweight AI integration info for settings and diagnostics (no secrets).
     * When the gateway is enabled, includes {@code /health} payload when reachable.
     */
    public Map<String, Object> getAiChatInfo() {
        Map<String, Object> m = new HashMap<>();
        boolean on = aiGatewayClient.isEnabled();
        m.put("gatewayConfigured", on);
        m.put("maxUserMessageChars", aiChatMaxUserMessageChars);
        m.put("maxConversationTurns", aiChatMaxConversationTurns);
        m.put("maxAssistantChars", aiChatMaxAssistantChars);
        m.put("maxContextChars", aiChatMaxContextChars);
        if (on) {
            try {
                m.put("gatewayHealth", aiGatewayClient.health());
            } catch (Exception e) {
                m.put("gatewayHealth", Map.of("ok", false, "error", e.getMessage()));
            }
        }
        return m;
    }

    /** Ensure older pet documents get a default name. */
    private void migratePetNameIfMissing(PetState pet) {
        if (pet.name == null || pet.name.isBlank()) {
            pet.name = "Pet";
            petStateRepo.save(pet);
        }
    }

    /** Update the current user's pet name. */
    public Map<String, Object> setPetName(String userId, String name) {
        PetState pet = simulate(userId);
        String previousName = pet.name;
        String n = name != null ? name.trim() : "";
        if (n.isEmpty()) {
            n = "Pet";
        }
        if (n.length() > 32) {
            n = n.substring(0, 32);
        }
        pet.name = n;
        petStateRepo.save(pet);
        recordActivity(userId, "PET_RENAMED", "pet", pet, walletOrNull(userId),
                Map.of("previousName", previousName == null ? "" : previousName, "newName", n));
        return Map.of("ok", true, "pet", pet);
    }

    /** Main (non-dev) AI chat: build fixed prefix from pet stats and call the AI gateway. */
    public Map<String, Object> aiChat(String userId, List<Map<String, String>> conversation, String message) {
        PetState pet = getPet(userId);
        String species = pet.speciesCode != null && !pet.speciesCode.isBlank() ? pet.speciesCode : "pet";
        String prefix = aiChatContextAssembler.assemble(userId, pet, null, aiPersonaAddendum, aiChatMaxContextChars);

        String safeMessage = truncateUtf16(message != null ? message : "", aiChatMaxUserMessageChars);
        if (safeMessage.isBlank()) {
            Map<String, Object> fallback = Map.of(
                    "assistantText", "_blinks expectantly_",
                    "fallbackUsed", true,
                    "fallbackReason", "empty_message");
            recordActivity(userId, "AI_CHAT_SENT", "ai", pet, walletOrNull(userId),
                    Map.of("conversationSize", conversation != null ? conversation.size() : 0,
                            "messageLength", 0,
                            "fallbackUsed", true,
                            "fallbackReason", "empty_message"));
            return fallback;
        }

        List<Map<String, String>> safeConv = sanitizeConversation(conversation, aiChatMaxConversationTurns, aiChatMaxUserMessageChars);

        if (!aiGatewayClient.isEnabled()) {
            Map<String, Object> fallback = Map.of(
                    "assistantText", fallbackNoisesForSpecies(species),
                    "fallbackUsed", true,
                    "fallbackReason", "gateway_not_configured");
            recordActivity(userId, "AI_CHAT_SENT", "ai", pet, walletOrNull(userId),
                    Map.of("conversationSize", safeConv.size(),
                            "messageLength", safeMessage.length(),
                            "fallbackUsed", true,
                            "fallbackReason", "gateway_not_configured"));
            return fallback;
        }

        Map<String, Object> payload = Map.of(
                "userId", userId,
                "contextPrefix", prefix,
                "conversation", safeConv,
                "message", safeMessage);
        try {
            Map<String, Object> res = enforceAssistantLength(aiGatewayClient.chat(payload));
            // Ensure callers always get a consistent shape.
            if (res.get("assistantText") instanceof String) {
                recordActivity(userId, "AI_CHAT_SENT", "ai", pet, walletOrNull(userId),
                        Map.of("conversationSize", safeConv.size(),
                                "messageLength", safeMessage.length(),
                                "fallbackUsed", false));
                return res;
            }
            Map<String, Object> normalized = Map.of("assistantText", String.valueOf(res), "fallbackUsed", false);
            recordActivity(userId, "AI_CHAT_SENT", "ai", pet, walletOrNull(userId),
                    Map.of("conversationSize", safeConv.size(),
                            "messageLength", safeMessage.length(),
                            "fallbackUsed", false));
            return normalized;
        } catch (Exception e) {
            Map<String, Object> fallback = Map.of(
                    "assistantText", fallbackNoisesForSpecies(species),
                    "fallbackUsed", true,
                    "fallbackReason", "gateway_error");
            recordActivity(userId, "AI_CHAT_SENT", "ai", pet, walletOrNull(userId),
                    Map.of("conversationSize", safeConv.size(),
                            "messageLength", safeMessage.length(),
                            "fallbackUsed", true,
                            "fallbackReason", "gateway_error"));
            return fallback;
        }
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

    private List<Map<String, String>> sanitizeConversation(List<Map<String, String>> conversation, int maxTurns, int maxCharsPerTurn) {
        if (conversation == null || conversation.isEmpty() || maxTurns <= 0) {
            return List.of();
        }
        int from = Math.max(0, conversation.size() - maxTurns);
        List<Map<String, String>> out = new ArrayList<>();
        for (int i = from; i < conversation.size(); i++) {
            Map<String, String> row = conversation.get(i);
            if (row == null) {
                continue;
            }
            String role = row.get("role");
            String content = row.get("content");
            if (role == null || (!role.equals("user") && !role.equals("assistant"))) {
                continue;
            }
            String safeContent = truncateUtf16(content != null ? content : "", maxCharsPerTurn);
            if (safeContent.isBlank()) {
                continue;
            }
            out.add(Map.of("role", role, "content", safeContent));
        }
        return out;
    }

    private Map<String, Object> enforceAssistantLength(Map<String, Object> res) {
        if (res == null) {
            return Map.of("assistantText", "", "fallbackUsed", false);
        }
        Object at = res.get("assistantText");
        if (!(at instanceof String)) {
            return res;
        }
        String s = (String) at;
        if (s.length() <= aiChatMaxAssistantChars) {
            return res;
        }
        Map<String, Object> copy = new HashMap<>(res);
        copy.put("assistantText", truncateUtf16(s, aiChatMaxAssistantChars));
        return copy;
    }

    private String fallbackNoisesForSpecies(String speciesCode) {
        String s = speciesCode != null ? speciesCode.trim().toLowerCase(Locale.ROOT) : "";
        if (s.equals("cat")) {
            String[] options = new String[] { "*meows softly*", "*purrs*", "*mrrp?*", "*makes a curious cat noise*" };
            return options[(int) (Math.random() * options.length)];
        }
        if (s.equals("penguin")) {
            String[] options = new String[] { "*chirps thoughtfully*", "*waddles closer*", "*makes a tiny penguin peep*", "*flaps flippers softly*" };
            return options[(int) (Math.random() * options.length)];
        }
        if (s.equals("fox")) {
            String[] options = new String[] { "*yips cheekily*", "*swishes a fluffy tail*", "*gives a tiny fox giggle*", "*makes a playful fox sound*" };
            return options[(int) (Math.random() * options.length)];
        }
        if (s.equals("hamster")) {
            String[] options = new String[] { "*squeaks with full cheeks*", "*nibbles happily*", "*makes a tiny hamster peep*", "*wiggles its whiskers*" };
            return options[(int) (Math.random() * options.length)];
        }
        if (s.equals("tiger")) {
            String[] options = new String[] { "*makes a tiny tiger chuff*", "*swishes a striped tail*", "*practices a baby roar*", "*pads closer with cub paws*" };
            return options[(int) (Math.random() * options.length)];
        }
        if (s.equals("lion")) {
            String[] options = new String[] { "*gives a soft cub roar*", "*flicks a tufted tail*", "*puffs up its little mane*", "*nuzzles proudly*" };
            return options[(int) (Math.random() * options.length)];
        }
        if (s.equals("horse")) {
            String[] options = new String[] { "*nickers softly*", "*prances in place*", "*flicks a fluffy tail*", "*makes a tiny foal whinny*" };
            return options[(int) (Math.random() * options.length)];
        }
        if (s.equals("parrot")) {
            String[] options = new String[] { "*chirps brightly*", "*fluffs colorful feathers*", "*squawks hello softly*", "*tilts a curious beak*" };
            return options[(int) (Math.random() * options.length)];
        }
        if (s.equals("unicorn")) {
            String[] options = new String[] { "*whinnies with sparkles*", "*taps golden hooves*", "*shakes a pastel mane*", "*makes a tiny magical neigh*" };
            return options[(int) (Math.random() * options.length)];
        }
        if (s.equals("midnight_cat")) {
            String[] options = new String[] { "*purrs like distant stars*", "*swishes a galaxy tail*", "*mrrps mysteriously*", "*sparkles with midnight fur*" };
            return options[(int) (Math.random() * options.length)];
        }
        if (s.equals("panda")) {
            String[] options = new String[] { "*blinks cluelessly*", "*hugs a bamboo snack*", "*makes a tiny panda huff*", "*rolls around happily*" };
            return options[(int) (Math.random() * options.length)];
        }
        if (s.equals("goldfish")) {
            String[] options = new String[] { "*blubs happily*", "*swishes golden fins*", "*makes tiny aquarium bubbles*", "*circles the bowl cheerfully*" };
            return options[(int) (Math.random() * options.length)];
        }
        if (s.equals("lizard")) {
            String[] options = new String[] { "*blinks with tiny lizard eyes*", "*curls a green tail*", "*makes a soft reptile chirp*", "*scampers closer on tiny toes*" };
            return options[(int) (Math.random() * options.length)];
        }
        // default: dog-ish
        String[] options = new String[] { "*barks cheerfully*", "*woof?*", "*wags tail*", "*makes a curious noise*" };
        return options[(int) (Math.random() * options.length)];
    }

    /** Visible to clients to gate developer tools UI. */
    public boolean isPrivileged(String userId) {
        UserAccount u = userAccountRepo.findById(userId).orElseThrow();
        return u.privileged || privilegedEmailMatches(u.email);
    }

    /**
     * Match the given email against a comma-separated allowlist from config.
     *
     * This allows enabling dev tools without editing DB flags.
     */
    private boolean privilegedEmailMatches(String email) {
        if (email == null || privilegedEmailsCsv == null || privilegedEmailsCsv.isBlank()) {
            return false;
        }
        String n = email.trim().toLowerCase(Locale.ROOT);
        for (String p : privilegedEmailsCsv.split(",")) {
            String t = p.trim().toLowerCase(Locale.ROOT);
            if (!t.isEmpty() && n.equals(t)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Developer sandbox: grant coins to the current user.
     *
     * Security: privileged users only.
     */
    public Map<String, Object> devGrantCoins(String userId, int amount) {
        if (!isPrivileged(userId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Forbidden");
        }
        Wallet w = walletRepo.findByUserId(userId).orElseThrow();
        w.coins += Math.max(0, amount);
        walletRepo.save(w);
        return Map.of("ok", true, "coins", w.coins);
    }

    /**
     * Developer sandbox: set pet stats to 100% for quick UI testing.
     *
     * Security: privileged users only.
     */
    public Map<String, Object> devRefillStats(String userId) {
        if (!isPrivileged(userId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Forbidden");
        }
        PetState pet = simulate(userId);
        pet.hunger = 100;
        pet.happiness = 100;
        pet.energy = 100;
        petStateRepo.save(pet);
        return Map.of("ok", true, "pet", pet);
    }

    /**
     * Developer sandbox: set pet stats as fractions (0..1).
     *
     * Security: privileged users only.
     */
    public Map<String, Object> devSetStats(String userId, double hungerPct, double happinessPct, double energyPct) {
        if (!isPrivileged(userId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Forbidden");
        }
        PetState pet = simulate(userId);
        pet.hunger = clamp(hungerPct * 100.0);
        pet.happiness = clamp(happinessPct * 100.0);
        pet.energy = clamp(energyPct * 100.0);
        petStateRepo.save(pet);
        return Map.of("ok", true, "pet", pet);
    }

    /**
     * Server-side preview of coin payouts with the same rules as finish endpoints (including COIN_MULT consumables).
     */
    public Map<String, Object> buildRewardPreview(String userId) {
        PetState pet = simulate(userId);
        double mult = coinMultiplierForPet(pet, Instant.now());
        MinigameSession hl = minigameSessionRepo.findByUserIdAndGameCodeAndActiveTrue(userId, "higher_lower").orElse(null);
        MinigameConfig hlGame = minigameRepo.findByCodeAndActiveTrue("higher_lower").orElse(null);
        int streak = hl != null ? hl.streak : 0;
        int hlMax = hlGame != null ? maxRewardCap(hlGame) : DEFAULT_SHIFTED_FIBONACCI_CAP;
        int hlBase = GameMath.shiftedFibonacciReward(streak, hlMax);
        int hlPayout = applyCoinMultiplierToBase(hlBase, mult);

        MinigameConfig pz = minigameRepo.findByCodeAndActiveTrue("puzzle_swap").orElse(null);
        Map<String, Integer> puzzleAtScore = new HashMap<>();
        if (pz != null) {
            for (int s : previewScoresOrDefault(pz, DEFAULT_PUZZLE_PREVIEW_SCORES)) {
                int raw = calculateSimpleRewardBase(pz, s);
                puzzleAtScore.put("score_" + s, applyCoinMultiplierToBase(raw, mult));
            }
        }

        Map<String, Integer> c4 = new HashMap<>();
        MinigameConfig c4g = minigameRepo.findByCodeAndActiveTrue("connect4_ai").orElse(null);
        if (c4g != null) {
            c4.put("win", applyCoinMultiplierToBase(calculateConnect4BaseReward(c4g, 2), mult));
            c4.put("draw", applyCoinMultiplierToBase(calculateConnect4BaseReward(c4g, 1), mult));
            c4.put("loss", applyCoinMultiplierToBase(calculateConnect4BaseReward(c4g, 0), mult));
        }

        Map<String, Integer> ms = new HashMap<>();
        MinigameConfig msg = minigameRepo.findByCodeAndActiveTrue("minesweep_ai").orElse(null);
        if (msg != null) {
            ms.put("win", applyCoinMultiplierToBase(calculateConnect4BaseReward(msg, 2), mult));
            ms.put("loss", applyCoinMultiplierToBase(calculateConnect4BaseReward(msg, 0), mult));
        }
        Map<String, Integer> chk = new HashMap<>();
        MinigameConfig chg = minigameRepo.findByCodeAndActiveTrue("checkers_ai").orElse(null);
        if (chg != null) {
            chk.put("win", applyCoinMultiplierToBase(calculateConnect4BaseReward(chg, 2), mult));
            chk.put("draw", applyCoinMultiplierToBase(calculateConnect4BaseReward(chg, 1), mult));
            chk.put("loss", applyCoinMultiplierToBase(calculateConnect4BaseReward(chg, 0), mult));
        }

        Map<String, Integer> energyCosts = new HashMap<>();
        for (MinigameConfig m : minigameRepo.findByActiveTrue()) {
            energyCosts.put(m.code, m.energyCost);
        }

        Map<String, Object> out = new HashMap<>();
        out.put("coinMultiplier", round2(mult));
        out.put("energyCosts", energyCosts);
        out.put("higherLower", Map.of(
                "hasActiveSession", hl != null,
                "streak", streak,
                "coinsIfFinishNow", hlPayout,
                "fibonacciCap", hlMax));
        out.put("puzzle_swap", Map.of("coinsBySampleScore", puzzleAtScore));
        out.put("connect4_ai", Map.of("coinsByOutcome", c4));
        out.put("minesweep_ai", Map.of("coinsByOutcome", ms));
        out.put("checkers_ai", Map.of("coinsByOutcome", chk));
        return out;
    }

    /**
     * List active shop items visible to players.
     *
     * This is the "shop catalog" endpoint consumed by the frontend.
     */
    public List<ShopItem> shopItems() {
        return shopItemRepo.findByActiveTrue().stream()
                .filter(AppService::isPlayerVisibleInShop)
                .toList();
    }

    /** Catalog rows: {@code playerVisible == false} hides from shop; null or true shows (backward compatible). */
    private static boolean isPlayerVisibleInShop(ShopItem item) {
        return item.playerVisible == null || Boolean.TRUE.equals(item.playerVisible);
    }

    /**
     * Purchase an item from the shop.
     *
     * Supported types:
     * - CONSUMABLE: increases inventory quantity
     * - COSMETIC: grants a visual asset code into {@code PetState.ownedVisualAssetCodes}
     * - SPECIES: grants a selectable pet species into {@code PetState.ownedSpeciesCodes}
     */
    public Map<String, Object> purchase(String userId, String itemCode) {
        ShopItem item = shopItemRepo.findByCodeAndActiveTrue(itemCode)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Item not found"));
        if (!isPlayerVisibleInShop(item)) {
            throw new RuntimeException("Item not available");
        }
        Wallet wallet = walletRepo.findByUserId(userId).orElseThrow();
        if (wallet.coins < item.priceCoins) {
            throw new RuntimeException("Not enough coins");
        }

        if ("CONSUMABLE".equals(item.type)) {
            wallet.coins -= item.priceCoins;
            walletRepo.save(wallet);
            InventoryItem inv = inventoryRepo.findByUserIdAndItemCode(userId, item.code).orElseGet(() -> {
                InventoryItem i = new InventoryItem();
                i.userId = userId;
                i.itemCode = item.code;
                i.quantity = 0;
                return i;
            });
            inv.quantity += 1;
            inventoryRepo.save(inv);
            recordActivity(userId, "SHOP_PURCHASED", "shop", getPet(userId), wallet,
                    Map.of("itemCode", item.code, "itemType", item.type, "priceCoins", item.priceCoins));
            return Map.of("ok", true);
        }

        if ("COSMETIC".equals(item.type)) {
            String visualCode = extractGrantVisualAssetCode(item);
            if (visualCode == null || visualCode.isBlank()) {
                throw new RuntimeException("Cosmetic item missing GRANT_VISUAL effect");
            }
            petVisualAssetRepo.findByCodeAndActiveTrue(visualCode)
                    .orElseThrow(() -> new RuntimeException("Unknown visual asset: " + visualCode));
            PetState pet = petStateRepo.findByUserId(userId).orElseThrow();
            migratePetVisualFields(pet);
            if (pet.ownedVisualAssetCodes.contains(visualCode)) {
                throw new RuntimeException("Already owned");
            }
            wallet.coins -= item.priceCoins;
            walletRepo.save(wallet);
            pet.ownedVisualAssetCodes.add(visualCode);
            petStateRepo.save(pet);
            recordActivity(userId, "SHOP_PURCHASED", "shop", pet, wallet,
                    Map.of("itemCode", item.code, "itemType", item.type, "priceCoins", item.priceCoins,
                            "grantedVisualAssetCode", visualCode));
            return Map.of("ok", true, "grantedVisualAssetCode", visualCode);
        }

        if ("SPECIES".equals(item.type)) {
            String grantedSpecies = normalizeSpeciesCode(extractGrantSpeciesCode(item));
            if (!SUPPORTED_SPECIES.contains(grantedSpecies) || STARTER_SPECIES.contains(grantedSpecies)) {
                throw new RuntimeException("Species item missing valid GRANT_SPECIES effect");
            }
            PetState pet = petStateRepo.findByUserId(userId).orElseThrow();
            migratePetVisualFields(pet);
            if (pet.ownedSpeciesCodes.contains(grantedSpecies)) {
                throw new RuntimeException("Already owned");
            }
            wallet.coins -= item.priceCoins;
            walletRepo.save(wallet);
            pet.ownedSpeciesCodes.add(grantedSpecies);
            petStateRepo.save(pet);
            recordActivity(userId, "SHOP_PURCHASED", "shop", pet, wallet,
                    Map.of("itemCode", item.code, "itemType", item.type, "priceCoins", item.priceCoins,
                            "grantedSpeciesCode", grantedSpecies));
            return Map.of("ok", true, "grantedSpeciesCode", grantedSpecies);
        }

        throw new RuntimeException("Unsupported shop item type: " + item.type);
    }

    /**
     * Return all inventory rows for the user.
     *
     * Each row is (itemCode, quantity). Quantity 0 rows are deleted (see {@link #consumeItem}).
     */
    public List<InventoryItem> inventory(String userId) {
        return inventoryRepo.findByUserId(userId);
    }

    /**
     * Return the global visual catalog (all active assets).
     *
     * Ownership is enforced on equip/apply routes; the catalog itself is not user-specific.
     */
    public List<PetVisualAsset> petVisualCatalog() {
        return petVisualAssetRepo.findByActiveTrue().stream()
                .sorted((a, b) -> {
                    int t = String.valueOf(a.assetType).compareTo(String.valueOf(b.assetType));
                    if (t != 0) {
                        return t;
                    }
                    return String.valueOf(a.code).compareTo(String.valueOf(b.code));
                })
                .toList();
    }

    /**
     * Set the base pet species. Dog/cat are starter choices; every other species must be unlocked in the shop first.
     *
     * Species controls which mood assets can be equipped.
     */
    public Map<String, Object> setSpecies(String userId, String speciesCode) {
        String normalized = normalizeSpeciesCode(speciesCode);
        if (!SUPPORTED_SPECIES.contains(normalized)) {
            throw new RuntimeException("Unsupported species");
        }
        PetState pet = petStateRepo.findByUserId(userId).orElseThrow();
        migratePetVisualFields(pet);
        if (!pet.ownedSpeciesCodes.contains(normalized)) {
            throw new RuntimeException("Species not owned");
        }
        String previousSpecies = pet.speciesCode;
        pet.speciesCode = normalized;
        petStateRepo.save(pet);
        recordActivity(userId, "PET_SPECIES_SET", "pet", pet, walletOrNull(userId),
                Map.of("previousSpeciesCode", previousSpecies == null ? "" : previousSpecies, "speciesCode", normalized));
        return Map.of("ok", true, "speciesCode", normalized);
    }

    /**
     * Set mood slot overrides (mood -> assetCode).
     *
     * Uses "none"/blank to clear an override and fall back to starter defaults.
     */
    public Map<String, Object> setMoodAssets(String userId, Map<String, String> moodAssetCodes) {
        if (moodAssetCodes == null) {
            throw new RuntimeException("moodAssetCodes is required");
        }
        PetState pet = petStateRepo.findByUserId(userId).orElseThrow();
        String species = (pet.speciesCode == null || pet.speciesCode.isBlank()) ? "dog" : pet.speciesCode;
        Map<String, String> next = new HashMap<>();
        for (String mood : List.of("happy", "sad", "hungry", "tired", "playing_dead")) {
            String code = moodAssetCodes.get(mood);
            if (code == null || code.isBlank() || "none".equalsIgnoreCase(code)) {
                continue;
            }
            PetVisualAsset asset = petVisualAssetRepo.findByCodeAndActiveTrue(code)
                    .orElseThrow(() -> new RuntimeException("Unknown asset code: " + code));
            if (!"PET_MOOD".equals(asset.assetType)) {
                throw new RuntimeException("Asset is not PET_MOOD: " + code);
            }
            if (!mood.equalsIgnoreCase(asset.moodCode)) {
                throw new RuntimeException("Asset mood mismatch for " + mood);
            }
            if (!species.equalsIgnoreCase(asset.speciesCode)) {
                throw new RuntimeException("Asset species mismatch for current species");
            }
            if (!asset.starter && (pet.ownedVisualAssetCodes == null || !pet.ownedVisualAssetCodes.contains(code))) {
                throw new RuntimeException("You do not own this mood visual: " + code);
            }
            next.put(mood, code);
        }
        pet.moodAssetCodes = next;
        petStateRepo.save(pet);
        return Map.of("ok", true, "moodAssetCodes", next);
    }

    /**
     * Equip background + foreground visual layers.
     *
     * Passing "none" or blank clears a layer.
     */
    public Map<String, Object> equipVisualLayers(String userId, String backgroundAssetCode, String foregroundAssetCode) {
        PetState pet = petStateRepo.findByUserId(userId).orElseThrow();
        migratePetVisualFields(pet);
        if (backgroundAssetCode != null && !backgroundAssetCode.isBlank() && !"none".equalsIgnoreCase(backgroundAssetCode)) {
            PetVisualAsset bg = petVisualAssetRepo.findByCodeAndActiveTrue(backgroundAssetCode)
                    .orElseThrow(() -> new RuntimeException("Unknown background asset"));
            if (!"BACKGROUND".equals(bg.assetType)) {
                throw new RuntimeException("Not a background asset");
            }
            if (!bg.starter && !pet.ownedVisualAssetCodes.contains(backgroundAssetCode)) {
                throw new RuntimeException("You do not own this background");
            }
            pet.equippedBackgroundAssetCode = backgroundAssetCode;
        } else {
            pet.equippedBackgroundAssetCode = null;
        }
        if (foregroundAssetCode != null && !foregroundAssetCode.isBlank() && !"none".equalsIgnoreCase(foregroundAssetCode)) {
            PetVisualAsset fg = petVisualAssetRepo.findByCodeAndActiveTrue(foregroundAssetCode)
                    .orElseThrow(() -> new RuntimeException("Unknown foreground asset"));
            if (!"FOREGROUND".equals(fg.assetType)) {
                throw new RuntimeException("Not a foreground asset");
            }
            if (!fg.starter && !pet.ownedVisualAssetCodes.contains(foregroundAssetCode)) {
                throw new RuntimeException("You do not own this foreground");
            }
            pet.equippedForegroundAssetCode = foregroundAssetCode;
        } else {
            pet.equippedForegroundAssetCode = null;
        }
        petStateRepo.save(pet);
        return Map.of(
                "ok", true,
                "equippedBackgroundAssetCode", pet.equippedBackgroundAssetCode == null ? "" : pet.equippedBackgroundAssetCode,
                "equippedForegroundAssetCode", pet.equippedForegroundAssetCode == null ? "" : pet.equippedForegroundAssetCode);
    }

    /**
     * Use a consumable item from inventory and apply its effects.
     *
     * This endpoint supports a two-step "confirm overwrite" flow for timed effects:
     * - first call returns { needsConfirmation: true } if an active effect would be overwritten
     * - second call repeats with confirmOverwrite=true to overwrite intentionally
     */
    public Map<String, Object> useConsumable(String userId, String itemCode, boolean confirmOverwrite) {
        InventoryItem inv = inventoryRepo.findByUserIdAndItemCode(userId, itemCode)
                .orElseThrow(() -> new RuntimeException("Item not owned"));
        if (inv.quantity <= 0) {
            throw new RuntimeException("No quantity");
        }
        if (minigameSessionRepo.findByUserIdAndGameCodeAndActiveTrue(userId, "higher_lower").isPresent()) {
            throw new RuntimeException("Cannot use consumables during active minigame");
        }

        ShopItem item = shopItemRepo.findByCodeAndActiveTrue(itemCode).orElseThrow();
        PetState pet = simulate(userId);

        if (item.effectKey != null) {
            PetState.ActiveEffect existing = pet.activeEffects.stream()
                    .filter(a -> item.effectKey.equals(a.effectKey) && a.expiresAt.isAfter(Instant.now()))
                    .findFirst()
                    .orElse(null);
            if (existing != null) {
                double newValue = extractEnergyMultiplier(item.effects);
                if (Math.abs(existing.value - newValue) < 0.0001) {
                    existing.expiresAt = Instant.now().plus(Duration.ofHours(extractDurationHours(item.effects)));
                    petStateRepo.save(pet);
                    consumeItem(inv);
                    recordActivity(userId, "CONSUMABLE_USED", "inventory", pet, walletOrNull(userId),
                            Map.of("itemCode", item.code, "effectKey", item.effectKey == null ? "" : item.effectKey,
                                    "result", "timer_reset"));
                    return Map.of("ok", true, "message", "Effect timer reset.");
                }
                if (!confirmOverwrite) {
                    return Map.of("needsConfirmation", true, "message", "Active effect will be overwritten.");
                }
                if (existing.value > newValue && existing.expiresAt.isAfter(Instant.now().plus(Duration.ofHours(extractDurationHours(item.effects))))) {
                    throw new RuntimeException("Weaker/shorter effect blocked without explicit confirmation.");
                }
                existing.value = newValue;
                existing.expiresAt = Instant.now().plus(Duration.ofHours(extractDurationHours(item.effects)));
                applyNonTimedEffects(pet, item.effects);
                petStateRepo.save(pet);
                consumeItem(inv);
                recordActivity(userId, "CONSUMABLE_USED", "inventory", pet, walletOrNull(userId),
                        Map.of("itemCode", item.code, "effectKey", item.effectKey == null ? "" : item.effectKey,
                                "result", "effect_overwritten"));
                return Map.of("ok", true, "message", "Effect overwritten.");
            }
        }

        applyEffects(pet, item.effects, item.effectKey);
        petStateRepo.save(pet);
        consumeItem(inv);
        recordActivity(userId, "CONSUMABLE_USED", "inventory", pet, walletOrNull(userId),
                Map.of("itemCode", item.code, "effectKey", item.effectKey == null ? "" : item.effectKey,
                        "result", "applied"));
        return Map.of("ok", true);
    }


    /**
     * Start (or resume) a Higher/Lower session.
     *
     * Higher/Lower is session-based: state is persisted in {@code minigame_sessions} so it can resume.
     * Starting a new session charges energy once; resuming does not.
     */
    public Map<String, Object> startHigherLower(String userId) {
        MinigameConfig game = minigameRepo.findByCodeAndActiveTrue("higher_lower").orElseThrow();
        MinigameSession existing = minigameSessionRepo.findByUserIdAndGameCodeAndActiveTrue(userId, "higher_lower").orElse(null);
        if (existing != null) {
            return Map.of("currentNumber", existing.currentNumber, "streak", existing.streak, "resumed", true);
        }
        PetState pet = simulate(userId);
        if (pet.energy < game.energyCost) {
            throw new RuntimeException("Not enough energy");
        }
        pet.energy = clamp(pet.energy - game.energyCost);
        petStateRepo.save(pet);

        MinigameSession session = new MinigameSession();
        session.userId = userId;
        session.gameCode = "higher_lower";
        session.currentNumber = random.nextInt(100) + 1;
        session.streak = 0;
        session.active = true;
        session.startedAt = Instant.now();
        minigameSessionRepo.save(session);
        return Map.of("currentNumber", session.currentNumber, "streak", session.streak);
    }

    /**
     * Submit a Higher/Lower guess (HIGHER or LOWER).
     *
     * If correct, the streak continues and the session stays active.
     * If incorrect, the session is finished and payout/happiness changes are applied.
     */
    public Map<String, Object> guessHigherLower(String userId, String guess) {
        MinigameSession session = minigameSessionRepo.findByUserIdAndGameCodeAndActiveTrue(userId, "higher_lower")
                .orElseThrow(() -> new ApiException(HttpStatus.CONFLICT, "No active session"));
        int next = random.nextInt(100) + 1;
        boolean correct = "HIGHER".equalsIgnoreCase(guess) ? next > session.currentNumber : next < session.currentNumber;
        int previous = session.currentNumber;
        session.currentNumber = next;
        if (correct) {
            session.streak += 1;
            minigameSessionRepo.save(session);
            return Map.of("correct", true, "previous", previous, "next", next, "streak", session.streak, "gameOver", false);
        }
        return finishSession(userId, session, previous, next);
    }

    /**
     * Quit Higher/Lower and claim coins for the current streak.
     *
     * This differs from simple minigames where abandon provides an energy refund.
     */
    public Map<String, Object> quitHigherLower(String userId) {
        Optional<MinigameSession> sessionOpt = minigameSessionRepo.findByUserIdAndGameCodeAndActiveTrue(userId, "higher_lower");
        if (sessionOpt.isEmpty()) {
            return Map.of("ok", true, "coinsReward", 0, "happinessDeltaPercent", 0, "noActiveSession", true);
        }
        MinigameSession session = sessionOpt.get();
        return finishSession(userId, session, session.currentNumber, session.currentNumber);
    }

    /**
     * List all active minigame configurations from the DB catalog.
     *
     * The frontend uses this list to render the minigames hub and show descriptions/energy costs.
     */
    public List<MinigameConfig> minigames() {
        return minigameRepo.findByActiveTrue();
    }

    /**
     * Start a "simple" (non-session) minigame by charging its entry energy cost.
     *
     * The gameplay itself happens client-side; the backend only records start/finish/abandon effects.
     */
    public Map<String, Object> startSimpleMinigame(String userId, String gameCode) {
        MinigameConfig game = minigameRepo.findByCodeAndActiveTrue(gameCode)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Minigame not found"));
        if ("higher_lower".equals(gameCode)) {
            throw new RuntimeException("Use higher/lower dedicated endpoint");
        }
        PetState pet = simulate(userId);
        if (pet.energy < game.energyCost) {
            throw new RuntimeException("Not enough energy");
        }
        pet.energy = clamp(pet.energy - game.energyCost);
        petStateRepo.save(pet);
        return Map.of("ok", true, "energyCost", game.energyCost);
    }

    /**
     * Finish a simple minigame.
     *
     * Applies:
     * - coins reward into wallet (with active coin multipliers)
     * - happiness delta into pet
     */
    public Map<String, Object> finishSimpleMinigame(String userId, String gameCode, int score, String connectDifficulty, Integer connectHumanMoves) {
        MinigameConfig game = minigameRepo.findByCodeAndActiveTrue(gameCode)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Minigame not found"));
        PetState pet = simulate(userId);
        double mult = coinMultiplierForPet(pet, Instant.now());
        int baseReward = calculateSimpleRewardBase(game, score);
        int reward = applyCoinMultiplierToBase(baseReward, mult);
        int happinessDeltaPercent = happinessDeltaForSimpleMinigame(gameCode, score, connectDifficulty, connectHumanMoves);

        Wallet wallet = walletRepo.findByUserId(userId).orElseThrow();
        wallet.coins += reward;
        walletRepo.save(wallet);

        pet.happiness = clamp(pet.happiness + (pet.happiness * happinessDeltaPercent / 100.0));
        petStateRepo.save(pet);
        recordActivity(userId, "MINIGAME_FINISHED", "minigame", pet, wallet,
                Map.of("gameCode", gameCode, "score", score, "coinsReward", reward,
                        "coinsBaseBeforeMultiplier", baseReward, "coinMultiplierApplied", round2(mult),
                        "happinessDeltaPercent", happinessDeltaPercent,
                        "connectDifficulty", connectDifficulty == null ? "" : connectDifficulty,
                        "connectHumanMoves", connectHumanMoves == null ? 0 : connectHumanMoves));
        return Map.of("ok", true, "coinsReward", reward, "happinessDeltaPercent", happinessDeltaPercent, "coinsBaseBeforeMultiplier", baseReward,
                "coinMultiplierApplied", round2(mult));
    }

    /**
     * Leave a simple minigame mid-run: refund half the entry energy cost (integer: {@code cost - cost/2}),
     * no coins and no happiness change. Higher/Lower must use {@link #quitHigherLower}.
     */
    public Map<String, Object> abandonSimpleMinigame(String userId, String gameCode) {
        MinigameConfig game = minigameRepo.findByCodeAndActiveTrue(gameCode)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Minigame not found"));
        if ("higher_lower".equals(gameCode)) {
            throw new RuntimeException("Use higher-lower quit endpoint");
        }
        PetState pet = simulate(userId);
        int cost = game.energyCost;
        int refund = cost - cost / 2;
        pet.energy = clamp(pet.energy + refund);
        petStateRepo.save(pet);
        return Map.of("ok", true, "energyRefund", refund, "energyCost", cost);
    }

    /**
     * Connect 4: win uses raised base + tier×moves×3 (capped at 100%). Loss on easy uses global score thresholds;
     * loss on medium/hard uses effort-based delta (base -5%, +3/+6% per human move vs easy), clamped.
     */
    private int happinessDeltaForSimpleMinigame(String gameCode, int score, String connectDifficulty, Integer connectHumanMoves) {
        if ("connect4_ai".equals(gameCode) && score == 2) {
            int moves = connectHumanMoves == null ? 0 : Math.max(0, connectHumanMoves);
            return GameMath.connect4WinHappinessDeltaPercent(connectDifficulty, moves);
        }
        if (!"connect4_ai".equals(gameCode) || score != 0) {
            return GameMath.happinessDeltaForScore(Math.max(0, score));
        }
        String d = connectDifficulty == null ? "easy" : connectDifficulty.trim().toLowerCase(Locale.ROOT);
        if ("easy".equals(d)) {
            return GameMath.happinessDeltaForScore(0);
        }
        int moves = connectHumanMoves == null ? 0 : Math.max(0, connectHumanMoves);
        int weight = "hard".equals(d) ? 2 : 1;
        int delta = -5 + weight * moves * 3;
        return Math.max(-10, Math.min(62, delta));
    }

    /**
     * Shared Higher/Lower finishing logic (used both on quit and on loss).
     *
     * Computes base reward, applies coin multipliers, updates wallet and pet happiness, then deactivates the session.
     */
    private Map<String, Object> finishSession(String userId, MinigameSession session, int previous, int next) {
        MinigameConfig hlGame = minigameRepo.findByCodeAndActiveTrue("higher_lower").orElseThrow();
        int fibMax = maxRewardCap(hlGame);
        int streak = session.streak;
        int baseReward = GameMath.shiftedFibonacciReward(streak, fibMax);

        PetState pet = simulate(userId);
        double mult = coinMultiplierForPet(pet, Instant.now());
        int reward = applyCoinMultiplierToBase(baseReward, mult);

        int happinessDeltaPercent = GameMath.happinessDeltaForScore(streak);
        Wallet wallet = walletRepo.findByUserId(userId).orElseThrow();
        wallet.coins += reward;
        walletRepo.save(wallet);

        pet.happiness = clamp(pet.happiness + (pet.happiness * happinessDeltaPercent / 100.0));
        petStateRepo.save(pet);
        session.active = false;
        minigameSessionRepo.save(session);
        recordActivity(userId, "MINIGAME_FINISHED", "higher_lower", pet, wallet,
                Map.of("gameCode", "higher_lower", "streak", streak, "previous", previous, "next", next,
                        "coinsReward", reward, "coinsBaseBeforeMultiplier", baseReward,
                        "coinMultiplierApplied", round2(mult), "happinessDeltaPercent", happinessDeltaPercent));
        return Map.of("correct", false, "previous", previous, "next", next, "streak", streak, "gameOver", true,
                "coinsReward", reward, "happinessDeltaPercent", happinessDeltaPercent,
                "coinsBaseBeforeMultiplier", baseReward, "coinMultiplierApplied", round2(mult));
    }

    /**
     * Compute the active coin multiplier from timed effects.
     *
     * Multiple multipliers stack multiplicatively.
     */
    private double coinMultiplierForPet(PetState pet, Instant now) {
        double m = 1.0;
        for (PetState.ActiveEffect e : pet.activeEffects) {
            if (e.expiresAt.isBefore(now)) {
                continue;
            }
            if ("COIN_MULT".equals(e.bonusKind)) {
                if (e.value > 0) {
                    m *= e.value;
                }
            }
        }
        return m;
    }

    /**
     * Multiply base coins by the multiplier and return integer coins.
     *
     * Uses rounding; guards against negative base and invalid multipliers.
     */
    private int applyCoinMultiplierToBase(int baseCoins, double mult) {
        if (mult <= 0) {
            mult = 1.0;
        }
        return (int) Math.round(Math.max(0, baseCoins) * mult);
    }

    /** Utility: round to 2 decimal places for client-facing debug/payout fields. */
    private double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    /**
     * Read the maximum cap for shifted-fibonacci style rewards from the minigame's rewardStrategy.
     *
     * This prevents unbounded rewards for long sessions.
     */
    private int maxRewardCap(MinigameConfig game) {
        if (game == null || game.rewardStrategy == null) {
            return DEFAULT_SHIFTED_FIBONACCI_CAP;
        }
        return ((Number) game.rewardStrategy.getOrDefault("maxReward", DEFAULT_SHIFTED_FIBONACCI_CAP)).intValue();
    }

    /**
     * Reward preview uses a handful of sample scores so the UI can show "if you score ~X you'll earn ~Y coins".
     *
     * We support reading those samples from the DB (rewardStrategy.previewScores), but keep a small default list
     * for older rows.
     */
    private int[] previewScoresOrDefault(MinigameConfig game, int[] fallback) {
        if (game == null || game.rewardStrategy == null) {
            return fallback;
        }
        Object raw = game.rewardStrategy.get("previewScores");
        if (!(raw instanceof List<?> list) || list.isEmpty()) {
            return fallback;
        }
        List<Integer> parsed = new ArrayList<>();
        for (Object o : list) {
            if (o instanceof Number n) {
                parsed.add(n.intValue());
            } else if (o != null) {
                try {
                    parsed.add(Integer.parseInt(String.valueOf(o)));
                } catch (NumberFormatException ignored) {
                    // ignore invalid values
                }
            }
        }
        if (parsed.isEmpty()) {
            return fallback;
        }
        int[] out = new int[parsed.size()];
        for (int i = 0; i < parsed.size(); i++) {
            out[i] = parsed.get(i);
        }
        return out;
    }

    /**
     * Base coins from DB rules only (coin consumable multiplier applied by callers).
     */
    private int calculateSimpleRewardBase(MinigameConfig game, int score) {
        if (game.rewardStrategy == null) {
            return Math.max(0, score);
        }
        String type = String.valueOf(game.rewardStrategy.getOrDefault("type", "SCORE_LINEAR"));
        if ("SHIFTED_FIBONACCI".equals(type)) {
            return GameMath.shiftedFibonacciReward(Math.max(0, score), maxRewardCap(game));
        }
        if ("CONNECT4_OUTCOME".equals(type)) {
            return calculateConnect4BaseReward(game, score);
        }
        int coinsPerPoint = ((Number) game.rewardStrategy.getOrDefault("coinsPerPoint", 2)).intValue();
        int maxReward = ((Number) game.rewardStrategy.getOrDefault("maxReward", 120)).intValue();
        return Math.min(maxReward, Math.max(0, score) * coinsPerPoint);
    }

    /**
     * Compute base reward coins for outcome-based minigames (win/draw/loss).
     *
     * outcomeScore: 2=win, 1=draw, 0=loss.
     */
    @SuppressWarnings("unchecked")
    private int calculateConnect4BaseReward(MinigameConfig game, int outcomeScore) {
        Map<String, Object> rewards = (Map<String, Object>) game.rewardStrategy.getOrDefault("rewards", Map.of());
        if (outcomeScore >= 2) {
            return ((Number) rewards.getOrDefault("win", 8)).intValue();
        }
        if (outcomeScore == 1) {
            return ((Number) rewards.getOrDefault("draw", 3)).intValue();
        }
        return ((Number) rewards.getOrDefault("loss", 1)).intValue();
    }

    /**
     * Consume one unit of an inventory item.
     *
     * Deletes the inventory record when quantity reaches 0.
     */
    private void consumeItem(InventoryItem inv) {
        inv.quantity -= 1;
        if (inv.quantity <= 0) {
            inventoryRepo.delete(inv);
            return;
        }
        inventoryRepo.save(inv);
    }

    /**
     * Apply all item effects to a pet.
     *
     * - non-timed effects apply immediately to stats
     * - timed effects become {@link PetState.ActiveEffect} rows with an expiry
     */
    private void applyEffects(PetState pet, List<Map<String, Object>> effects, String effectKey) {
        applyNonTimedEffects(pet, effects);
        if (effectKey == null) {
            return;
        }
        Instant start = Instant.now();
        double energyBonus = extractEnergyMultiplier(effects);
        if (energyBonus > 0) {
            int hours = extractDurationHours(effects);
            PetState.ActiveEffect ae = new PetState.ActiveEffect();
            ae.effectKey = effectKey;
            ae.bonusKind = "ENERGY_REGEN";
            ae.value = energyBonus;
            ae.expiresAt = start.plus(Duration.ofHours(hours));
            pet.activeEffects.add(ae);
        }
        double coinMult = extractCoinMultiplierFactor(effects);
        if (coinMult > 1.0) {
            int coinHours = extractDurationHoursCoin(effects);
            PetState.ActiveEffect ce = new PetState.ActiveEffect();
            ce.effectKey = effectKey;
            ce.bonusKind = "COIN_MULT";
            ce.value = coinMult;
            ce.expiresAt = start.plus(Duration.ofHours(Math.max(1, coinHours)));
            pet.activeEffects.add(ce);
        }
    }

    /**
     * Apply "instant" effects that directly modify pet stats (hunger/happiness/energy).
     */
    private void applyNonTimedEffects(PetState pet, List<Map<String, Object>> effects) {
        for (Map<String, Object> effect : effects) {
            String kind = String.valueOf(effect.get("kind"));
            double value = effect.get("value") == null ? 0.0 : ((Number) effect.get("value")).doubleValue();
            switch (kind) {
                case "HUNGER_ADD" -> pet.hunger = clamp(pet.hunger + value);
                case "ENERGY_PERCENT_ADD" -> pet.energy = clamp(pet.energy + value * 100.0);
                case "SET_HUNGER_PERCENT" -> pet.hunger = clamp(value * 100.0);
                case "SET_HAPPINESS_PERCENT" -> pet.happiness = clamp(value * 100.0);
                case "HAPPINESS_ADD" -> pet.happiness = clamp(pet.happiness + value);
                case "SET_ENERGY_PERCENT" -> pet.energy = clamp(value * 100.0);
                default -> {
                }
            }
        }
    }

    /**
     * Extract the ENERGY_REGEN_MULTIPLIER value from an effect list.
     *
     * Returns 0 when no multiplier effect exists.
     */
    private double extractEnergyMultiplier(List<Map<String, Object>> effects) {
        for (Map<String, Object> effect : effects) {
            if ("ENERGY_REGEN_MULTIPLIER".equals(effect.get("kind"))) {
                return ((Number) effect.get("value")).doubleValue();
            }
        }
        return 0;
    }

    /** Multiplicative factor for coins (e.g. 1.2 = +20%). */
    private double extractCoinMultiplierFactor(List<Map<String, Object>> effects) {
        for (Map<String, Object> effect : effects) {
            if ("COIN_MULTIPLIER".equals(String.valueOf(effect.get("kind")))) {
                double v = ((Number) effect.get("value")).doubleValue();
                return v > 0 ? v : 1.0;
            }
        }
        return 1.0;
    }

    /**
     * Determine a duration (hours) for timed effects.
     *
     * Preference:
     * - durationHours on the ENERGY_REGEN_MULTIPLIER effect
     * - any durationHours field in the effect list
     * - default 1 hour
     */
    private int extractDurationHours(List<Map<String, Object>> effects) {
        for (Map<String, Object> effect : effects) {
            if ("ENERGY_REGEN_MULTIPLIER".equals(effect.get("kind")) && effect.get("durationHours") != null) {
                return ((Number) effect.get("durationHours")).intValue();
            }
        }
        for (Map<String, Object> effect : effects) {
            if (effect.get("durationHours") != null) {
                return ((Number) effect.get("durationHours")).intValue();
            }
        }
        return 1;
    }

    /**
     * Determine duration for coin multipliers (COIN_MULTIPLIER), falling back to {@link #extractDurationHours}.
     */
    private int extractDurationHoursCoin(List<Map<String, Object>> effects) {
        for (Map<String, Object> effect : effects) {
            if ("COIN_MULTIPLIER".equals(String.valueOf(effect.get("kind"))) && effect.get("durationHours") != null) {
                return ((Number) effect.get("durationHours")).intValue();
            }
        }
        return extractDurationHours(effects);
    }

    /**
     * Read cosmetic shop effect payload and return granted visual asset code, if present.
     *
     * Cosmetic items should contain: { kind: "GRANT_VISUAL", visualAssetCode: "<code>" }.
     */
    private String extractGrantVisualAssetCode(ShopItem item) {
        if (item.effects == null) {
            return null;
        }
        for (Map<String, Object> effect : item.effects) {
            if ("GRANT_VISUAL".equals(String.valueOf(effect.get("kind")))) {
                Object c = effect.get("visualAssetCode");
                if (c != null) {
                    return String.valueOf(c).trim();
                }
            }
        }
        return null;
    }

    /** Read species shop effect payload and return the granted species code, if present. */
    private String extractGrantSpeciesCode(ShopItem item) {
        if (item.effects == null) {
            return null;
        }
        for (Map<String, Object> effect : item.effects) {
            if ("GRANT_SPECIES".equals(String.valueOf(effect.get("kind")))) {
                Object c = effect.get("speciesCode");
                if (c != null) {
                    return String.valueOf(c).trim();
                }
            }
        }
        return null;
    }

    private String normalizeSpeciesCode(String speciesCode) {
        return speciesCode == null ? "" : speciesCode.trim().toLowerCase(Locale.ROOT);
    }

    /** Migrate legacy mood key and ensure new lists exist; persists when changed. */
    private void migratePetVisualFields(PetState pet) {
        boolean changed = false;
        if (pet.moodAssetCodes != null && pet.moodAssetCodes.containsKey("dead")) {
            String v = pet.moodAssetCodes.remove("dead");
            if (v != null) {
                pet.moodAssetCodes.put("playing_dead", v);
            }
            changed = true;
        }
        if (pet.ownedVisualAssetCodes == null) {
            pet.ownedVisualAssetCodes = new ArrayList<>();
            changed = true;
        }
        if (pet.ownedSpeciesCodes == null) {
            pet.ownedSpeciesCodes = new ArrayList<>();
            changed = true;
        }
        for (String starterSpecies : STARTER_SPECIES) {
            if (!pet.ownedSpeciesCodes.contains(starterSpecies)) {
                pet.ownedSpeciesCodes.add(starterSpecies);
                changed = true;
            }
        }
        if (pet.speciesCode == null || pet.speciesCode.isBlank()) {
            pet.speciesCode = "dog";
            changed = true;
        }
        if (!pet.ownedSpeciesCodes.contains(pet.speciesCode) && SUPPORTED_SPECIES.contains(pet.speciesCode)) {
            pet.ownedSpeciesCodes.add(pet.speciesCode);
            changed = true;
        }
        if (changed) {
            petStateRepo.save(pet);
        }
    }

    /**
     * Simulate time progression for the pet between {@code lastSimulationAt} and now.
     *
     * Called on-demand when the user hits the API (dashboard, minigame start/finish, inventory use, etc.).
     * This avoids simulating offline users in the background.
     */
    private PetState simulate(String userId) {
        PetState pet = petStateRepo.findByUserId(userId).orElseThrow();
        migratePetVisualFields(pet);
        Instant now = Instant.now();
        double hours = Duration.between(pet.lastSimulationAt, now).toMinutes() / 60.0;
        if (hours <= 0) {
            return pet;
        }

        // "Dead" pets (hunger <= 0) are treated as frozen: we still clean up effects but we do not keep
        // evolving the simulation on every request. From the user's POV, this state is stable until
        // they consume food (which is blocked at hunger==0 in the UI loop anyway).
        if (pet.hunger <= 0) {
            pet.activeEffects = new ArrayList<>(pet.activeEffects.stream().filter(e -> e.expiresAt.isAfter(now)).toList());
            pet.lastSimulationAt = now;
            return pet;
        }

        pet.activeEffects = new ArrayList<>(pet.activeEffects.stream().filter(e -> e.expiresAt.isAfter(now)).toList());
        double regenMultiplier = pet.activeEffects.stream()
                .filter(e -> !"COIN_MULT".equals(e.bonusKind))
                .mapToDouble(e -> e.value)
                .sum();
        double hungerK = 0.029;
        double happinessK = 0.06;

        pet.hunger = clamp(pet.hunger * Math.exp(-hungerK * hours));
        pet.happiness = clamp(pet.happiness * Math.exp(-happinessK * hours));
        if (pet.hunger < 30) {
            pet.happiness = clamp(pet.happiness - hours * 1.2);
        }
        double energyRegenPerHour = 8.0 * (1.0 + regenMultiplier);
        if (pet.hunger < 30) {
            energyRegenPerHour *= 0.8;
        }
        pet.energy = clamp(pet.energy + hours * energyRegenPerHour);
        pet.lastSimulationAt = now;
        return pet;
    }

    /**
     * Create baseline pet + wallet documents for a newly registered user.
     *
     * This seeds:
     * - pet stats and defaults
     * - initial wallet coin balance
     */
    private void initializeUserGameData(String userId) {
        PetState pet = new PetState();
        pet.userId = userId;
        pet.hunger = 100;
        pet.happiness = 100;
        pet.energy = 100;
        pet.name = "Pet";
        pet.speciesCode = "dog";
        pet.ownedSpeciesCodes = new ArrayList<>(STARTER_SPECIES);
        pet.moodAssetCodes = new HashMap<>();
        pet.ownedVisualAssetCodes = new ArrayList<>();
        pet.equippedBackgroundAssetCode = null;
        pet.equippedForegroundAssetCode = null;
        pet.lastSimulationAt = Instant.now();
        petStateRepo.save(pet);

        Wallet wallet = new Wallet();
        wallet.userId = userId;
        wallet.coins = 300;
        walletRepo.save(wallet);
    }

    /**
     * Issue access + refresh tokens and return them in a JSON-friendly map.
     *
     * Tokens are stored server-side and validated by {@code AuthInterceptor}.
     */
    private Map<String, Object> issueAuthTokens(String userId, String email) {
        UserToken access = makeToken(userId, "ACCESS", Duration.ofHours(accessTokenHours));
        UserToken refresh = makeToken(userId, "REFRESH", Duration.ofDays(refreshTokenDays));
        Map<String, Object> res = new HashMap<>();
        res.put("accessToken", access.token);
        res.put("refreshToken", refresh.token);
        res.put("email", email);
        return res;
    }

    /** Forward a domain action into the SQL activity history stream. */
    private void recordActivity(String userId, String eventType, String source, PetState pet, Wallet wallet, Map<String, Object> metadata) {
        activityHistoryService.record(userId, eventType, source, pet, wallet, metadata);
    }

    /** Wallet lookup helper used only for optional activity snapshots. */
    private Wallet walletOrNull(String userId) {
        return walletRepo.findByUserId(userId).orElse(null);
    }

    /**
     * Create a token row with the given TTL.
     *
     * Token strings are random UUID pairs; the DB record holds expiry and used flag.
     */
    private UserToken makeToken(String userId, String type, Duration ttl) {
        UserToken token = new UserToken();
        token.userId = userId;
        token.type = type;
        token.token = UUID.randomUUID().toString() + "-" + UUID.randomUUID();
        token.createdAt = Instant.now();
        token.expiresAt = token.createdAt.plus(ttl);
        token.used = false;
        return userTokenRepo.save(token);
    }

    /**
     * Send a verification email after registration.
     *
     * Includes both:
     * - backend verification link (direct API call)
     * - frontend route link (nice UX)
     */
    private void sendVerificationMail(UserAccount user) {
        UserToken token = makeToken(user.id, "VERIFY_EMAIL", Duration.ofHours(24));
        sendMail(user.email, "Verify your Poe Pet account",
                "Verify here: " + apiBaseUrl + "/auth/verify-email?token=" + token.token + "\n\n"
                        + "Frontend route: " + webBaseUrl + "/verify-email?token=" + token.token);
    }

    /**
     * Send a mail message using configured SMTP.
     *
     * In local dev, failures are logged to stdout as a fallback.
     */
    private void sendMail(String to, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            message.setFrom("no-reply@poepet.local");
            mailSender.send(message);
        } catch (Exception ex) {
            // Dev fallback for local environment when SMTP is unavailable.
            System.out.println("MAIL_FALLBACK to=" + to + " subject=" + subject + " body=" + body);
        }
    }

    /**
     * Simple password policy used by register/reset endpoints.
     *
     * Rule: length >= 5 and contains at least one digit OR at least one uppercase letter.
     */
    private boolean passwordValid(String password) {
        if (password == null || password.length() < 5) {
            return false;
        }
        boolean hasDigit = password.chars().anyMatch(Character::isDigit);
        boolean hasUpper = password.chars().anyMatch(Character::isUpperCase);
        return hasDigit || hasUpper;
    }

    /** Clamp a percent-like stat value into the inclusive range [0..100]. */
    private double clamp(double value) {
        return Math.max(0, Math.min(100, value));
    }

}
