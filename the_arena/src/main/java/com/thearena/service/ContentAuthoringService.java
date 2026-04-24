package com.thearena.service;

import com.thearena.model.request.CreateContestantTemplateRequest;
import com.thearena.model.request.CreateItemDefinitionRequest;
import com.thearena.persistence.mongo.ItemDefinitionDocument;
import com.thearena.persistence.mongo.ItemDefinitionRepository;
import com.thearena.persistence.mongo.MonsterTemplateDocument;
import com.thearena.persistence.mongo.MonsterTemplateRepository;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class ContentAuthoringService {
    private final ItemDefinitionRepository itemDefinitionRepository;
    private final MonsterTemplateRepository monsterTemplateRepository;

    public ContentAuthoringService(
            ItemDefinitionRepository itemDefinitionRepository,
            MonsterTemplateRepository monsterTemplateRepository
    ) {
        this.itemDefinitionRepository = itemDefinitionRepository;
        this.monsterTemplateRepository = monsterTemplateRepository;
    }

    public List<ItemDefinitionDocument> listItems() {
        return itemDefinitionRepository.findAll();
    }

    public ItemDefinitionDocument createItem(CreateItemDefinitionRequest request) {
        ItemDefinitionDocument doc = new ItemDefinitionDocument(
                request.itemType(),
                request.name(),
                request.description() == null ? "" : request.description(),
                request.image() == null ? "" : request.image(),
                request.specialEffect() == null ? "" : request.specialEffect(),
                request.attributes() == null ? Map.of() : request.attributes()
        );
        return itemDefinitionRepository.save(doc);
    }

    public List<MonsterTemplateDocument> listContestants() {
        return monsterTemplateRepository.findAll();
    }

    public MonsterTemplateDocument createContestant(CreateContestantTemplateRequest request) {
        MonsterTemplateDocument doc = new MonsterTemplateDocument(
                request.name(),
                request.description() == null ? "" : request.description(),
                request.baseHealth(),
                blankToNull(request.leftHandItemId()),
                blankToNull(request.rightHandItemId()),
                blankToNull(request.bodyWearItemId()),
                blankToNull(request.accessoryItemId())
        );
        return monsterTemplateRepository.save(doc);
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
