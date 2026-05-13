package com.poe.notificationsoap.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import com.poe.notificationsoap.schema.SendNotificationRequest;

class NotificationMailServiceTest {
    private final JavaMailSender mailSender = org.mockito.Mockito.mock(JavaMailSender.class);
    private final NotificationMailService service = new NotificationMailService(mailSender);

    @Test
    void sendReturnsAcceptedWhenMailSenderSucceeds() {
        ReflectionTestUtils.setField(service, "fromAddress", "no-reply@test.local");

        var response = service.send(request());

        assertTrue(response.isAccepted());
        verify(mailSender).send(any(org.springframework.mail.SimpleMailMessage.class));
    }

    @Test
    void sendReturnsRejectedWhenMailSenderThrows() {
        ReflectionTestUtils.setField(service, "fromAddress", "no-reply@test.local");
        doThrow(new RuntimeException("smtp down")).when(mailSender).send(any(org.springframework.mail.SimpleMailMessage.class));

        var response = service.send(request());

        assertFalse(response.isAccepted());
    }

    private SendNotificationRequest request() {
        SendNotificationRequest request = new SendNotificationRequest();
        request.setKind("LOW_HUNGER");
        request.setToEmail("user@example.com");
        request.setSubject("Subject");
        request.setBody("Body");
        return request;
    }
}
