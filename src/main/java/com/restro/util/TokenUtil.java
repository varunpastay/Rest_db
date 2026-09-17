package com.restro.util;

import java.security.SecureRandom;

/** Generates opaque hex tokens for table QR links (kept from the original app - no reason to change it). */
public final class TokenUtil {

    private static final SecureRandom RANDOM = new SecureRandom();

    private TokenUtil() {
    }

    public static String generateHexToken(int byteLength) {
        byte[] bytes = new byte[byteLength];
        RANDOM.nextBytes(bytes);
        StringBuilder hex = new StringBuilder(byteLength * 2);
        for (byte b : bytes) {
            hex.append(String.format("%02x", b));
        }
        return hex.toString();
    }
}
