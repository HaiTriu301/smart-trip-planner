package com.trieu.tripplanner.common.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.text.Normalizer;
import org.junit.jupiter.api.Test;

class VietnameseTextTest {

    @Test
    void removesToneAndVowelMarksButKeepsCaseAndSpaces() {
        assertThat(VietnameseText.stripAccents("Chùa Linh Ứng")).isEqualTo("Chua Linh Ung");
        assertThat(VietnameseText.stripAccents("Bún chả cá 109 Nguyễn Chí Thanh")).isEqualTo("Bun cha ca 109 Nguyen Chi Thanh");
        assertThat(VietnameseText.stripAccents("ăâêôơư ẰẤỂỖỢỰ")).isEqualTo("aaeoou AAEOOU");
    }

    @Test
    void replacesDStrokeWhichIsALetterOfItsOwn() {
        // Đ is not "D + mark": Unicode decomposition alone leaves it untouched
        assertThat(VietnameseText.stripAccents("Đà Nẵng đẹp")).isEqualTo("Da Nang dep");
    }

    @Test
    void sameResultWhetherAccentsArriveComposedOrDecomposed() {
        // A keyboard or a copy-paste may send "à" as one code point or as "a" + combining grave
        String composed = Normalizer.normalize("Hà Nội", Normalizer.Form.NFC);
        String decomposed = Normalizer.normalize("Hà Nội", Normalizer.Form.NFD);

        assertThat(composed).isNotEqualTo(decomposed);
        assertThat(VietnameseText.stripAccents(composed)).isEqualTo("Ha Noi");
        assertThat(VietnameseText.stripAccents(decomposed)).isEqualTo("Ha Noi");
    }

    @Test
    void leavesTextWithoutAccentsAsItIs() {
        assertThat(VietnameseText.stripAccents("Hotel 5* - Wi-Fi!")).isEqualTo("Hotel 5* - Wi-Fi!");
        assertThat(VietnameseText.stripAccents("東京")).isEqualTo("東京");
        assertThat(VietnameseText.stripAccents("")).isEmpty();
    }

}
