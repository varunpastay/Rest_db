package com.restro.util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/** Pure formatting only - see OrderNumberUtil for why the sequence itself moved to SequenceService. */
public final class InvoiceNumberUtil {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private InvoiceNumberUtil() {
    }

    public static String todayKey() {
        return "invoice-" + LocalDate.now().format(DATE_FMT);
    }

    public static String format(long sequence) {
        String datePart = LocalDate.now().format(DATE_FMT);
        return "INV-" + datePart + "-" + String.format("%04d", sequence);
    }
}
