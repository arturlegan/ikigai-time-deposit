package org.ikigaidigital;

/**
 * Calculates the interest a time deposit earns for one month.
 * One implementation per {@link PlanType}, registered in {@link TimeDepositCalculator}.
 */
interface InterestPolicy {

    double calculateMonthlyInterest(TimeDeposit deposit);
}
