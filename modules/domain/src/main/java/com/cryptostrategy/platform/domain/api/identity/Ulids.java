package com.cryptostrategy.platform.domain.api.identity;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.time.Clock;
import java.util.Objects;
import java.util.regex.Pattern;

/** Shared validation and generation for typed domain ULID values. */
public final class Ulids {
    private static final Pattern FORMAT = Pattern.compile("^[0-7][0-9A-HJKMNP-TV-Z]{25}$");
    private static final char[] ALPHABET = "0123456789ABCDEFGHJKMNPQRSTVWXYZ".toCharArray();
    private static final SecureRandom RANDOM = new SecureRandom();

    private Ulids() {
    }

    public static String requireValid(String value) {
        Objects.requireNonNull(value, "ULID");
        if (!FORMAT.matcher(value).matches()) {
            throw new IllegalArgumentException("Business ID must be a canonical uppercase Crockford ULID");
        }
        return value;
    }

    public static String generate() {
        byte[] bytes = new byte[16];
        long timestamp = Clock.systemUTC().millis();
        for (int index = 5; index >= 0; index--) {
            bytes[index] = (byte) timestamp;
            timestamp >>>= 8;
        }
        byte[] random = new byte[10];
        RANDOM.nextBytes(random);
        System.arraycopy(random, 0, bytes, 6, random.length);
        BigInteger number = new BigInteger(1, bytes);
        char[] encoded = new char[26];
        BigInteger mask = BigInteger.valueOf(31);
        for (int index = encoded.length - 1; index >= 0; index--) {
            encoded[index] = ALPHABET[number.and(mask).intValue()];
            number = number.shiftRight(5);
        }
        return new String(encoded);
    }
}
