package com.thearena.persistence.mongo;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface CombatLogRepository extends MongoRepository<CombatLogDocument, String> {
}
