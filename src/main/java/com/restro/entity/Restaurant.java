package com.restro.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;

/** One row per deployment - single-restaurant, single-owner system. */
@Entity
@Table(name = "restaurant")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Restaurant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "restaurant_id")
    private Integer restaurantId;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(name = "logo_path")
    private String logoPath;

    @Column(name = "banner_path")
    private String bannerPath;

    @Column(length = 500)
    private String address;

    @Column(length = 20)
    private String phone;

    @Column(length = 150)
    private String email;

    @Column(length = 20)
    private String gstin;

    /** UPI VPA (e.g. "restaurant@okhdfcbank") used to render a pay-enabled UPI QR on invoices.
     *  Left null/blank means invoices print without a payment QR. */
    @Column(name = "upi_id", length = 100)
    private String upiId;

    @Column(name = "currency_code", nullable = false, length = 10)
    @Builder.Default
    private String currencyCode = "INR";

    @Column(name = "currency_symbol", nullable = false, length = 5)
    @Builder.Default
    private String currencySymbol = "\u20B9";

    @Column(name = "service_charge_percent", nullable = false, precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal serviceChargePercent = BigDecimal.ZERO;

    @Column(name = "theme_color", nullable = false, length = 20)
    @Builder.Default
    private String themeColor = "#c0392b";

    @Column(name = "dark_mode_default", nullable = false)
    @Builder.Default
    private boolean darkModeDefault = false;

    @Column(name = "opening_time")
    private LocalTime openingTime;

    @Column(name = "closing_time")
    private LocalTime closingTime;

    @Column(name = "is_open", nullable = false)
    @Builder.Default
    private boolean open = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
