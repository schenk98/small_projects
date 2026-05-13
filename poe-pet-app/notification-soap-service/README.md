# Notification SOAP Service

Small standalone SOAP side-service for Poe Pet notification delivery.

Current scope:
- exposes one SOAP operation: `sendNotification`
- sends real email through SMTP
- intended first caller: `poe-pet-app/backend`
- local default SMTP target: `MailHog`

Local defaults:
- HTTP port: `8081`
- SOAP path: `http://localhost:8081/ws`
- WSDL: `http://localhost:8081/ws/notifications.wsdl`

This service is intentionally narrow so the main app can stay REST-focused while still learning SOAP through a bounded integration.
