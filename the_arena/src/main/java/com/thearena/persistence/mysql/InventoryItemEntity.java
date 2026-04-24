package com.thearena.persistence.mysql;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.PrePersist;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "inventory_items")
public class InventoryItemEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long accountId;

    @Column(nullable = false, length = 64)
    private String itemType;

    @Column(nullable = false)
    private String itemName;

    @Lob
    @Column(nullable = false)
    private String payloadJson;

    @Column(nullable = false)
    private Instant acquiredAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private InventoryBucket bucket = InventoryBucket.STASH;

    @Enumerated(EnumType.STRING)
    @Column(length = 24)
    private EquipSlot equipSlot;

    protected InventoryItemEntity() {}

    public InventoryItemEntity(
            Long accountId,
            String itemType,
            String itemName,
            String payloadJson,
            Instant acquiredAt,
            InventoryBucket bucket,
            EquipSlot equipSlot
    ) {
        this.accountId = accountId;
        this.itemType = itemType;
        this.itemName = itemName;
        this.payloadJson = payloadJson;
        this.acquiredAt = acquiredAt;
        this.bucket = bucket == null ? InventoryBucket.STASH : bucket;
        this.equipSlot = equipSlot;
    }

    public Long getId() { return id; }
    public Long getAccountId() { return accountId; }
    public String getItemType() { return itemType; }
    public String getItemName() { return itemName; }
    public String getPayloadJson() { return payloadJson; }
    public Instant getAcquiredAt() { return acquiredAt; }

    public InventoryBucket getBucket() {
        return bucket == null ? InventoryBucket.STASH : bucket;
    }

    public void setBucket(InventoryBucket bucket) {
        this.bucket = bucket;
    }

    public EquipSlot getEquipSlot() {
        return equipSlot;
    }

    public void setEquipSlot(EquipSlot equipSlot) {
        this.equipSlot = equipSlot;
    }

    @PrePersist
    void prePersist() {
        if (bucket == null) {
            bucket = InventoryBucket.STASH;
        }
    }
}
