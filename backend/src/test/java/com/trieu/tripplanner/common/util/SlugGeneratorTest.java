package com.trieu.tripplanner.common.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Random;
import org.junit.jupiter.api.Test;

class SlugGeneratorTest {

    @Test
    void stripsVietnameseAccentsIncludingDStroke() {
        assertThat(SlugGenerator.base("Đà Lạt 3 ngày!")).isEqualTo("da-lat-3-ngay");
        assertThat(SlugGenerator.base("Phú Quốc – Hòn Thơm")).isEqualTo("phu-quoc-hon-thom");
    }

    @Test
    void collapsesSymbolsAndTrimsDashes() {
        assertThat(SlugGenerator.base("  --Hà   Nội @@ 2026!!  ")).isEqualTo("ha-noi-2026");
    }

    @Test
    void fallsBackWhenNothingUsableRemains() {
        assertThat(SlugGenerator.base("!!! ###")).isEqualTo(SlugGenerator.FALLBACK_BASE);
        assertThat(SlugGenerator.base("東京")).isEqualTo(SlugGenerator.FALLBACK_BASE);
        assertThat(SlugGenerator.base(null)).isEqualTo(SlugGenerator.FALLBACK_BASE);
    }

    @Test
    void cutsLongTitlesWithoutTrailingDash() {
        String base = SlugGenerator.base("ab ".repeat(100));

        assertThat(base.length()).isLessThanOrEqualTo(SlugGenerator.MAX_BASE_LENGTH);
        assertThat(base).doesNotEndWith("-");
    }

    @Test
    void appendsSixCharacterSuffix() {
        String slug = new SlugGenerator().generate("Đà Lạt 3 ngày");

        assertThat(slug).matches("da-lat-3-ngay-[a-z0-9]{6}");
    }

    @Test
    void suffixComesFromTheInjectedRandomSource() {
        String first = new SlugGenerator(new Random(42)).generate("Huế");
        String second = new SlugGenerator(new Random(42)).generate("Huế");

        assertThat(first).isEqualTo(second).startsWith("hue-");
    }

}
