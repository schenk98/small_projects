package com.poe.backend.notification;

/** Result of one downstream SOAP notification request. */
public record NotificationSoapResult(boolean accepted, String message) {
}
