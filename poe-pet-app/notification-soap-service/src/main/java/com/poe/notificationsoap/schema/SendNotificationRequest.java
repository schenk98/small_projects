package com.poe.notificationsoap.schema;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

/** SOAP request payload for one outbound email notification. */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = { "kind", "toEmail", "subject", "body" })
@XmlRootElement(name = "SendNotificationRequest", namespace = "http://poe.com/notificationsoap")
public class SendNotificationRequest {
    @XmlElement(namespace = "http://poe.com/notificationsoap", required = true)
    private String kind;

    @XmlElement(namespace = "http://poe.com/notificationsoap", required = true)
    private String toEmail;

    @XmlElement(namespace = "http://poe.com/notificationsoap", required = true)
    private String subject;

    @XmlElement(namespace = "http://poe.com/notificationsoap", required = true)
    private String body;

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    public String getToEmail() {
        return toEmail;
    }

    public void setToEmail(String toEmail) {
        this.toEmail = toEmail;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }
}
