package com.poe.backend.sql.repo;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.poe.backend.sql.model.DailyChallengeDefinition;

/** Repository for the shared daily challenge definitions generated per date. */
public interface DailyChallengeDefinitionRepo extends JpaRepository<DailyChallengeDefinition, Long> {
    /** Load the three challenge rows for a given day, in display order. */
    List<DailyChallengeDefinition> findByChallengeDateOrderBySlotOrderAsc(LocalDate challengeDate);
}
