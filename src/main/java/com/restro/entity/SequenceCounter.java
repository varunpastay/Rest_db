package com.restro.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Backing table for SequenceService's atomic order/invoice numbering.
 * This entity exists ONLY so Hibernate's ddl-auto=update creates the table
 * automatically, the same way it does for every other table in this app -
 * SequenceServiceImpl talks to this table directly via JdbcTemplate (for
 * the atomic INSERT ... ON DUPLICATE KEY UPDATE ... LAST_INSERT_ID()
 * pattern, which doesn't map cleanly onto standard JPA operations), not
 * through this entity. There is no repository for it and no other code
 * should read/write it through JPA.
 */
@Entity
@Table(name = "sequence_counter")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SequenceCounter {

    @Id
    @Column(name = "seq_key", length = 50)
    private String seqKey;

    @Column(name = "current_value", nullable = false)
    private Long currentValue;
}
