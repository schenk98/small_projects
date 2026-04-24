package com.thearena.persistence.mysql;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AchievementRepository extends JpaRepository<AchievementEntity, Long> {
    boolean existsByAccountIdAndCode(Long accountId, String code);
    List<AchievementEntity> findByAccountId(Long accountId);
}
