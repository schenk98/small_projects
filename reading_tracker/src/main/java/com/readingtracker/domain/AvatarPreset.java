package com.readingtracker.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "avatar_presets")
@Getter
@Setter
@NoArgsConstructor
public class AvatarPreset {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "image_blob_id", nullable = false)
    private Long imageBlobId;

    @Column(nullable = false)
    private Boolean active;
}

