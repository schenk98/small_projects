package com.poe.backend.model;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("users")
public class UserAccount {
    @Id
    public String id;
    public String email;
    public String passwordHash;
    public boolean emailVerified;
    /** When true, developer tools unlock in UI (combined with privileged email config). */
    public boolean privileged;
    public Instant createdAt;
}
