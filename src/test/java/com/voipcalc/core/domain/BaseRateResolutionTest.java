package com.voipcalc.core.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 针对"基础费率解析"子需求的单元测试。
 * <p>
 * 覆盖两条解析链：
 * <ol>
 *   <li>CalledNumber → CountryCode（被叫号码解析为国家代码）</li>
 *   <li>CountryCode → BaseRate.pricePerMinute()（国家代码映射为基础单价）</li>
 * </ol>
 * 三条业务规则：
 * <ul>
 *   <li>中国（+86）→ ¥0.10/分钟</li>
 *   <li>美国（+1）  → ¥0.05/分钟</li>
 *   <li>其他        → ¥0.50/分钟</li>
 * </ul>
 */
@DisplayName("基础费率解析")
class BaseRateResolutionTest {

    // ────────────────── 第一步：CalledNumber → CountryCode ──────────────────

    @Nested
    @DisplayName("被叫号码解析为国家代码")
    class CalledNumberToCountryCode {

        @Test
        @DisplayName("should resolve CHINA when called number starts with +86")
        void shouldResolveChinaWhenNumberStartsWith86() {
            CalledNumber called = new CalledNumber("+8613800138000");

            assertThat(called.countryCode()).isEqualTo(CountryCode.CHINA);
        }

        @Test
        @DisplayName("should resolve USA when called number starts with +1")
        void shouldResolveUsaWhenNumberStartsWith1() {
            CalledNumber called = new CalledNumber("+12025551234");

            assertThat(called.countryCode()).isEqualTo(CountryCode.USA);
        }

        @Test
        @DisplayName("should resolve OTHER for unknown country prefix like +44")
        void shouldResolveOtherForUnknownPrefix44() {
            CalledNumber called = new CalledNumber("+447911123456");

            assertThat(called.countryCode()).isEqualTo(CountryCode.OTHER);
        }

        @Test
        @DisplayName("should resolve OTHER for any prefix not explicitly mapped")
        void shouldResolveOtherForUnmappedPrefix81() {
            CalledNumber called = new CalledNumber("+81312345678");

            assertThat(called.countryCode()).isEqualTo(CountryCode.OTHER);
        }
    }

    // ────────────────── 第二步：CountryCode → BaseRate ──────────────────

    @Nested
    @DisplayName("国家代码映射为基础单价")
    class CountryCodeToBaseRate {

        @Test
        @DisplayName("should return 0.10 per minute for CHINA")
        void shouldReturn010ForChina() {
            BaseRate rate = new BaseRate(CountryCode.CHINA);

            assertThat(rate.pricePerMinute())
                    .usingComparator(BigDecimal::compareTo)
                    .isEqualTo(BigDecimal.valueOf(0.10));
        }

        @Test
        @DisplayName("should return 0.05 per minute for USA")
        void shouldReturn005ForUsa() {
            BaseRate rate = new BaseRate(CountryCode.USA);

            assertThat(rate.pricePerMinute())
                    .usingComparator(BigDecimal::compareTo)
                    .isEqualByComparingTo("0.05");
        }

        @Test
        @DisplayName("should return 0.50 per minute for OTHER (default)")
        void shouldReturn050ForOther() {
            BaseRate rate = new BaseRate(CountryCode.OTHER);

            assertThat(rate.pricePerMinute())
                    .usingComparator(BigDecimal::compareTo)
                    .isEqualByComparingTo("0.50");
        }
    }

    // ────────────────── 组合验证：CalledNumber → CountryCode → BaseRate ──────────────────

    @Nested
    @DisplayName("组合验证：被叫号码 → 国家代码 → 基础单价")
    class CalledNumberToBaseRate {

        @Test
        @DisplayName("should resolve base rate 0.10 for China number +8613800138000")
        void shouldResolveBaseRate010ForChinaNumber() {
            CalledNumber called = new CalledNumber("+8613800138000");
            BaseRate rate = new BaseRate(called.countryCode());

            assertThat(rate.pricePerMinute())
                    .usingComparator(BigDecimal::compareTo)
                    .isEqualTo(BigDecimal.valueOf(0.10));
        }

        @Test
        @DisplayName("should resolve base rate 0.05 for USA number +12025551234")
        void shouldResolveBaseRate005ForUsaNumber() {
            CalledNumber called = new CalledNumber("+12025551234");
            BaseRate rate = new BaseRate(called.countryCode());

            assertThat(rate.pricePerMinute())
                    .usingComparator(BigDecimal::compareTo)
                    .isEqualByComparingTo("0.05");
        }

        @Test
        @DisplayName("should resolve base rate 0.50 for UK number +447911123456")
        void shouldResolveBaseRate050ForUkNumber() {
            CalledNumber called = new CalledNumber("+447911123456");
            BaseRate rate = new BaseRate(called.countryCode());

            assertThat(rate.pricePerMinute())
                    .usingComparator(BigDecimal::compareTo)
                    .isEqualByComparingTo("0.50");
        }
    }
}
