package com.voipcalc.core.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 针对"完整叠加逻辑"子需求的集成测试——RateCalculator 规则链规约。
 *
 * <p>规则链顺序（固定不可颠倒）：
 * <ol>
 *   <li>基础费率 → 客户折扣 → 夜间减免 → FinalUnitPrice</li>
 * </ol>
 *
 * <p>覆盖：不同国家 × 不同身份 × 白天/夜间 的组合场景。
 */
@DisplayName("完整叠加逻辑：RateCalculator 规则链")
class RateCalculatorIntegrationTest {

    private static final LocalDateTime DAYTIME = LocalDateTime.of(2026, 6, 4, 12, 0);
    private static final LocalDateTime NIGHT_2300 = LocalDateTime.of(2026, 6, 4, 23, 0);
    private static final LocalDateTime NIGHT_0000 = LocalDateTime.of(2026, 6, 5, 0, 0);
    private static final LocalDateTime NIGHT_0430 = LocalDateTime.of(2026, 6, 5, 4, 30);

    private final RateCalculator calculator = new RateCalculator();

    @Nested
    @DisplayName("白天时段（无夜间减免）")
    class DaytimeScenarios {

        @Test
        @DisplayName("China +86 NORMAL daytime → 0.10")
        void shouldReturn010ForChinaNormalDaytime() {
            CallContext context = new CallContext(
                    new CalledNumber("+8613800138000"),
                    CustomerType.NORMAL,
                    new CallTime(DAYTIME)
            );

            FinalUnitPrice result = calculator.calculate(context);

            assertThat(result.value())
                    .usingComparator(BigDecimal::compareTo)
                    .isEqualByComparingTo("0.10");
        }

        @Test
        @DisplayName("USA +1 VIP daytime → 0.045 (0.05 × 0.9)")
        void shouldReturn0045ForUsaVipDaytime() {
            CallContext context = new CallContext(
                    new CalledNumber("+12025551234"),
                    CustomerType.VIP,
                    new CallTime(DAYTIME)
            );

            FinalUnitPrice result = calculator.calculate(context);

            assertThat(result.value())
                    .usingComparator(BigDecimal::compareTo)
                    .isEqualByComparingTo("0.045");
        }

        @Test
        @DisplayName("Other +44 NORMAL daytime → 0.50")
        void shouldReturn050ForOtherNormalDaytime() {
            CallContext context = new CallContext(
                    new CalledNumber("+447911123456"),
                    CustomerType.NORMAL,
                    new CallTime(DAYTIME)
            );

            FinalUnitPrice result = calculator.calculate(context);

            assertThat(result.value())
                    .usingComparator(BigDecimal::compareTo)
                    .isEqualByComparingTo("0.50");
        }
    }

    @Nested
    @DisplayName("夜间时段（含夜间减免 -0.02）")
    class NighttimeScenarios {

        @Test
        @DisplayName("China +86 NORMAL night 23:00 → 0.08 (0.10 - 0.02)")
        void shouldReturn008ForChinaNormalNight() {
            CallContext context = new CallContext(
                    new CalledNumber("+8613800138000"),
                    CustomerType.NORMAL,
                    new CallTime(NIGHT_2300)
            );

            FinalUnitPrice result = calculator.calculate(context);

            assertThat(result.value())
                    .usingComparator(BigDecimal::compareTo)
                    .isEqualByComparingTo("0.08");
        }

        @Test
        @DisplayName("Other +44 VIP night 00:00 → 0.43 (0.50×0.9 - 0.02)")
        void shouldReturn043ForOtherVipNight() {
            CallContext context = new CallContext(
                    new CalledNumber("+447911123456"),
                    CustomerType.VIP,
                    new CallTime(NIGHT_0000)
            );

            FinalUnitPrice result = calculator.calculate(context);

            assertThat(result.value())
                    .usingComparator(BigDecimal::compareTo)
                    .isEqualByComparingTo("0.43");
        }

        @Test
        @DisplayName("USA +1 VIP night 04:30 → 0.025 (0.05×0.9 - 0.02, smallest non-zero)")
        void shouldReturn0025ForUsaVipNight() {
            CallContext context = new CallContext(
                    new CalledNumber("+12025551234"),
                    CustomerType.VIP,
                    new CallTime(NIGHT_0430)
            );

            FinalUnitPrice result = calculator.calculate(context);

            assertThat(result.value())
                    .usingComparator(BigDecimal::compareTo)
                    .isEqualByComparingTo("0.025");
        }
    }

    @Nested
    @DisplayName("边界场景")
    class BoundaryScenarios {

        @Test
        @DisplayName("China +86 VIP daytime → 0.09 (0.10 × 0.9)")
        void shouldReturn009ForChinaVipDaytime() {
            CallContext context = new CallContext(
                    new CalledNumber("+8613800138000"),
                    CustomerType.VIP,
                    new CallTime(DAYTIME)
            );

            FinalUnitPrice result = calculator.calculate(context);

            assertThat(result.value())
                    .usingComparator(BigDecimal::compareTo)
                    .isEqualByComparingTo("0.09");
        }

        @Test
        @DisplayName("USA +1 NORMAL night 23:00 → 0.03 (0.05 - 0.02)")
        void shouldReturn003ForUsaNormalNight() {
            CallContext context = new CallContext(
                    new CalledNumber("+12025551234"),
                    CustomerType.NORMAL,
                    new CallTime(NIGHT_2300)
            );

            FinalUnitPrice result = calculator.calculate(context);

            assertThat(result.value())
                    .usingComparator(BigDecimal::compareTo)
                    .isEqualByComparingTo("0.03");
        }
    }
}
