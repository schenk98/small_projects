package com.poe.backend.sql.repo;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.poe.backend.sql.model.AchievementDefinition;

/** Relational repository for achievement catalog rows. */
public interface AchievementDefinitionRepo extends JpaRepository<AchievementDefinition, Long> {
    /** List active achievements ordered for predictable UI rendering. */
    List<AchievementDefinition> findByActiveTrueOrderBySortOrderAscCodeAsc();

    /** Load active achievements that should react to a specific activity event. */
    List<AchievementDefinition> findByActiveTrueAndTriggerEventTypeOrderBySortOrderAscCodeAsc(String triggerEventType);
}
