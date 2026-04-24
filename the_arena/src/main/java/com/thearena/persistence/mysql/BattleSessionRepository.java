package com.thearena.persistence.mysql;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BattleSessionRepository extends JpaRepository<BattleSessionEntity, Long> {
    Optional<BattleSessionEntity> findByBattleId(String battleId);
}
