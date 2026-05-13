package com.poe.notificationsoap.endpoint;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

import com.poe.notificationsoap.schema.SendNotificationRequest;
import com.poe.notificationsoap.schema.SendNotificationResponse;
import com.poe.notificationsoap.service.NotificationMailService;

class NotificationEndpointTest {
    private final NotificationMailService notificationMailService = org.mockito.Mockito.mock(NotificationMailService.class);
    private final NotificationEndpoint endpoint = new NotificationEndpoint(notificationMailService);

    @Test
    void sendNotificationDelegatesToMailService() {
        SendNotificationRequest request = new SendNotificationRequest();
        SendNotificationResponse response = new SendNotificationResponse();
        response.setAccepted(true);
        response.setMessage("queued");
        when(notificationMailService.send(request)).thenReturn(response);

        SendNotificationResponse actual = endpoint.sendNotification(request);

        assertSame(response, actual);
    }
}
