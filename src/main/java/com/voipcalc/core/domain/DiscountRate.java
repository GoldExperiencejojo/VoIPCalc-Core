package com.voipcalc.core.domain;

import java.math.BigDecimal;

public record DiscountRate(CustomerType customerType) {

    public BigDecimal applyTo(BigDecimal amount) {
        return amount.multiply(customerType.discountFactor());
    }
}
