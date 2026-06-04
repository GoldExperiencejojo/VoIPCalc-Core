package com.voipcalc.core.domain;

public record CalledNumber(String rawNumber) {

    public CountryCode countryCode() {
        if (rawNumber.startsWith("+86")) {
            return CountryCode.CHINA;
        }
        if (rawNumber.startsWith("+1")) {
            return CountryCode.USA;
        }
        return CountryCode.OTHER;
    }
}
