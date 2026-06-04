package com.voipcalc.core.domain;

public record CalledNumber(String rawNumber) {

    public CalledNumber {
        if (rawNumber == null) {
            throw new IllegalArgumentException("rawNumber must not be null");
        }
    }

    public CountryCode countryCode() {
        return CountryCode.fromPrefix(rawNumber);
    }
}
