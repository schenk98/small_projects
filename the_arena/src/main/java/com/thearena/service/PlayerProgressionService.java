package com.thearena.service;

import com.thearena.persistence.mysql.PlayerEntity;
import com.thearena.persistence.mysql.PlayerRepository;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PlayerProgressionService {
    private static final int XP_PER_VICTORY = 40;
    private static final int XP_PER_LEVEL = 100;

    private final PlayerRepository playerRepository;

    public PlayerProgressionService(PlayerRepository playerRepository) {
        this.playerRepository = playerRepository;
    }

    @Transactional
    public PlayerEntity ensurePlayer(String username) {
        return playerRepository.findByUsername(username)
                .orElseGet(() -> playerRepository.save(new PlayerEntity(username, 1, 0, Instant.now())));
    }

    @Transactional
    public PlayerEntity grantVictoryXp(String username) {
        PlayerEntity player = ensurePlayer(username);
        int totalXp = player.getXp() + XP_PER_VICTORY;
        int levelsGained = totalXp / XP_PER_LEVEL;
        int remainingXp = totalXp % XP_PER_LEVEL;

        player.setLevel(player.getLevel() + levelsGained);
        player.setXp(remainingXp);
        return playerRepository.save(player);
    }
}
