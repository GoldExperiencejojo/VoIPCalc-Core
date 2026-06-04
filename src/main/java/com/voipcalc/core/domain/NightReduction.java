package com.voipcalc.core.domain;

import java.math.BigDecimal;

public record NightReduction() {

    private static final BigDecimal REDUCTION = BigDecimal.valueOf(0.02);

    public BigDecimal apply(BigDecimal price) {
        return price.subtract(REDUCTION).max(BigDecimal.ZERO);
    }
}
