package com.poe.notificationsoap.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import com.poe.notificationsoap.schema.SendNotificationRequest;
import com.poe.notificationsoap.schema.SendNotificationResponse;

/** Sends email notifications via SMTP for incoming SOAP requests. */
@Service
public class NotificationMailService {
    private final JavaMailSender mailSender;

    @Value("${notification.fromAddress:no-reply@poe-pet.local}")
    private String fromAddress;

    public NotificationMailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /** Attempt to send one email and return a SOAP-friendly result payload. */
    public SendNotificationResponse send(SendNotificationRequest request) {
        SendNotificationResponse response = new SendNotificationResponse();
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(request.getToEmail());
            message.setSubject(request.getSubject());
            message.setText(request.getBody());
            mailSender.send(message);
            response.setAccepted(true);
            response.setMessage("queued");
        } catch (Exception e) {
            response.setAccepted(false);
            response.setMessage(e.getMessage() == null ? "mail_error" : e.getMessage());
        }
        return response;
    }
}
