package com.restro.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/** Uploaded image bytes (logos, banners, food photos, generated QR codes) stored as BLOBs - no filesystem state needed. */
@Entity
@Table(name = "uploaded_file", uniqueConstraints = @UniqueConstraint(columnNames = {"relative_path"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UploadedFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "file_id")
    private Integer fileId;

    @Column(name = "relative_path", nullable = false, length = 255)
    private String relativePath;

    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType;

    /** Explicit LONGBLOB - without a length hint here, Hibernate infers the smallest MySQL blob
     *  type (TINYBLOB, 255-byte max) for a bare byte[], which silently truncates every real image
     *  (food photos, branding, generated QR codes) and throws "Data truncation" on insert. */
    @Lob
    @Column(nullable = false, columnDefinition = "LONGBLOB")
    private byte[] data;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() { createdAt = LocalDateTime.now(); }
}
