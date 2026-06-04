package com.voipcalc.core.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 针对"夜间低谷福利"子需求的单元测试。
 *
 * <p>业务规则：
 * <ul>
 *   <li>通话时间在 23:00 至次日 05:00 之间为夜间低谷时段</li>
 *   <li>夜间时段单价减免 0.02 元/分钟</li>
 *   <li>减免后单价不低于 0 元</li>
 * </ul>
 */
@DisplayName("夜间低谷福利")
class NightDiscountTest {

    // ────────────────── CallTime.isNightPeriod() ──────────────────

    @Nested
    @DisplayName("通话时间段判断")
    class CallTimeNightPeriod {

        @Test
        @DisplayName("should return false for daytime 12:00")
        void shouldReturnFalseForDaytime12() {
            CallTime callTime = new CallTime(LocalDateTime.of(2026, 6, 4, 12, 0));

            assertThat(callTime.isNightPeriod()).isFalse();
        }

        @Test
        @DisplayName("should return false for evening 22:59")
        void shouldReturnFalseForEvening2259() {
            CallTime callTime = new CallTime(LocalDateTime.of(2026, 6, 4, 22, 59));

            assertThat(callTime.isNightPeriod()).isFalse();
        }

        @Test
        @DisplayName("should return true at boundary 23:00")
        void shouldReturnTrueAtBoundary2300() {
            CallTime callTime = new CallTime(LocalDateTime.of(2026, 6, 4, 23, 0));

            assertThat(callTime.isNightPeriod()).isTrue();
        }

        @Test
        @DisplayName("should return true at midnight 00:00")
        void shouldReturnTrueAtMidnight0000() {
            CallTime callTime = new CallTime(LocalDateTime.of(2026, 6, 5, 0, 0));

            assertThat(callTime.isNightPeriod()).isTrue();
        }

        @Test
        @DisplayName("should return true at 04:59 within night period")
        void shouldReturnTrueAt0459() {
            CallTime callTime = new CallTime(LocalDateTime.of(2026, 6, 5, 4, 59));

            assertThat(callTime.isNightPeriod()).isTrue();
        }

        @Test
        @DisplayName("should return false at boundary 05:00")
        void shouldReturnFalseAtBoundary0500() {
            CallTime callTime = new CallTime(LocalDateTime.of(2026, 6, 5, 5, 0));

            assertThat(callTime.isNightPeriod()).isFalse();
        }
    }

    // ────────────────── NightReduction.apply() ──────────────────

    @Nested
    @DisplayName("夜间减免计算")
    class NightReductionApply {

        @Test
        @DisplayName("should reduce 0.10 by 0.02 resulting in 0.08")
        void shouldReduce010To008() {
            NightReduction reduction = new NightReduction();

            BigDecimal result = reduction.apply(BigDecimal.valueOf(0.10));

            assertThat(result)
                    .usingComparator(BigDecimal::compareTo)
                    .isEqualByComparingTo("0.08");
        }

        @Test
        @DisplayName("should reduce 0.02 by 0.02 resulting in exactly 0.00")
        void shouldReduce002ToExactly000() {
            NightReduction reduction = new NightReduction();

            BigDecimal result = reduction.apply(BigDecimal.valueOf(0.02));

            assertThat(result)
                    .usingComparator(BigDecimal::compareTo)
                    .isEqualByComparingTo("0.00");
        }

        @Test
        @DisplayName("should clamp to 0.00 when reduction goes below zero (0.01 - 0.02)")
        void shouldClampToZeroWhenBelowZero() {
            NightReduction reduction = new NightReduction();

            BigDecimal result = reduction.apply(BigDecimal.valueOf(0.01));

            assertThat(result)
                    .usingComparator(BigDecimal::compareTo)
                    .isEqualByComparingTo("0.00");
        }

        @Test
        @DisplayName("should reduce 0.05 by 0.02 resulting in 0.03")
        void shouldReduce005To003() {
            NightReduction reduction = new NightReduction();

            BigDecimal result = reduction.apply(BigDecimal.valueOf(0.05));

            assertThat(result)
                    .usingComparator(BigDecimal::compareTo)
                    .isEqualByComparingTo("0.03");
        }
    }
}
