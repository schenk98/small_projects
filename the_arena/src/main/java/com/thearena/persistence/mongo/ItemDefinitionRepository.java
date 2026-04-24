package com.thearena.persistence.mongo;

import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ItemDefinitionRepository extends MongoRepository<ItemDefinitionDocument, String> {
    Optional<ItemDefinitionDocument> findByName(String name);
}
