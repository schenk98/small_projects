package com.readingtracker.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "quiz_responses")
@Getter
@Setter
@NoArgsConstructor
public class QuizResponse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "reading_session_id", nullable = false)
    private Long readingSessionId;

    @Column(name = "quiz_question_id", nullable = false)
    private Long quizQuestionId;

    @Column(name = "answer_text", length = 2000)
    private String answerText;

    @Column(name = "points_awarded", precision = 10, scale = 2)
    private BigDecimal pointsAwarded;
}

