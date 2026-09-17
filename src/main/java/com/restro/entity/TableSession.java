package com.restro.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * One "visit" at a table - opens automatically the moment the first order
 * (from whichever phone) is placed after the table was last free, and
 * closes the moment the owner settles the combined bill. Every order
 * placed at that table while the session is open - regardless of which
 * customer's phone placed it - attaches to this same session, so billing
 * combines everything into one total instead of one bill per phone.
 * Once closed, the table is "free" again: the very next order starts a
 * brand new session with nothing carried over from the last one.
 */
@Entity
@Table(name = "table_session")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TableSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "table_session_id")
    private Integer tableSessionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "restaurant_id", nullable = false)
    private Restaurant restaurant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "table_id", nullable = false)
    private RestaurantTable table;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private TableSessionStatus status = TableSessionStatus.OPEN;

    @Column(name = "opened_at", nullable = false, updatable = false)
    private LocalDateTime openedAt;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @OneToMany(mappedBy = "tableSession", fetch = FetchType.LAZY)
    @OrderBy("createdAt ASC")
    @Builder.Default
    private List<Order> orders = new ArrayList<>();

    @OneToOne(mappedBy = "tableSession", fetch = FetchType.LAZY)
    private Payment payment;

    @PrePersist
    void onCreate() {
        openedAt = LocalDateTime.now();
    }
}
