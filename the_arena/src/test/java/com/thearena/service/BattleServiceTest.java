package com.thearena.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.thearena.model.combat.Contestant;
import com.thearena.model.combat.ContestantType;
import com.thearena.model.combat.Consumable;
import com.thearena.model.combat.Weapon;
import com.thearena.model.response.StartArenaResponse;
import com.thearena.model.response.TurnResponse;
import com.thearena.persistence.mysql.UserAccountEntity;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BattleServiceTest {
    @Mock
    private MonsterCatalogService monsterCatalogService;
    @Mock
    private CombatLogService combatLogService;
    @Mock
    private PlayerProgressionService playerProgressionService;
    @Mock
    private AchievementService achievementService;
    @Mock
    private AccountService accountService;
    @Mock
    private BattleSessionPersistenceService battleSessionPersistenceService;
    @Mock
    private BattleHistoryService battleHistoryService;
    @Mock
    private LootService lootService;
    @Mock
    private InventoryService inventoryService;
    @Mock
    private ContestantLoadoutSnapshotService contestantLoadoutSnapshotService;
    @InjectMocks
    private BattleService battleService;

    @Test
    void startBattleCreatesSession() {
        UserAccountEntity account = mock(UserAccountEntity.class);
        when(account.getId()).thenReturn(1L);

        when(inventoryService.buildContestantFromEquipped(anyString(), anyInt()))
                .thenReturn(new Contestant("Jakub", ContestantType.PLAYER, "", 100, new Weapon("Training Sword", 15), null, null, null, List.of()));
        when(monsterCatalogService.randomMonsterWithRandomizedLoadout(anyLong()))
                .thenReturn(new Contestant("Test Goblin", ContestantType.ENEMY, "", 70, new Weapon("Dagger", 8), null, null, null, List.of()));
        when(contestantLoadoutSnapshotService.freezeAtBattleStart(any()))
                .thenReturn(Map.of("snapshot", true));
        when(playerProgressionService.ensurePlayer(anyString())).thenReturn(null);
        when(accountService.getByUsername(anyString())).thenReturn(account);
        doNothing().when(combatLogService).log(anyString(), anyString());
        doNothing().when(battleSessionPersistenceService).create(anyString(), any(), anyLong(), any());
        doNothing().when(battleHistoryService).recordInitialSnapshot(any());

        StartArenaResponse response = battleService.startBattle("Jakub", null);

        assertNotNull(response.sessionId());
        assertEquals("Jakub", response.playerName());
        assertEquals(100, response.playerHealth());
        assertEquals(70, response.enemyHealth());
        assertEquals("Training Sword", response.playerWeapon());
    }

    @Test
    void attackTurnReducesBothHpUntilFinished() {
        UserAccountEntity account = mock(UserAccountEntity.class);
        when(account.getId()).thenReturn(1L);

        when(inventoryService.buildContestantFromEquipped(anyString(), anyInt()))
                .thenReturn(new Contestant("Jakub", ContestantType.PLAYER, "", 100, new Weapon("Training Sword", 15), null, null, null, List.of()));
        when(monsterCatalogService.randomMonsterWithRandomizedLoadout(anyLong()))
                .thenReturn(new Contestant("Test Goblin", ContestantType.ENEMY, "", 70, new Weapon("Dagger", 8), null, null, null, List.of()));
        when(contestantLoadoutSnapshotService.freezeAtBattleStart(any()))
                .thenReturn(Map.of("snapshot", true));
        when(playerProgressionService.ensurePlayer(anyString())).thenReturn(null);
        when(playerProgressionService.grantVictoryXp(anyString())).thenReturn(null);
        when(accountService.getByUsername(anyString())).thenReturn(account);
        when(lootService.rollLoot(any(), anyLong(), anyInt()))
                .thenReturn(new Consumable("Small Potion", "heal", 20));
        doNothing().when(combatLogService).log(anyString(), anyString());
        doNothing().when(battleSessionPersistenceService).create(anyString(), any(), anyLong(), any());
        doNothing().when(battleSessionPersistenceService).updateTurn(anyString(), anyInt(), anyString(), any());
        doNothing().when(battleHistoryService).recordInitialSnapshot(any());
        doNothing().when(battleHistoryService).recordTurn(any(), anyString(), anyString());
        doNothing().when(battleHistoryService).recordFinalSnapshot(any(), anyString());
        doNothing().when(achievementService).unlock(anyString(), anyString());
        doNothing().when(inventoryService).storeLoot(anyString(), any());

        StartArenaResponse start = battleService.startBattle("Jakub", null);

        TurnResponse firstTurn = battleService.processTurn(start.sessionId(), "attack", "Jakub");
        assertFalse(firstTurn.finished());
        assertTrue(firstTurn.playerHealth() < 100);
        assertTrue(firstTurn.enemyHealth() < 70);

        TurnResponse secondTurn = battleService.processTurn(start.sessionId(), "attack", "Jakub");
        TurnResponse thirdTurn = battleService.processTurn(start.sessionId(), "attack", "Jakub");
        TurnResponse fourthTurn = battleService.processTurn(start.sessionId(), "attack", "Jakub");
        TurnResponse fifthTurn = battleService.processTurn(start.sessionId(), "attack", "Jakub");

        assertTrue(fifthTurn.finished());
        assertEquals(0, fifthTurn.enemyHealth());
        assertEquals("Jakub", fifthTurn.winner());
        assertNotNull(secondTurn.message());
        assertNotNull(thirdTurn.message());
        assertNotNull(fourthTurn.message());
    }
}
