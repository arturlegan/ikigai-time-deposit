package org.ikigaidigital;

/** Student plan: 3% per year, no interest after the first year. */
class StudentInterestPolicy extends AnnualRateInterestPolicy {

    private static final int LAST_DAY_WITH_INTEREST = 365;

    StudentInterestPolicy() {
        super(0.03);
    }

    @Override
    boolean earnsInterestOnDay(int days) {
        return days <= LAST_DAY_WITH_INTEREST;
    }
}
