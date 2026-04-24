package com.thearena.controller;

import com.thearena.model.request.CreateContestantTemplateRequest;
import com.thearena.model.request.CreateItemDefinitionRequest;
import com.thearena.persistence.mongo.ItemDefinitionDocument;
import com.thearena.persistence.mongo.MonsterTemplateDocument;
import com.thearena.service.ContentAuthoringService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/content")
public class ContentController {
    private final ContentAuthoringService contentAuthoringService;

    public ContentController(ContentAuthoringService contentAuthoringService) {
        this.contentAuthoringService = contentAuthoringService;
    }

    @GetMapping("/items")
    public List<ItemDefinitionDocument> listItems() {
        return contentAuthoringService.listItems();
    }

    @PostMapping("/items")
    public ItemDefinitionDocument createItem(@Valid @RequestBody CreateItemDefinitionRequest request) {
        return contentAuthoringService.createItem(request);
    }

    @GetMapping("/contestants")
    public List<MonsterTemplateDocument> listContestants() {
        return contentAuthoringService.listContestants();
    }

    @PostMapping("/contestants")
    public MonsterTemplateDocument createContestant(@Valid @RequestBody CreateContestantTemplateRequest request) {
        return contentAuthoringService.createContestant(request);
    }
}
