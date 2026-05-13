package com.poe.backend.notification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.poe.backend.model.PetState;
import com.poe.backend.model.UserAccount;
import com.poe.backend.repo.UserAccountRepo;
import com.poe.backend.service.AppService;
import com.poe.backend.sql.model.ActivityEvent;
import com.poe.backend.sql.repo.ActivityEventRepo;
import com.poe.backend.sql.repo.NotificationDeliveryRepo;
import com.poe.backend.sql.repo.NotificationPreferenceRepo;

class NotificationAutomationServiceTest {
    private final NotificationPreferenceRepo notificationPreferenceRepo = org.mockito.Mockito.mock(NotificationPreferenceRepo.class);
    private final NotificationDeliveryRepo notificationDeliveryRepo = org.mockito.Mockito.mock(NotificationDeliveryRepo.class);
    private final UserAccountRepo userAccountRepo = org.mockito.Mockito.mock(UserAccountRepo.class);
    private final ActivityEventRepo activityEventRepo = org.mockito.Mockito.mock(ActivityEventRepo.class);
    private final AppService appService = org.mockito.Mockito.mock(AppService.class);
    private final NotificationSoapClient notificationSoapClient = org.mockito.Mockito.mock(NotificationSoapClient.class);

    private final NotificationAutomationService service = new NotificationAutomationService(
            notificationPreferenceRepo,
            notificationDeliveryRepo,
            userAccountRepo,
            activityEventRepo,
            appService,
            notificationSoapClient);

    NotificationAutomationServiceTest() {
        ReflectionTestUtils.setField(service, "notificationsEnabled", true);
        ReflectionTestUtils.setField(service, "lowHungerThreshold", 15);
    }

    @Test
    void sendLowHungerReminderSendsAndRecordsWhenPetIsBelowThreshold() {
        UserAccount user = verifiedUser("u1");
        PetState pet = pet("Miki", 10, 80, 70);

        when(userAccountRepo.findById("u1")).thenReturn(Optional.of(user));
        when(appService.getPet("u1")).thenReturn(pet);
        when(notificationDeliveryRepo.existsByDeliveryKeyAndSuccessTrue(org.mockito.ArgumentMatchers.startsWith("LOW_HUNGER:u1:")))
                .thenReturn(false);
        when(notificationSoapClient.send(any(), any(), any(), any())).thenReturn(new NotificationSoapResult(true, "queued"));

        NotificationSoapResult result = service.sendLowHungerReminder("u1");

        assertTrue(result.accepted());
        verify(notificationDeliveryRepo).save(any());
    }

    @Test
    void sendLowHungerReminderSkipsWhenAlreadySentToday() {
        when(userAccountRepo.findById("u1")).thenReturn(Optional.of(verifiedUser("u1")));
        when(appService.getPet("u1")).thenReturn(pet("Miki", 5, 80, 70));
        when(notificationDeliveryRepo.existsByDeliveryKeyAndSuccessTrue(org.mockito.ArgumentMatchers.startsWith("LOW_HUNGER:u1:")))
                .thenReturn(true);

        NotificationSoapResult result = service.sendLowHungerReminder("u1");

        assertFalse(result.accepted());
        assertEquals("already_sent_today", result.message());
        verify(notificationSoapClient, never()).send(any(), any(), any(), any());
    }

    @Test
    void sendDailyAiSummaryBuildsSummaryFromRecentActivity() {
        UserAccount user = verifiedUser("u1");
        PetState pet = pet("Miki", 50, 90, 60);
        ActivityEvent event = new ActivityEvent();
        event.eventType = "MINIGAME_FINISHED";
        event.happenedAt = Instant.parse("2026-05-12T12:00:00Z");

        when(userAccountRepo.findById("u1")).thenReturn(Optional.of(user));
        when(appService.getPet("u1")).thenReturn(pet);
        when(notificationDeliveryRepo.existsByDeliveryKeyAndSuccessTrue(org.mockito.ArgumentMatchers.startsWith("DAILY_AI_SUMMARY:u1:")))
                .thenReturn(false);
        when(activityEventRepo.findTop20ByUserIdOrderByHappenedAtDescIdDesc("u1")).thenReturn(List.of(event));
        when(notificationSoapClient.send(any(), any(), any(), any())).thenReturn(new NotificationSoapResult(true, "queued"));

        NotificationSoapResult result = service.sendDailyAiSummary("u1");

        assertTrue(result.accepted());
        verify(notificationSoapClient).send(
                org.mockito.ArgumentMatchers.eq("DAILY_AI_SUMMARY"),
                org.mockito.ArgumentMatchers.eq("u1@example.com"),
                org.mockito.ArgumentMatchers.contains("Miki"),
                org.mockito.ArgumentMatchers.contains("Finished a minigame"));
    }

    @Test
    void failedAttemptUsesNonBlockingAuditKey() {
        when(userAccountRepo.findById("u1")).thenReturn(Optional.of(verifiedUser("u1")));
        when(appService.getPet("u1")).thenReturn(pet("Miki", 5, 80, 70));
        when(notificationDeliveryRepo.existsByDeliveryKeyAndSuccessTrue(org.mockito.ArgumentMatchers.startsWith("LOW_HUNGER:u1:")))
                .thenReturn(false);
        when(notificationSoapClient.send(any(), any(), any(), any())).thenReturn(new NotificationSoapResult(false, "soap_down"));

        service.sendLowHungerReminder("u1");

        verify(notificationDeliveryRepo).save(org.mockito.ArgumentMatchers.argThat(saved ->
                !saved.success && saved.deliveryKey.contains(":FAILED:")));
    }

    private UserAccount verifiedUser(String id) {
        UserAccount user = new UserAccount();
        user.id = id;
        user.email = id + "@example.com";
        user.emailVerified = true;
        return user;
    }

    private PetState pet(String name, double hunger, double happiness, double energy) {
        PetState pet = new PetState();
        pet.name = name;
        pet.hunger = hunger;
        pet.happiness = happiness;
        pet.energy = energy;
        pet.speciesCode = "dog";
        return pet;
    }
}
