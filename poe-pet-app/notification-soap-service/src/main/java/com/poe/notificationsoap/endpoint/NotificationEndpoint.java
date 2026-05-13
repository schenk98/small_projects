package com.poe.notificationsoap.endpoint;

import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;
import org.springframework.ws.server.endpoint.annotation.RequestPayload;
import org.springframework.ws.server.endpoint.annotation.ResponsePayload;

import com.poe.notificationsoap.schema.SendNotificationRequest;
import com.poe.notificationsoap.schema.SendNotificationResponse;
import com.poe.notificationsoap.service.NotificationMailService;

/** SOAP endpoint that exposes the sendNotification operation. */
@Endpoint
public class NotificationEndpoint {
    private static final String NAMESPACE_URI = "http://poe.com/notificationsoap";

    private final NotificationMailService notificationMailService;

    public NotificationEndpoint(NotificationMailService notificationMailService) {
        this.notificationMailService = notificationMailService;
    }

    @PayloadRoot(namespace = NAMESPACE_URI, localPart = "SendNotificationRequest")
    @ResponsePayload
    public SendNotificationResponse sendNotification(@RequestPayload SendNotificationRequest request) {
        return notificationMailService.send(request);
    }
}
