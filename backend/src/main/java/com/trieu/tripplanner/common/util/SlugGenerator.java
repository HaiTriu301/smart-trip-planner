package com.trieu.tripplanner.common.util;

import java.security.SecureRandom;
import java.text.Normalizer;
import java.util.Locale;
import java.util.random.RandomGenerator;
import org.springframework.stereotype.Component;

/**
 * Turns a trip title into a URL slug (design.md 5.2 "trips.slug"):
 * "Đà Lạt 3 ngày!" → "da-lat-3-ngay-x7k2qp". The random suffix keeps two trips with the same title apart;
 * uniqueness against the database is checked by the caller.
 */
@Component
public class SlugGenerator {

    static final int MAX_BASE_LENGTH = 150;
    static final int SUFFIX_LENGTH = 6;
    static final String FALLBACK_BASE = "trip";

    private static final String SUFFIX_ALPHABET = "abcdefghijklmnopqrstuvwxyz0123456789";

    private final RandomGenerator random;

    public SlugGenerator() {
        this(new SecureRandom());
    }

    /** Tests pass a seeded generator to get a predictable suffix. */
    SlugGenerator(RandomGenerator random) {
        this.random = random;
    }

    public String generate(String title) {
        return base(title) + "-" + suffix();
    }

    static String base(String title) {
        if (title == null) {
            return FALLBACK_BASE;
        }
        // Đ/đ is a separate letter, not d + combining mark, so NFD would not strip it
        String ascii = Normalizer.normalize(title.replace('Đ', 'D').replace('đ', 'd'), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");
        String slug = ascii.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-+)|(-+$)", "");
        if (slug.length() > MAX_BASE_LENGTH) {
            slug = slug.substring(0, MAX_BASE_LENGTH).replaceAll("-+$", "");
        }
        return slug.isEmpty() ? FALLBACK_BASE : slug;
    }

    private String suffix() {
        StringBuilder sb = new StringBuilder(SUFFIX_LENGTH);
        for (int i = 0; i < SUFFIX_LENGTH; i++) {
            sb.append(SUFFIX_ALPHABET.charAt(random.nextInt(SUFFIX_ALPHABET.length())));
        }
        return sb.toString();
    }

}
