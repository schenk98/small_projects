package com.poe.notificationsoap;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Standalone SOAP notification service for real email delivery. */
@SpringBootApplication
public class NotificationSoapServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(NotificationSoapServiceApplication.class, args);
    }
}
