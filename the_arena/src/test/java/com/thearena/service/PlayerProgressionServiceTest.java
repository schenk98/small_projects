package com.thearena.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.thearena.persistence.mysql.PlayerEntity;
import com.thearena.persistence.mysql.PlayerRepository;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PlayerProgressionServiceTest {
    @Mock
    private PlayerRepository playerRepository;
    @InjectMocks
    private PlayerProgressionService playerProgressionService;

    @Test
    void grantVictoryXpLevelsUpAndCarriesOverXp() {
        PlayerEntity player = new PlayerEntity("Jakub", 1, 80, Instant.now());
        when(playerRepository.findByUsername("Jakub")).thenReturn(Optional.of(player));
        when(playerRepository.save(any(PlayerEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PlayerEntity updated = playerProgressionService.grantVictoryXp("Jakub");

        assertEquals(2, updated.getLevel());
        assertEquals(20, updated.getXp());
    }
}
