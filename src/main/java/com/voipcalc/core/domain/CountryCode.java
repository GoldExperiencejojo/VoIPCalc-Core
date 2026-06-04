package com.voipcalc.core.domain;

import java.math.BigDecimal;

public enum CountryCode {
    CHINA("+86", BigDecimal.valueOf(0.10)),
    USA("+1", BigDecimal.valueOf(0.05)),
    OTHER("", BigDecimal.valueOf(0.50));

    private final String prefix;
    private final BigDecimal baseRate;

    CountryCode(String prefix, BigDecimal baseRate) {
        this.prefix = prefix;
        this.baseRate = baseRate;
    }

    public BigDecimal baseRate() {
        return baseRate;
    }

    public static CountryCode fromPrefix(String rawNumber) {
        if (rawNumber.startsWith(CHINA.prefix)) {
            return CHINA;
        }
        if (rawNumber.startsWith(USA.prefix)) {
            return USA;
        }
        return OTHER;
    }
}
