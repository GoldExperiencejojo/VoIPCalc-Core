package com.voipcalc.core.domain;

import java.math.BigDecimal;

public record BaseRate(CountryCode countryCode) {

    public BigDecimal pricePerMinute() {
        return switch (countryCode) {
            case CHINA -> BigDecimal.valueOf(0.10);
            case USA -> BigDecimal.valueOf(0.05);
            case OTHER -> BigDecimal.valueOf(0.50);
        };
    }
}
