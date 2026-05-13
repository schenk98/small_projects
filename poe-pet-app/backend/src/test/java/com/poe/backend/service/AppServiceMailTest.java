package com.poe.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import com.poe.backend.model.PetState;
import com.poe.backend.model.UserAccount;
import com.poe.backend.model.UserToken;
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

/** Ensures transactional auth email uses {@link JavaMailSender} (MailHog / SMTP in dev). */
class AppServiceMailTest {
    private final UserAccountRepo userAccountRepo = mock(UserAccountRepo.class);
    private final UserTokenRepo userTokenRepo = mock(UserTokenRepo.class);
    private final PetStateRepo petStateRepo = mock(PetStateRepo.class);
    private final WalletRepo walletRepo = mock(WalletRepo.class);
    private final ShopItemRepo shopItemRepo = mock(ShopItemRepo.class);
    private final InventoryRepo inventoryRepo = mock(InventoryRepo.class);
    private final MinigameRepo minigameRepo = mock(MinigameRepo.class);
    private final MinigameSessionRepo minigameSessionRepo = mock(MinigameSessionRepo.class);
    private final PetVisualAssetRepo petVisualAssetRepo = mock(PetVisualAssetRepo.class);
    private final JavaMailSender mailSender = mock(JavaMailSender.class);
    private final ActivityHistoryService activityHistoryService = mock(ActivityHistoryService.class);
    private final NotificationPreferenceService notificationPreferenceService = mock(NotificationPreferenceService.class);

    private AppService appService;

    @BeforeEach
    void initService() {
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
                mailSender,
                mock(AiGatewayClient.class),
                activityHistoryService,
                notificationPreferenceService,
                mock(AiChatContextAssembler.class));
        ReflectionTestUtils.setField(appService, "webBaseUrl", "http://web.test");
        ReflectionTestUtils.setField(appService, "apiBaseUrl", "http://api.test");
        ReflectionTestUtils.setField(appService, "skipEmailVerification", false);
    }

    @Test
    void registerInvokesMailSenderForVerificationEmail() {
        when(userAccountRepo.findByEmail("user@example.com")).thenReturn(Optional.empty());
        when(userAccountRepo.save(any(UserAccount.class))).thenAnswer(invocation -> {
            UserAccount u = invocation.getArgument(0);
            u.id = "u1";
            return u;
        });
        AtomicReference<PetState> petRef = new AtomicReference<>();
        when(petStateRepo.save(any(PetState.class))).thenAnswer(invocation -> {
            petRef.set(invocation.getArgument(0));
            return invocation.getArgument(0);
        });
        when(petStateRepo.findByUserId("u1")).thenAnswer(invocation -> Optional.ofNullable(petRef.get()));
        AtomicReference<Wallet> walletRef = new AtomicReference<>();
        when(walletRepo.save(any(Wallet.class))).thenAnswer(invocation -> {
            walletRef.set(invocation.getArgument(0));
            return invocation.getArgument(0);
        });
        when(walletRepo.findByUserId("u1")).thenAnswer(invocation -> Optional.ofNullable(walletRef.get()));
        when(userTokenRepo.save(any(UserToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        appService.register("user@example.com", "Abcd1");

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        SimpleMailMessage sent = captor.getValue();
        assertEquals("user@example.com", sent.getTo()[0]);
        assertEquals("Verify your Poe Pet account", sent.getSubject());
        assertTrue(sent.getText().contains("http://api.test/auth/verify-email?token="));
        assertTrue(sent.getText().contains("http://web.test/verify-email?token="));
    }

    @Test
    void forgotPasswordInvokesMailSenderWhenUserExists() {
        UserAccount user = new UserAccount();
        user.id = "u1";
        user.email = "user@example.com";
        when(userAccountRepo.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(userTokenRepo.save(any(UserToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        appService.forgotPassword("user@example.com");

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        assertEquals("Reset password", captor.getValue().getSubject());
        assertTrue(captor.getValue().getText().contains("http://web.test/reset-password?token="));
    }
}
