package com.restro.entity;

import jakarta.persistence.*;
import lombok.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.LocalDateTime;

@Entity
@Table(name = "food_image")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FoodImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "food_image_id")
    private Integer foodImageId;

    /** @JsonIgnore breaks the FoodItem.images <-> FoodImage.foodItem cycle - without it, any
     *  JSON/JS serialization of a FoodItem (e.g. the owner menu page's edit-modal data) recurses
     *  forever: FoodItem -> images -> FoodImage -> foodItem -> images -> ... */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "food_item_id", nullable = false)
    @JsonIgnore
    private FoodItem foodItem;

    @Column(name = "image_path", nullable = false)
    private String imagePath;

    @Column(name = "is_primary", nullable = false)
    @Builder.Default
    private boolean primary = false;

    @Column(name = "display_order", nullable = false)
    @Builder.Default
    private int displayOrder = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() { createdAt = LocalDateTime.now(); }
}
