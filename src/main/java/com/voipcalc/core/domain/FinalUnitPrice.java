package com.voipcalc.core.domain;

import java.math.BigDecimal;

public record FinalUnitPrice(BigDecimal value) {

    public FinalUnitPrice {
        if (value.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("单价不能为负");
        }
    }
}
