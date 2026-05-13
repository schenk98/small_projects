package com.poe.backend.notification;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.poe.backend.model.PetState;
import com.poe.backend.model.UserAccount;
import com.poe.backend.repo.UserAccountRepo;
import com.poe.backend.service.AppService;
import com.poe.backend.sql.model.ActivityEvent;
import com.poe.backend.sql.model.NotificationDelivery;
import com.poe.backend.sql.model.NotificationPreference;
import com.poe.backend.sql.repo.ActivityEventRepo;
import com.poe.backend.sql.repo.NotificationDeliveryRepo;
import com.poe.backend.sql.repo.NotificationPreferenceRepo;

/**
 * Orchestrates the first automated notification flows.
 *
 * The business rules stay intentionally simple for the MVP:
 * - low-hunger reminder: at most once per user per UTC day
 * - daily AI summary: at most once per user per UTC day
 */
@Service
public class NotificationAutomationService {
    private static final DateTimeFormatter DAY_KEY = DateTimeFormatter.ISO_LOCAL_DATE;

    private final NotificationPreferenceRepo notificationPreferenceRepo;
    private final NotificationDeliveryRepo notificationDeliveryRepo;
    private final UserAccountRepo userAccountRepo;
    private final ActivityEventRepo activityEventRepo;
    private final AppService appService;
    private final NotificationSoapClient notificationSoapClient;

    @Value("${app.notificationsEnabled:false}")
    private boolean notificationsEnabled;

    @Value("${app.notificationLowHungerThreshold:15}")
    private int lowHungerThreshold;

    public NotificationAutomationService(
            NotificationPreferenceRepo notificationPreferenceRepo,
            NotificationDeliveryRepo notificationDeliveryRepo,
            UserAccountRepo userAccountRepo,
            ActivityEventRepo activityEventRepo,
            AppService appService,
            NotificationSoapClient notificationSoapClient) {
        this.notificationPreferenceRepo = notificationPreferenceRepo;
        this.notificationDeliveryRepo = notificationDeliveryRepo;
        this.userAccountRepo = userAccountRepo;
        this.activityEventRepo = activityEventRepo;
        this.appService = appService;
        this.notificationSoapClient = notificationSoapClient;
    }

    /** Periodically scan opted-in users for low-hunger reminders. */
    @Scheduled(fixedDelayString = "${app.notificationSweepDelayMs:900000}")
    public void lowHungerSweep() {
        if (!notificationsEnabled) {
            return;
        }
        for (NotificationPreference preference : notificationPreferenceRepo.findByLowHungerEnabledTrue()) {
            sendLowHungerReminder(preference.userId);
        }
    }

    /** Periodically scan opted-in users for daily summaries. */
    @Scheduled(fixedDelayString = "${app.notificationDailySummarySweepDelayMs:3600000}", initialDelayString = "${app.notificationDailySummaryInitialDelayMs:120000}")
    public void dailyAiSummarySweep() {
        if (!notificationsEnabled) {
            return;
        }
        for (NotificationPreference preference : notificationPreferenceRepo.findByDailyAiSummaryEnabledTrue()) {
            sendDailyAiSummary(preference.userId);
        }
    }

    /** Manually trigger low-hunger logic for one user (used by tests/dev tooling). */
    public NotificationSoapResult sendLowHungerReminder(String userId) {
        UserAccount user = verifiedUserOrNull(userId);
        if (user == null) {
            return new NotificationSoapResult(false, "user_not_eligible");
        }
        PetState pet = appService.getPet(userId);
        if (Math.round(pet.hunger) >= lowHungerThreshold) {
            return new NotificationSoapResult(false, "hunger_not_low");
        }
        String day = LocalDate.now(ZoneOffset.UTC).format(DAY_KEY);
        String deliveryKey = "LOW_HUNGER:" + userId + ":" + day;
        if (notificationDeliveryRepo.existsByDeliveryKeyAndSuccessTrue(deliveryKey)) {
            return new NotificationSoapResult(false, "already_sent_today");
        }
        String petName = pet.name == null || pet.name.isBlank() ? "Pet" : pet.name;
        String subject = petName + " is getting hungry";
        String body = "Your pet " + petName + " is at " + Math.round(pet.hunger) + "/100 hunger. "
                + "Feed it soon so it feels better.";
        return sendAndRecord(user, "LOW_HUNGER", deliveryKey, subject, body);
    }

