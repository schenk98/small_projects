package com.readingtracker.repository;

import com.readingtracker.domain.QuizResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuizResponseRepository extends JpaRepository<QuizResponse, Long> {
    List<QuizResponse> findByReadingSessionId(Long readingSessionId);
}

