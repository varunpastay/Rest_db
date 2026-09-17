package com.restro.Impl;

import com.restro.Service.SequenceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Classic MySQL atomic-sequence-emulation trick: INSERT ... ON DUPLICATE KEY
 * UPDATE with LAST_INSERT_ID(expr) lets MySQL hand back the freshly
 * incremented value safely under concurrent access, without an explicit
 * table lock or a separate SELECT ... FOR UPDATE round trip - the increment
 * and the read-back happen as one atomic operation at the database level.
 * REQUIRES_NEW so a caller's own longer-running transaction never holds
 * this row's lock any longer than the single increment takes.
 */
@Service
public class SequenceServiceImpl implements SequenceService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public long next(String key) {
        jdbcTemplate.update(
                "INSERT INTO sequence_counter (seq_key, current_value) VALUES (?, 1) " +
                        "ON DUPLICATE KEY UPDATE current_value = LAST_INSERT_ID(current_value + 1)",
                key);
        Long value = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        return value == null ? 1L : value;
    }
}