    /** Manually trigger a daily summary for one user (used by tests/dev tooling). */
    public NotificationSoapResult sendDailyAiSummary(String userId) {
        UserAccount user = verifiedUserOrNull(userId);
        if (user == null) {
            return new NotificationSoapResult(false, "user_not_eligible");
        }
        String day = LocalDate.now(ZoneOffset.UTC).format(DAY_KEY);
        String deliveryKey = "DAILY_AI_SUMMARY:" + userId + ":" + day;
        if (notificationDeliveryRepo.existsByDeliveryKeyAndSuccessTrue(deliveryKey)) {
            return new NotificationSoapResult(false, "already_sent_today");
        }
        PetState pet = appService.getPet(userId);
        String petName = pet.name == null || pet.name.isBlank() ? "Pet" : pet.name;
        String subject = petName + "'s daily summary";
        String body = buildDailySummaryBody(pet, activityEventRepo.findTop20ByUserIdOrderByHappenedAtDescIdDesc(userId));
        return sendAndRecord(user, "DAILY_AI_SUMMARY", deliveryKey, subject, body);
    }

    private String buildDailySummaryBody(PetState pet, List<ActivityEvent> recentActivity) {
        String petName = pet.name == null || pet.name.isBlank() ? "Pet" : pet.name;
        String species = pet.speciesCode == null || pet.speciesCode.isBlank() ? "pet" : pet.speciesCode;
        String recent = recentActivity.stream()
                .limit(3)
                .map(this::humanizeActivity)
                .reduce((a, b) -> a + "\n- " + b)
                .map(v -> "- " + v)
                .orElse("- No notable activity was recorded today.");
        return """
                Daily summary for %s (%s)

                Current stats:
                - Hunger: %d/100
                - Happiness: %d/100
                - Energy: %d/100

                Recent activity:
                %s
                """.formatted(
                petName,
                species,
                Math.round(pet.hunger),
                Math.round(pet.happiness),
                Math.round(pet.energy),
                recent);
    }

    private String humanizeActivity(ActivityEvent event) {
        if (event == null || event.eventType == null) {
            return "Recorded activity";
        }
        return switch (event.eventType) {
            case "SHOP_PURCHASED" -> "Bought an item";
            case "CONSUMABLE_USED" -> "Used a consumable";
            case "MINIGAME_FINISHED" -> "Finished a minigame";
            case "PET_RENAMED" -> "Renamed the pet";
            case "PET_SPECIES_SET" -> "Changed the pet species";
            case "AI_CHAT_SENT" -> "Chatted with the pet AI";
            case "USER_LOGGED_IN" -> "Logged in";
            default -> event.eventType.replace('_', ' ').toLowerCase(Locale.ROOT);
        };
    }

    private NotificationSoapResult sendAndRecord(UserAccount user, String kind, String deliveryKey, String subject, String body) {
        NotificationSoapResult result = notificationSoapClient.send(kind, user.email, subject, body);
        NotificationDelivery delivery = new NotificationDelivery();
        delivery.userId = user.id;
        delivery.kind = kind;
        delivery.deliveryKey = result.accepted() ? deliveryKey : deliveryKey + ":FAILED:" + Instant.now().toEpochMilli();
        delivery.targetEmail = user.email;
        delivery.subject = subject;
        delivery.bodyPreview = body.length() > 1000 ? body.substring(0, 1000) : body;
        delivery.success = result.accepted();
        delivery.responseMessage = result.message();
        delivery.createdAt = Instant.now();
        notificationDeliveryRepo.save(delivery);
        return result;
    }

    private UserAccount verifiedUserOrNull(String userId) {
        return userAccountRepo.findById(userId)
                .filter(user -> user.email != null && !user.email.isBlank())
                .filter(user -> user.emailVerified)
                .orElse(null);
    }
}
