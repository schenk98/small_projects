package com.readingtracker.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "image_blobs")
@Getter
@Setter
@NoArgsConstructor
public class ImageBlob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "mime_type", nullable = false, length = 100)
    private String mimeType;

    @Lob
    @Column(nullable = false)
    private byte[] data;
}

