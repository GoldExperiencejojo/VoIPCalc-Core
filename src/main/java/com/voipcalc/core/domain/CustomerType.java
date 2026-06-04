package com.voipcalc.core.domain;

import java.math.BigDecimal;

public enum CustomerType {
    VIP(BigDecimal.valueOf(0.9)),
    NORMAL(BigDecimal.valueOf(1.0));

    private final BigDecimal discountFactor;

    CustomerType(BigDecimal discountFactor) {
        this.discountFactor = discountFactor;
    }

    public BigDecimal discountFactor() {
        return discountFactor;
    }
}
