package com.voipcalc.core.domain;

public record CalledNumber(String rawNumber) {

    public CountryCode countryCode() {
        return CountryCode.fromPrefix(rawNumber);
    }
}
