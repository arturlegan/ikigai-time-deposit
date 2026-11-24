package org.ikigaidigital;

/** Premium plan: 5% per year, interest starts after 45 days. */
class PremiumInterestPolicy extends AnnualRateInterestPolicy {

    private static final int INTEREST_STARTS_AFTER_DAYS = 45;

    PremiumInterestPolicy() {
        super(0.05);
    }

    @Override
    boolean earnsInterestOnDay(int days) {
        return days > INTEREST_STARTS_AFTER_DAYS;
    }
}
