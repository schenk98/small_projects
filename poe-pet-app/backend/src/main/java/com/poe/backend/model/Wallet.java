package com.poe.backend.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("wallets")
public class Wallet {
    @Id
    public String id;
    public String userId;
    public int coins;
}
