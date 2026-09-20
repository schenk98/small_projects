package com.readingtracker.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "purchases")
@Getter
@Setter
@NoArgsConstructor
public class Purchase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "shop_item_id", nullable = false)
    private Long shopItemId;

    @Column(name = "price_points", precision = 10, scale = 2, nullable = false)
    private BigDecimal pricePoints;

    @Column(name = "purchased_at", nullable = false)
    private Instant purchasedAt;
}

