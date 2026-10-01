package com.trieu.tripplanner.common.util;

import java.text.Normalizer;
import java.util.regex.Pattern;

/**
 * Text helpers for Vietnamese. Shared by the trip slug ("Đà Lạt" → "da-lat") and the place search, where
 * "linh ung" must find "Chùa Linh Ứng" (design.md 10.2 "Quy ước Place API").
 */
public final class VietnameseText {

    /** Combining marks left behind once a letter is split from its accents (NFD). */
    private static final Pattern MARKS = Pattern.compile("\\p{M}+");

    private VietnameseText() {
    }

    /**
     * Removes tone and vowel marks, keeping case, spaces and every other character:
     * "Chùa Linh Ứng" → "Chua Linh Ung".
     */
    public static String stripAccents(String text) {
        // Đ/đ is a separate letter, not d + combining mark, so NFD would not strip it
        String decomposed = Normalizer.normalize(text.replace('Đ', 'D').replace('đ', 'd'), Normalizer.Form.NFD);
        return MARKS.matcher(decomposed).replaceAll("");
    }

}
