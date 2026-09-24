package com.trieu.tripplanner.common.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Opaque one-time tokens handed to clients (refresh token, email verification, password reset).
 * The client keeps the raw value; the database only ever stores {@link #sha256Hex(String)} of it,
 * so a leaked table cannot be replayed (design.md 5.2, 6.1).
 */
public final class SecureTokens {

    /** 48 random bytes → 64 base64url characters, no padding. */
    private static final int RAW_TOKEN_BYTES = 48;
    private static final SecureRandom RANDOM = new SecureRandom();

    private SecureTokens() {
    }

    /** URL-safe random token of exactly 64 characters. */
    public static String generate() {
        byte[] bytes = new byte[RAW_TOKEN_BYTES];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /** Deterministic SHA-256 of the raw token as 64 lowercase hex chars, the form stored in *_hash columns. */
    public static String sha256Hex(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(rawToken.getBytes(StandardCharsets.UTF_8)));
        }
        catch (NoSuchAlgorithmException ex) {
            // SHA-256 is mandatory in every JVM; reaching here means a broken runtime, not a business error
            throw new IllegalStateException("SHA-256 not available", ex);
        }
    }

}
