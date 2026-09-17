package com.restro.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Converts a rupee amount into words using the Indian numbering system (thousand/lakh/crore),
 *  for the "Amount in words" line printed on invoices - e.g. 1234.50 -> "One Thousand Two
 *  Hundred Thirty Four Rupees and Fifty Paise Only". */
public final class NumberToWordsUtil {

    private static final String[] ONES = {
            "", "One", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight", "Nine", "Ten",
            "Eleven", "Twelve", "Thirteen", "Fourteen", "Fifteen", "Sixteen", "Seventeen", "Eighteen", "Nineteen"
    };
    private static final String[] TENS = {
            "", "", "Twenty", "Thirty", "Forty", "Fifty", "Sixty", "Seventy", "Eighty", "Ninety"
    };

    private NumberToWordsUtil() {
    }

    public static String rupeesInWords(BigDecimal amount) {
        if (amount == null) amount = BigDecimal.ZERO;
        amount = amount.setScale(2, RoundingMode.HALF_UP);
        long rupees = amount.longValue();
        int paise = amount.subtract(BigDecimal.valueOf(rupees)).movePointRight(2)
                .setScale(0, RoundingMode.HALF_UP).intValue();

        String rupeesWords = rupees == 0 ? "Zero" : convert(rupees);
        StringBuilder sb = new StringBuilder(rupeesWords).append(" Rupees");
        if (paise > 0) {
            sb.append(" and ").append(convert(paise)).append(" Paise");
        }
        return sb.append(" Only").toString();
    }

    private static String convert(long n) {
        if (n == 0) return "";
        if (n < 20) return ONES[(int) n];
        if (n < 100) return TENS[(int) (n / 10)] + (n % 10 != 0 ? " " + ONES[(int) (n % 10)] : "");
        if (n < 1000) return ONES[(int) (n / 100)] + " Hundred" + (n % 100 != 0 ? " " + convert(n % 100) : "");
        if (n < 100_000) return convert(n / 1000) + " Thousand" + (n % 1000 != 0 ? " " + convert(n % 1000) : "");
        if (n < 10_000_000) return convert(n / 100_000) + " Lakh" + (n % 100_000 != 0 ? " " + convert(n % 100_000) : "");
        return convert(n / 10_000_000) + " Crore" + (n % 10_000_000 != 0 ? " " + convert(n % 10_000_000) : "");
    }
}
