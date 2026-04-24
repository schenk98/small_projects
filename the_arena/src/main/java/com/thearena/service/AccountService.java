package com.thearena.service;

import com.thearena.persistence.mysql.UserAccountEntity;
import com.thearena.persistence.mysql.UserAccountRepository;
import org.springframework.stereotype.Service;

@Service
public class AccountService {
    private final UserAccountRepository userAccountRepository;

    public AccountService(UserAccountRepository userAccountRepository) {
        this.userAccountRepository = userAccountRepository;
    }

    public UserAccountEntity getByUsername(String username) {
        return userAccountRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Account not found: " + username));
    }
}
