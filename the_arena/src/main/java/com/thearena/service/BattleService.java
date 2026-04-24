package com.thearena.service;

import com.thearena.exception.BadRequestException;
import com.thearena.exception.ForbiddenException;
import com.thearena.model.BattleSession;
import com.thearena.model.combat.Contestant;
import com.thearena.model.combat.ContestantType;
import com.thearena.model.combat.Consumable;
import com.thearena.model.combat.Item;
import com.thearena.model.combat.LootStorageType;
import com.thearena.model.combat.Weapon;
import com.thearena.model.request.StartArenaRequest;
import com.thearena.model.response.StartArenaResponse;
import com.thearena.model.response.TurnResponse;
import com.thearena.persistence.mysql.UserAccountEntity;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

@Service
public class BattleService {
    private static final int INITIAL_PLAYER_HP = 100;

    private final Map<String, BattleSession> sessions = new ConcurrentHashMap<>();
    private final MonsterCatalogService monsterCatalogService;
    private final CombatLogService combatLogService;
    private final PlayerProgressionService playerProgressionService;
    private final AchievementService achievementService;
    private final AccountService accountService;
    private final BattleSessionPersistenceService battleSessionPersistenceService;
    private final BattleHistoryService battleHistoryService;
    private final LootService lootService;
    private final InventoryService inventoryService;
    private final ContestantLoadoutSnapshotService contestantLoadoutSnapshotService;

    public BattleService(
            MonsterCatalogService monsterCatalogService,
            CombatLogService combatLogService,
            PlayerProgressionService playerProgressionService,
            AchievementService achievementService,
            AccountService accountService,
            BattleSessionPersistenceService battleSessionPersistenceService,
            BattleHistoryService battleHistoryService,
            LootService lootService,
            InventoryService inventoryService,
            ContestantLoadoutSnapshotService contestantLoadoutSnapshotService
    ) {
        this.monsterCatalogService = monsterCatalogService;
        this.combatLogService = combatLogService;
        this.playerProgressionService = playerProgressionService;
        this.achievementService = achievementService;
        this.accountService = accountService;
        this.battleSessionPersistenceService = battleSessionPersistenceService;
        this.battleHistoryService = battleHistoryService;
        this.lootService = lootService;
        this.inventoryService = inventoryService;
        this.contestantLoadoutSnapshotService = contestantLoadoutSnapshotService;
    }

    public StartArenaResponse startBattle(String authenticatedUsername, StartArenaRequest request) {
        playerProgressionService.ensurePlayer(authenticatedUsername);
        inventoryService.ensureStarterGear(authenticatedUsername);
        UserAccountEntity account = accountService.getByUsername(authenticatedUsername);

        String sessionId = UUID.randomUUID().toString();
        long seed = Math.abs(new Random().nextLong());
        String mode = request == null || request.mode() == null || request.mode().isBlank()
                ? "RANDOM"
                : request.mode().trim();

        Contestant player;
        Contestant enemy;
        if ("DUEL".equalsIgnoreCase(mode)) {
            String pid = request.playerTemplateId();
            String eid = request.enemyTemplateId();
            if (pid == null || eid == null || pid.isBlank() || eid.isBlank()) {
                throw new BadRequestException(
                        "DUEL_PARAMS",
                        "DUEL mode requires playerTemplateId and enemyTemplateId (Mongo monster_templates document ids)."
                );
            }
            player = monsterCatalogService.contestantFromTemplateId(pid, authenticatedUsername, ContestantType.PLAYER, INITIAL_PLAYER_HP);
            enemy = monsterCatalogService.contestantFromTemplateId(eid, null, ContestantType.ENEMY, 0);
        } else {
            player = inventoryService.buildContestantFromEquipped(authenticatedUsername, INITIAL_PLAYER_HP);
            enemy = monsterCatalogService.randomMonsterWithRandomizedLoadout(seed);
        }

        BattleSession session = new BattleSession(
                sessionId,
                player,
                enemy,
                seed,
                contestantLoadoutSnapshotService.freezeAtBattleStart(player),
                contestantLoadoutSnapshotService.freezeAtBattleStart(enemy)
        );
        sessions.put(sessionId, session);

        String message = "Battle started: " + player.getName() + " vs " + enemy.getName();
        combatLogService.log(sessionId, message);
        battleSessionPersistenceService.create(sessionId, account.getId(), seed, session);
        battleHistoryService.recordInitialSnapshot(session);

        return new StartArenaResponse(
                session.getSessionId(),
                message,
                session.getPlayer().getName(),
                session.getPlayer().getHealth(),
                weaponName(session.getPlayer()),
                session.getEnemy().getName(),
                session.getEnemy().getHealth(),
                weaponName(session.getEnemy())
        );
    }

    public TurnResponse processTurn(String sessionId, String action, String username) {
        BattleSession session = sessions.get(sessionId);
        if (session == null) {
            throw new BadRequestException("SESSION_NOT_FOUND", "Session not found: " + sessionId);
        }
        if (!session.getPlayer().getName().equals(username)) {
            throw new ForbiddenException("SESSION_ACCESS_DENIED", "Access denied for session: " + sessionId);
        }
        if (session.isFinished()) {
            return new TurnResponse(
                    session.getSessionId(),
                    action,
                    "Battle already finished.",
                    session.getPlayer().getHealth(),
                    session.getEnemy().getHealth(),
                    true,
                    resolveWinner(session)
            );
        }

        String normalizedAction = action.trim().toLowerCase();
        return switch (normalizedAction) {
            case "attack" -> handleAttack(session);
            default -> throw new BadRequestException("UNSUPPORTED_ACTION", "Unsupported action: " + action + ". Supported: attack");
        };
    }

