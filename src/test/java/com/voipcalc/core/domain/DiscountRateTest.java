package com.voipcalc.core.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 针对"客户身份折扣"子需求的单元测试。
 *
 * <p>业务规则：
 * <ul>
 *   <li>VIP（海外留学生/华人卡）：折扣系数 0.9</li>
 *   <li>NORMAL（普通用户）：无折扣，系数 1.0</li>
 * </ul>
 *
 * <p>精度要求：折扣计算必须精确到分（如 0.10 × 0.9 = 0.09）。
 */
@DisplayName("客户身份折扣")
class DiscountRateTest {

    @Nested
    @DisplayName("VIP 客户折扣")
    class VipDiscount {

        @Test
        @DisplayName("should apply 0.9 discount to 0.10 resulting in 0.09")
        void shouldApply09DiscountTo010() {
            DiscountRate rate = new DiscountRate(CustomerType.VIP);

            BigDecimal result = rate.applyTo(BigDecimal.valueOf(0.10));

            assertThat(result)
                    .usingComparator(BigDecimal::compareTo)
                    .isEqualByComparingTo("0.09");
        }

        @Test
        @DisplayName("should apply 0.9 discount to 0.50 resulting in 0.45")
        void shouldApply09DiscountTo050() {
            DiscountRate rate = new DiscountRate(CustomerType.VIP);

            BigDecimal result = rate.applyTo(BigDecimal.valueOf(0.50));

            assertThat(result)
                    .usingComparator(BigDecimal::compareTo)
                    .isEqualByComparingTo("0.45");
        }

        @Test
        @DisplayName("should apply 0.9 discount to 1.00 resulting in 0.90")
        void shouldApply09DiscountTo100() {
            DiscountRate rate = new DiscountRate(CustomerType.VIP);

            BigDecimal result = rate.applyTo(BigDecimal.valueOf(1.00));

            assertThat(result)
                    .usingComparator(BigDecimal::compareTo)
                    .isEqualByComparingTo("0.90");
        }
    }

    @Nested
    @DisplayName("NORMAL 客户无折扣")
    class NormalNoDiscount {

        @Test
        @DisplayName("should return same amount 0.10 for NORMAL customer")
        void shouldReturnSame010ForNormal() {
            DiscountRate rate = new DiscountRate(CustomerType.NORMAL);

            BigDecimal result = rate.applyTo(BigDecimal.valueOf(0.10));

            assertThat(result)
                    .usingComparator(BigDecimal::compareTo)
                    .isEqualByComparingTo("0.10");
        }

        @Test
        @DisplayName("should return same amount 0.50 for NORMAL customer")
        void shouldReturnSame050ForNormal() {
            DiscountRate rate = new DiscountRate(CustomerType.NORMAL);

            BigDecimal result = rate.applyTo(BigDecimal.valueOf(0.50));

            assertThat(result)
                    .usingComparator(BigDecimal::compareTo)
                    .isEqualByComparingTo("0.50");
        }
    }
}
