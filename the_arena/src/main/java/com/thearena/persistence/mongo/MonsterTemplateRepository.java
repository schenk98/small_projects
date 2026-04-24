package com.thearena.persistence.mongo;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface MonsterTemplateRepository extends MongoRepository<MonsterTemplateDocument, String> {
}
