package com.readingtracker.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "featured_posts")
@Getter
@Setter
@NoArgsConstructor
public class FeaturedPost {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 1000)
    private String text;

    @Column(name = "image_blob_id")
    private Long imageBlobId;

    @Column(name = "link_url", length = 500)
    private String linkUrl;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    @Column(nullable = false)
    private Boolean active;
}

