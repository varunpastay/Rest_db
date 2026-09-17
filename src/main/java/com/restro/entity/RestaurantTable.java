package com.restro.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Physical dine-in table. qr_token is embedded in the printed QR URL so table ids aren't guessable. */
@Entity
@Table(name = "restaurant_table", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"restaurant_id", "table_no"}),
        @UniqueConstraint(columnNames = {"qr_token"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RestaurantTable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "table_id")
    private Integer tableId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "restaurant_id", nullable = false)
    private Restaurant restaurant;

    @Column(name = "table_no", nullable = false, length = 20)
    private String tableNo;

    @Column(nullable = false)
    @Builder.Default
    private int capacity = 4;

    @Column(name = "qr_token", nullable = false, length = 64)
    private String qrToken;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /** Cascade+orphanRemoval so deleting a table also deletes its QR code history - otherwise the
     *  delete hits a raw FK violation the moment any QR code has ever been generated for it. */
    @OneToMany(mappedBy = "table", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<QrCode> qrCodes = new ArrayList<>();

    @PrePersist
    void onCreate() { createdAt = LocalDateTime.now(); }
}
