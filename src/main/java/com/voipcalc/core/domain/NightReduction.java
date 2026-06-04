package com.voipcalc.core.domain;

import java.math.BigDecimal;

public record NightReduction() {

    private static final BigDecimal REDUCTION = BigDecimal.valueOf(0.02);

    public BigDecimal apply(BigDecimal price) {
        BigDecimal reduced = price.subtract(REDUCTION);
        if (reduced.compareTo(BigDecimal.ZERO) < 0) {
            return BigDecimal.ZERO;
        }
        return reduced;
    }
}
