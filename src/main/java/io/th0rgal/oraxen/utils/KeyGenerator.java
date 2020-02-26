package io.th0rgal.oraxen.utils;

import java.math.BigInteger;
import java.security.SecureRandom;

public class KeyGenerator {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final char[] STRING_CHARS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890".toCharArray();
    private static final char[] NUMERIC_CHARS = "1234567890".toCharArray();

    public static String generateKey(int size) {
        StringBuilder builder = new StringBuilder();
        for(; size != 0; size--)
            builder.append(STRING_CHARS[SECURE_RANDOM.nextInt(STRING_CHARS.length)]);
        return builder.toString();
    }

    public static BigInteger generateBigInt(int size) {
        StringBuilder builder = new StringBuilder();
        for(; size != 0; size--)
            builder.append(NUMERIC_CHARS[SECURE_RANDOM.nextInt(NUMERIC_CHARS.length)]);
        return new BigInteger(builder.toString());
    }

}
