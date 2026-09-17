package com.restro.entity;

import jakarta.persistence.*;
import lombok.*;

/** Generic key/value overflow for small toggles (e.g. kitchen notification sound). */
@Entity
@Table(name = "settings", uniqueConstraints = @UniqueConstraint(columnNames = {"restaurant_id", "setting_key"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Settings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "setting_id")
    private Integer settingId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "restaurant_id", nullable = false)
    private Restaurant restaurant;

    @Column(name = "setting_key", nullable = false, length = 100)
    private String settingKey;

    @Column(name = "setting_value", length = 1000)
    private String settingValue;
}
