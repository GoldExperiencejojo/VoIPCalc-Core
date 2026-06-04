package com.voipcalc.core.domain;

public record CallContext(
        CalledNumber calledNumber,
        CustomerType customerType,
        CallTime callTime
) {}
