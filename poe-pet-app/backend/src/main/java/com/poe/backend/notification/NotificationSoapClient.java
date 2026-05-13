package com.poe.backend.notification;

import java.io.StringReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

/**
 * Very small SOAP client for the notification side-service.
 *
 * This uses raw XML over HTTP on purpose so the main app can stay lightweight
 * and independent from generated SOAP stubs.
 */
@Component
public class NotificationSoapClient {
    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Value("${app.notificationsEnabled:false}")
    private boolean notificationsEnabled;

    @Value("${app.notificationSoapEnabled:false}")
    private boolean notificationSoapEnabled;

    @Value("${app.notificationSoapUrl:}")
    private String notificationSoapUrl;

    public boolean isEnabled() {
        return notificationsEnabled && notificationSoapEnabled && notificationSoapUrl != null && !notificationSoapUrl.isBlank();
    }

    /** Send one notification request to the SOAP side-service. */
    public NotificationSoapResult send(String kind, String toEmail, String subject, String body) {
        if (!isEnabled()) {
            return new NotificationSoapResult(false, "notification_soap_disabled");
        }
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(notificationSoapUrl))
                    .header("Content-Type", "text/xml; charset=utf-8")
                    .header("SOAPAction", "sendNotification")
                    .POST(HttpRequest.BodyPublishers.ofString(envelope(kind, toEmail, subject, body), StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return new NotificationSoapResult(false, "soap_http_" + response.statusCode());
            }
            return parseResponse(response.body());
        } catch (Exception e) {
            return new NotificationSoapResult(false, e.getMessage() == null ? "soap_error" : e.getMessage());
        }
    }

    private String envelope(String kind, String toEmail, String subject, String body) {
        return """
                <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:not="http://poe.com/notificationsoap">
                  <soapenv:Header/>
                  <soapenv:Body>
                    <not:SendNotificationRequest>
                      <not:kind>%s</not:kind>
                      <not:toEmail>%s</not:toEmail>
                      <not:subject>%s</not:subject>
                      <not:body>%s</not:body>
                    </not:SendNotificationRequest>
                  </soapenv:Body>
                </soapenv:Envelope>
                """.formatted(
                escapeXml(kind),
                escapeXml(toEmail),
                escapeXml(subject),
                escapeXml(body));
    }

    private NotificationSoapResult parseResponse(String xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        Document document = factory.newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
        var xpath = XPathFactory.newInstance().newXPath();
        String accepted = (String) xpath.evaluate("//*[local-name()='accepted']/text()", document, XPathConstants.STRING);
        String message = (String) xpath.evaluate("//*[local-name()='message']/text()", document, XPathConstants.STRING);
        return new NotificationSoapResult("true".equalsIgnoreCase(accepted), message == null ? "" : message);
    }

    private String escapeXml(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }
}
