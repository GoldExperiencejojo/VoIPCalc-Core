package com.voipcalc.core.domain;

import java.time.LocalDateTime;

public record CallTime(LocalDateTime timestamp) {

    public boolean isNightPeriod() {
        int hour = timestamp.getHour();
        return hour >= 23 || hour < 5;
    }
}
