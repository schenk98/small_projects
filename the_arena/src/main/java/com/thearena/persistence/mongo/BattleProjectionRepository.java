package com.thearena.persistence.mongo;

import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface BattleProjectionRepository extends MongoRepository<BattleProjectionDocument, String> {
    Optional<BattleProjectionDocument> findByBattleId(String battleId);
}
