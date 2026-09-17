package com.restro.Service;

/**
 * Atomic, database-backed counters (order numbers, invoice numbers, ...).
 * Deliberately NOT an in-memory counter: an in-memory AtomicInteger resets
 * to 0 on every app restart/redeploy, so the very next order placed that
 * same day would try to reuse a number already used earlier that day and
 * crash on the database's unique constraint. Backing this by a real table
 * survives restarts and stays correct under concurrent requests, since the
 * increment happens as a single atomic UPDATE at the database level.
 */
public interface SequenceService {

    /** Returns the next value for the given key, starting at 1 the first time a key is used. */
    long next(String key);
}
