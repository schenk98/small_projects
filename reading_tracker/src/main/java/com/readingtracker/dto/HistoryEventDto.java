package com.readingtracker.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@AllArgsConstructor
public class HistoryEventDto {
    private Long id;
    private String type;  // SESSION, QUIZ, PURCHASE, ADJUSTMENT
    private String description;
    private BigDecimal pointsChange;  // + or -
    private Instant eventDate;
    private String details;  // JSON or formatted text

    // For edit capability
    private boolean editable;
    private Long relatedEntityId;  // session ID, purchase ID, adjustment ID
}

