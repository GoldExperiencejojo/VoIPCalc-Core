package com.voipcalc.core.domain;

import java.time.LocalDateTime;

public record CallTime(LocalDateTime timestamp) {

    private static final int NIGHT_START_HOUR = 23;
    private static final int NIGHT_END_HOUR = 5;

    public CallTime {
        if (timestamp == null) {
            throw new IllegalArgumentException("timestamp must not be null");
        }
    }

    public boolean isNightPeriod() {
        int hour = timestamp.getHour();
        return hour >= NIGHT_START_HOUR || hour < NIGHT_END_HOUR;
    }
}
