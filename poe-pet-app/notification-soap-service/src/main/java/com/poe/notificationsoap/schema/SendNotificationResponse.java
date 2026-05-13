package com.poe.notificationsoap.schema;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

/** SOAP response payload returned after a send attempt. */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = { "accepted", "message" })
@XmlRootElement(name = "SendNotificationResponse", namespace = "http://poe.com/notificationsoap")
public class SendNotificationResponse {
    @XmlElement(namespace = "http://poe.com/notificationsoap", required = true)
    private boolean accepted;

    @XmlElement(namespace = "http://poe.com/notificationsoap", required = true)
    private String message;

    public boolean isAccepted() {
        return accepted;
    }

    public void setAccepted(boolean accepted) {
        this.accepted = accepted;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
