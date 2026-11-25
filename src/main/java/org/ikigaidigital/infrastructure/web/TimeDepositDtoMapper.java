package org.ikigaidigital.infrastructure.web;

import org.ikigaidigital.Withdrawal;
import org.ikigaidigital.application.TimeDepositWithWithdrawals;
import org.ikigaidigital.infrastructure.web.model.TimeDepositDto;
import org.ikigaidigital.infrastructure.web.model.WithdrawalDto;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Maps application read models to the DTOs generated from the OpenAPI contract. */
final class TimeDepositDtoMapper {

    private TimeDepositDtoMapper() {
    }

    static TimeDepositDto toDto(TimeDepositWithWithdrawals source) {
        var deposit = source.deposit();
        return new TimeDepositDto(
                deposit.getId(),
                deposit.getPlanType(),
                toMoney(deposit.getBalance()),
                deposit.getDays(),
                source.withdrawals().stream().map(TimeDepositDtoMapper::toDto).toList());
    }

    private static WithdrawalDto toDto(Withdrawal withdrawal) {
        return new WithdrawalDto(withdrawal.id(), withdrawal.amount(), withdrawal.date());
    }

    // Domain balance is a double; the API exposes money with exactly two decimals (e.g. 1000.0 -> 1000.00).
    private static BigDecimal toMoney(double amount) {
        return BigDecimal.valueOf(amount).setScale(2, RoundingMode.HALF_UP);
    }
}
