package com.thearena.persistence.mongo;

import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface BattleEventRepository extends MongoRepository<BattleEventDocument, String> {
    List<BattleEventDocument> findByBattleIdOrderByTurnNumberAsc(String battleId);
    List<BattleEventDocument> findByBattleIdAndTurnNumberOrderByCreatedAtAsc(String battleId, int turnNumber);
}
