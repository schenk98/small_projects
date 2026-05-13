package com.poe.notificationsoap.schema;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.StringReader;

import javax.xml.transform.stream.StreamSource;

import org.junit.jupiter.api.Test;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;

class SendNotificationRequestXmlBindingTest {
    @Test
    void unmarshalsNamespacedSoapPayloadShape() throws Exception {
        Jaxb2Marshaller marshaller = new Jaxb2Marshaller();
        marshaller.setClassesToBeBound(SendNotificationRequest.class, SendNotificationResponse.class);
        marshaller.afterPropertiesSet();

        String xml = """
                <not:SendNotificationRequest xmlns:not="http://poe.com/notificationsoap">
                  <not:kind>LOW_HUNGER</not:kind>
                  <not:toEmail>user@example.com</not:toEmail>
                  <not:subject>Subject</not:subject>
                  <not:body>Body</not:body>
                </not:SendNotificationRequest>
                """;

        SendNotificationRequest request = (SendNotificationRequest) marshaller.unmarshal(new StreamSource(new StringReader(xml)));

        assertEquals("LOW_HUNGER", request.getKind());
        assertEquals("user@example.com", request.getToEmail());
        assertEquals("Subject", request.getSubject());
        assertEquals("Body", request.getBody());
    }
}
