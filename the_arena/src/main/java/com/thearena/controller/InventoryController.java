package com.thearena.controller;

import com.thearena.model.request.EquipItemRequest;
import com.thearena.model.request.ScrapItemRequest;
import com.thearena.model.response.InventoryItemResponse;
import com.thearena.model.response.InventoryOverviewResponse;
import com.thearena.service.InventoryService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/inventory")
public class InventoryController {
    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    /** All rows (stash + equipped) for debugging / legacy clients. */
    @GetMapping("/me")
    public List<InventoryItemResponse> myInventory(Authentication authentication) {
        return inventoryService.listForUser(authentication.getName());
    }

    /** Split stash vs equipped for the UI. */
    @GetMapping("/overview")
    public InventoryOverviewResponse overview(Authentication authentication) {
        return inventoryService.overview(authentication.getName());
    }

    @PostMapping("/equip")
    public void equip(@Valid @RequestBody EquipItemRequest request, Authentication authentication) {
        inventoryService.equipFromStash(authentication.getName(), request.inventoryItemId(), request.equipSlot());
    }

    @PostMapping("/scrap")
    public void scrap(@Valid @RequestBody ScrapItemRequest request, Authentication authentication) {
        inventoryService.scrap(authentication.getName(), request.inventoryItemId());
    }
}
