package com.thearena.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.thearena.model.combat.Contestant;
import com.thearena.model.combat.ContestantType;
import com.thearena.model.combat.Weapon;
import com.thearena.model.combat.WeaponHandMode;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ContestantLoadoutSnapshotServiceTest {

    private final ContestantLoadoutSnapshotService service = new ContestantLoadoutSnapshotService();

    @Test
    void freezeAtBattleStartCapturesSlotsAndStartingHealth() {
        Contestant c = new Contestant(
                "Hero",
                ContestantType.PLAYER,
                "Test hero",
                100,
                new Weapon("Blade", 10),
                null,
                null,
                null,
                List.of()
        );

        Map<String, Object> snap = service.freezeAtBattleStart(c);

        assertEquals("Hero", snap.get("name"));
        assertEquals("PLAYER", snap.get("type"));
        assertEquals(100, snap.get("startingHealth"));
        @SuppressWarnings("unchecked")
        Map<String, Object> slots = (Map<String, Object>) snap.get("slots");
        assertNotNull(slots);
        @SuppressWarnings("unchecked")
        Map<String, Object> left = (Map<String, Object>) slots.get("leftHand");
        assertNotNull(left);
        assertEquals("WEAPON", left.get("kind"));
        assertEquals("Blade", left.get("name"));
        assertEquals(10, left.get("physicalDamage"));
        assertTrue(snap.containsKey("carriedItems"));
    }
}
