package org.ikigaidigital;

/** Basic plan: 1% per year. */
class BasicInterestPolicy extends AnnualRateInterestPolicy {

    BasicInterestPolicy() {
        super(0.01);
    }

    @Override
    boolean earnsInterestOnDay(int days) {
        return true;
    }
}
