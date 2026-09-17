package com.restro.entity;

import jakarta.persistence.*;
import lombok.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.LocalDateTime;

@Entity
@Table(name = "qr_code")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QrCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "qr_code_id")
    private Integer qrCodeId;

    /** @JsonIgnore breaks the RestaurantTable.qrCodes <-> QrCode.table cycle. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "table_id", nullable = false)
    @JsonIgnore
    private RestaurantTable table;

    @Column(name = "image_path", nullable = false)
    private String imagePath;

    @Column(name = "target_url", nullable = false, length = 500)
    private String targetUrl;

    @Column(name = "generated_at", nullable = false, updatable = false)
    private LocalDateTime generatedAt;

    @PrePersist
    void onCreate() { generatedAt = LocalDateTime.now(); }
}
