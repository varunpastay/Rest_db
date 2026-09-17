package com.restro.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "food_item")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FoodItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "food_item_id")
    private Integer foodItemId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "restaurant_id", nullable = false)
    private Restaurant restaurant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(length = 1000)
    private String description;

    @Column(length = 1000)
    private String ingredients;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "offer_price", precision = 10, scale = 2)
    private BigDecimal offerPrice;

    @Column(name = "prep_time_minutes", nullable = false)
    @Builder.Default
    private int prepTimeMinutes = 15;

    @Enumerated(EnumType.STRING)
    @Column(name = "food_type", nullable = false, length = 20)
    @Builder.Default
    private FoodType foodType = FoodType.VEG;

    @Enumerated(EnumType.STRING)
    @Column(name = "spice_level", nullable = false, length = 20)
    @Builder.Default
    private SpiceLevel spiceLevel = SpiceLevel.MEDIUM;

    @Column(name = "is_available", nullable = false)
    @Builder.Default
    private boolean available = true;

    @Column(name = "is_recommended", nullable = false)
    @Builder.Default
    private boolean recommended = false;

    @Column(name = "is_bestseller", nullable = false)
    @Builder.Default
    private boolean bestseller = false;

    @Column(name = "display_order", nullable = false)
    @Builder.Default
    private int displayOrder = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "foodItem", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("displayOrder ASC")
    @Builder.Default
    private List<FoodImage> images = new ArrayList<>();

    @PrePersist
    void onCreate() { createdAt = LocalDateTime.now(); updatedAt = createdAt; }

    @PreUpdate
    void onUpdate() { updatedAt = LocalDateTime.now(); }

    /** Price actually charged - offer price when set, else the base price. */
    @Transient
    public BigDecimal getEffectivePrice() {
        return offerPrice != null ? offerPrice : price;
    }

    /** Convenience accessor for the primary/first image path, used on menu cards. */
    @Transient
    public String getPrimaryImagePath() {
        return images.stream()
                .filter(FoodImage::isPrimary)
                .findFirst()
                .or(() -> images.stream().findFirst())
                .map(FoodImage::getImagePath)
                .orElse(null);
    }
}
