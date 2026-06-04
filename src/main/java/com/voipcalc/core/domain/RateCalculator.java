package com.voipcalc.core.domain;

import java.math.BigDecimal;

public class RateCalculator {

    public FinalUnitPrice calculate(CallContext context) {
        BaseRate baseRate = new BaseRate(context.calledNumber().countryCode());
        BigDecimal price = baseRate.pricePerMinute();

        DiscountRate discountRate = new DiscountRate(context.customerType());
        price = discountRate.applyTo(price);

        if (context.callTime().isNightPeriod()) {
            NightReduction reduction = new NightReduction();
            price = reduction.apply(price);
        }

        return new FinalUnitPrice(price);
    }
}
