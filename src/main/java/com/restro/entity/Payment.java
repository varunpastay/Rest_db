package com.restro.entity;

import jakarta.persistence.*;
import lombok.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Created only once a table's session is settled - one payment covers every order in that
 *  session (however many phones placed them), which is why this is keyed by TableSession rather
 *  than by an individual Order. Absence of a row for a session means "unpaid". */
@Entity
@Table(name = "payment", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"table_session_id"}),
        @UniqueConstraint(columnNames = {"invoice_no"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payment_id")
    private Integer paymentId;

    /** @JsonIgnore breaks the TableSession.payment <-> Payment.tableSession cycle. */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "table_session_id", nullable = false)
    @JsonIgnore
    private TableSession tableSession;

    @Column(name = "invoice_no", nullable = false, length = 30)
    private String invoiceNo;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private PaymentMethod method = PaymentMethod.CASH;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false, length = 20)
    @Builder.Default
    private PaymentStatus paymentStatus = PaymentStatus.PAID;

    @Column(name = "paid_at", nullable = false)
    private LocalDateTime paidAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() { createdAt = LocalDateTime.now(); if (paidAt == null) paidAt = createdAt; }
}
