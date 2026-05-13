package com.poe.backend.sql.repo;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.poe.backend.sql.model.ActivityEvent;

/** Relational repository for append-only activity history. */
public interface ActivityEventRepo extends JpaRepository<ActivityEvent, Long> {
    /** Load the newest activity rows for a user, newest first. */
    List<ActivityEvent> findTop20ByUserIdOrderByHappenedAtDescIdDesc(String userId);
}
