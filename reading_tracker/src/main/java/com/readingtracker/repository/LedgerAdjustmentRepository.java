package com.readingtracker.repository;

import com.readingtracker.domain.LedgerAdjustment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LedgerAdjustmentRepository extends JpaRepository<LedgerAdjustment, Long> {
    List<LedgerAdjustment> findByUserId(Long userId);
}

