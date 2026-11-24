package org.ikigaidigital;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class TimeDepositCalculator {

    // To support a new plan: add a PlanType constant, implement InterestPolicy and register it here.
    private final Map<PlanType, InterestPolicy> policies = new EnumMap<>(Map.of(
            PlanType.BASIC, new BasicInterestPolicy(),
            PlanType.STUDENT, new StudentInterestPolicy(),
            PlanType.PREMIUM, new PremiumInterestPolicy()));

    /** Applies one month of interest to every deposit. Deposits with an unknown or missing plan type are left unchanged. */
    public void updateBalance(List<TimeDeposit> deposits) {
        for (TimeDeposit deposit : deposits) {
            deposit.setBalance(deposit.getBalance() + roundToCents(monthlyInterest(deposit)));
        }
    }

    private double monthlyInterest(TimeDeposit deposit) {
        return PlanType.fromCode(deposit.getPlanType())
                .map(planType -> policies.get(planType).calculateMonthlyInterest(deposit))
                .orElse(0.0);
    }

    // new BigDecimal(double) rounds the exact binary value of the interest; kept deliberately to preserve
    // the existing behaviour (BigDecimal.valueOf would round some half-cent cases differently).
    private static double roundToCents(double amount) {
        return new BigDecimal(amount).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}
