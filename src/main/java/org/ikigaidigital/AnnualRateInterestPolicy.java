package org.ikigaidigital;

/**
 * Base for the existing plans: pays one month's share of an annual rate,
 * never during the first 30 days, and only on days the plan-specific {@link #earnsInterestOnDay} allows.
 */
abstract class AnnualRateInterestPolicy implements InterestPolicy {

    private static final int NO_INTEREST_PERIOD_DAYS = 30;
    private static final int MONTHS_PER_YEAR = 12;

    private final double annualRate;

    AnnualRateInterestPolicy(double annualRate) {
        this.annualRate = annualRate;
    }

    @Override
    public final double calculateMonthlyInterest(TimeDeposit deposit) {
        int days = deposit.getDays();
        if (days <= NO_INTEREST_PERIOD_DAYS || !earnsInterestOnDay(days)) {
            return 0;
        }
        // Same operation order as the original implementation (balance * rate / 12) to keep results bit-identical.
        return deposit.getBalance() * annualRate / MONTHS_PER_YEAR;
    }

    /** Plan-specific condition, checked only after the 30-day no-interest period. */
    abstract boolean earnsInterestOnDay(int days);
}
