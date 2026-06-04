package com.voipcalc.core.domain;

import java.math.BigDecimal;

public class RateCalculator {

    public FinalUnitPrice calculate(CallContext context) {
        BigDecimal price = new BaseRate(context.calledNumber().countryCode())
                .pricePerMinute();

        price = new DiscountRate(context.customerType())
                .applyTo(price);

        if (context.callTime().isNightPeriod()) {
            price = new NightReduction().apply(price);
        }

        return new FinalUnitPrice(price);
    }
}
