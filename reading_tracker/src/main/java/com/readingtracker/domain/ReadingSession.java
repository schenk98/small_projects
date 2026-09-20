package com.readingtracker.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "reading_sessions")
@Getter
@Setter
@NoArgsConstructor
public class ReadingSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "book_id", nullable = false)
    private Long bookId;

    @Column(name = "session_date", nullable = false)
    private Instant sessionDate;

    @Column(nullable = false)
    private Integer minutes;

    @Column(name = "pages_from", nullable = false)
    private Integer pagesFrom;

    @Column(name = "pages_to", nullable = false)
    private Integer pagesTo;

    @Column(length = 1000)
    private String note;

    @Column(name = "points_awarded", precision = 10, scale = 2)
    private BigDecimal pointsAwarded;

    @Column(name = "marked_finished")
    private Boolean markedFinished;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}

