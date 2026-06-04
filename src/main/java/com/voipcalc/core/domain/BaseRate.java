package com.voipcalc.core.domain;

import java.math.BigDecimal;

public record BaseRate(CountryCode countryCode) {

    public BigDecimal pricePerMinute() {
        return countryCode.baseRate();
    }
}
