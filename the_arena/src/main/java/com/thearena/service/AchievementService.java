package com.thearena.service;

import com.thearena.persistence.mysql.AchievementEntity;
import com.thearena.persistence.mysql.AchievementRepository;
import com.thearena.persistence.mysql.UserAccountEntity;
import com.thearena.persistence.mysql.UserAccountRepository;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AchievementService {
    public static final String FIRST_LOGIN = "FIRST_LOGIN";
    public static final String FIRST_ATTACK = "FIRST_ATTACK";
    public static final String FIRST_BATTLE_WIN = "FIRST_BATTLE_WIN";

    private final AchievementRepository achievementRepository;
    private final UserAccountRepository userAccountRepository;

    public AchievementService(AchievementRepository achievementRepository, UserAccountRepository userAccountRepository) {
        this.achievementRepository = achievementRepository;
        this.userAccountRepository = userAccountRepository;
    }

    @Transactional
    public void unlock(String username, String code) {
        UserAccountEntity account = userAccountRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Account not found: " + username));

        if (!achievementRepository.existsByAccountIdAndCode(account.getId(), code)) {
            achievementRepository.save(new AchievementEntity(account.getId(), code, Instant.now()));
        }
    }

    @Transactional(readOnly = true)
    public List<String> listCodes(String username) {
        UserAccountEntity account = userAccountRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Account not found: " + username));
        return achievementRepository.findByAccountId(account.getId()).stream().map(AchievementEntity::getCode).toList();
    }
}
