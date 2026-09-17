package com.restro.util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Pure formatting only - the actual sequence number comes from
 * SequenceService (database-backed, safe across restarts and concurrent
 * requests). Format: ORD-YYYYMMDD-NNNN, growing past 4 digits on a very
 * busy day rather than wrapping back to 0001 and colliding.
 */
public final class OrderNumberUtil {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private OrderNumberUtil() {
    }

    public static String todayKey() {
        return "order-" + LocalDate.now().format(DATE_FMT);
    }

    public static String format(long sequence) {
        String datePart = LocalDate.now().format(DATE_FMT);
        return "ORD-" + datePart + "-" + String.format("%04d", sequence);
    }
}
