package com.thearena.persistence.mysql;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryItemRepository extends JpaRepository<InventoryItemEntity, Long> {
    List<InventoryItemEntity> findByAccountIdOrderByAcquiredAtDesc(Long accountId);

    long countByAccountId(Long accountId);

    List<InventoryItemEntity> findByAccountIdAndBucketOrderByAcquiredAtDesc(Long accountId, InventoryBucket bucket);

    Optional<InventoryItemEntity> findByIdAndAccountId(Long id, Long accountId);

    Optional<InventoryItemEntity> findByAccountIdAndBucketAndEquipSlot(Long accountId, InventoryBucket bucket, EquipSlot equipSlot);
}