    public void snapshotAndCloseSession(String sessionId, String username) {
        BattleSession session = sessions.get(sessionId);
        if (session == null) {
            return;
        }
        if (!session.getPlayer().getName().equals(username)) {
            throw new ForbiddenException("SESSION_ACCESS_DENIED", "Access denied for session: " + sessionId);
        }
        battleHistoryService.recordTurn(session, "logout_snapshot", "Session snapshot created on logout/termination.");
        battleSessionPersistenceService.updateTurn(sessionId, session.getRoundNumber(), "PAUSED", session);
    }

    private TurnResponse handleAttack(BattleSession session) {
        session.incrementRound();
        achievementService.unlock(session.getPlayer().getName(), AchievementService.FIRST_ATTACK);

        int playerDamage = effectiveDamage(session.getPlayer(), session.getEnemy());
        int enemyDamage = effectiveDamage(session.getEnemy(), session.getPlayer());
        session.getEnemy().setHealth(Math.max(0, session.getEnemy().getHealth() - playerDamage));

        String message;
        if (session.getEnemy().getHealth() == 0) {
            session.setFinished(true);
            playerProgressionService.grantVictoryXp(session.getPlayer().getName());
            achievementService.unlock(session.getPlayer().getName(), AchievementService.FIRST_BATTLE_WIN);
            Item loot = lootService.rollLoot(LootStorageType.CHEST, session.getSeed(), session.getRoundNumber());
            inventoryService.storeLoot(session.getPlayer().getName(), loot);
            message = "You hit " + session.getEnemy().getName() + " for " + playerDamage + " and won the battle.";
            message += " Loot found: " + loot.getName() + " (" + loot.getItemType() + ").";
        } else {
            session.getPlayer().setHealth(Math.max(0, session.getPlayer().getHealth() - enemyDamage));
            if (session.getPlayer().getHealth() == 0) {
                session.setFinished(true);
                message = "You hit for " + playerDamage + ", but " + session.getEnemy().getName()
                        + " countered for " + enemyDamage + ". You were defeated.";
            } else {
                message = "You hit " + session.getEnemy().getName() + " for " + playerDamage
                        + ". " + session.getEnemy().getName() + " hit back for " + enemyDamage + ".";
            }
        }
        combatLogService.log(session.getSessionId(), message);
        battleHistoryService.recordTurn(session, "attack", message);
        if (session.isFinished()) {
            battleHistoryService.recordFinalSnapshot(session, resolveWinner(session));
        }
        battleSessionPersistenceService.updateTurn(
                session.getSessionId(),
                session.getRoundNumber(),
                session.isFinished() ? "ENDED" : "PLAYER",
                session
        );

        return new TurnResponse(
                session.getSessionId(),
                "attack",
                message,
                session.getPlayer().getHealth(),
                session.getEnemy().getHealth(),
                session.isFinished(),
                resolveWinner(session)
        );
    }

    private int effectiveDamage(Contestant attacker, Contestant defender) {
        Weapon weapon = attacker.primaryWeapon();
        if (weapon == null) {
            return 1;
        }

        int physical = weapon.getPhysicalDamage();
        int magical = weapon.getMagicalDamage();

        int physicalAfterDefense = Math.max(0, physical - defender.totalPhysicalReduction());
        int magicalAfterDefense = Math.max(0, magical - defender.totalMagicalReduction());
        int total = physicalAfterDefense + magicalAfterDefense;

        total = applyAttackSpecial(total, weapon.getSpecialEffect(), attacker, defender);
        total = applyDefenseSpecial(total, defender.getBodyWear() == null ? "" : defender.getBodyWear().getSpecialDefense());
        total = applyAccessorySpecial(total, attacker.hasAccessoryToken("lucky"));

        return Math.max(1, total);
    }

    private int applyAttackSpecial(int damage, String token, Contestant attacker, Contestant defender) {
        if (token == null || token.isBlank()) {
            return damage;
        }
        Random rng = new Random(attacker.getName().hashCode() ^ defender.getName().hashCode());
        if ("burning".equalsIgnoreCase(token)) {
            return damage + 2;
        }
        if ("critical".equalsIgnoreCase(token) && rng.nextInt(100) < 20) {
            return damage + 4;
        }
        return damage;
    }

    private int applyDefenseSpecial(int damage, String token) {
        if (token == null || token.isBlank()) {
            return damage;
        }
        if ("thorns".equalsIgnoreCase(token)) {
            return Math.max(1, damage - 1);
        }
        return damage;
    }

    private int applyAccessorySpecial(int damage, boolean hasLuckyAccessory) {
        if (!hasLuckyAccessory) {
            return damage;
        }
        return damage + 1;
    }

    private String weaponName(Contestant contestant) {
        return contestant.primaryWeapon() == null ? "Unarmed" : contestant.primaryWeapon().getName();
    }

    private String resolveWinner(BattleSession session) {
        if (!session.isFinished()) {
            return null;
        }
        if (session.getEnemy().getHealth() == 0) {
            return session.getPlayer().getName();
        }
        if (session.getPlayer().getHealth() == 0) {
            return session.getEnemy().getName();
        }
        return null;
    }
}
