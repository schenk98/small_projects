package com.poe.backend.model;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("user_tokens")
public class UserToken {
    @Id
    public String id;
    public String userId;
    public String token;
    public String type; // ACCESS, REFRESH, VERIFY_EMAIL, RESET_PASSWORD
    public Instant expiresAt;
    public Instant createdAt;
    public boolean used;
}
