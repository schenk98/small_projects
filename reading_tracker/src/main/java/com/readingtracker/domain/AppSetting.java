package com.readingtracker.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "app_settings")
@Getter
@Setter
@NoArgsConstructor
public class AppSetting {

    @Id
    @Column(name = "setting_key", nullable = false, length = 255)
    private String key;

    @Column(name = "setting_value", nullable = false, length = 1000)
    private String value;
}
