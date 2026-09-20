package com.readingtracker.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "shop_items")
@Getter
@Setter
@NoArgsConstructor
public class ShopItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(length = 1000)
    private String description;

    @Column(name = "price_points", precision = 10, scale = 2, nullable = false)
    private BigDecimal pricePoints;

    @Column(name = "cooldown_days")
    private Integer cooldownDays;

    @Column(nullable = false)
    private Boolean active;
}

