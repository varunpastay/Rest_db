package com.restro.entity;

/**
 * Deliberately short lifecycle so the owner can process an order in as few
 * taps as possible: a new order is PENDING until the owner taps Accept,
 * then ACCEPTED until they tap Served, then SERVED until payment is taken
 * (which flips it to COMPLETED). CANCELLED covers a scrapped order at any
 * point before payment.
 */
public enum OrderStatus {
    PENDING, ACCEPTED, SERVED, COMPLETED, CANCELLED
}
