package org.ikigaidigital.application;

import org.ikigaidigital.TimeDeposit;
import org.ikigaidigital.Withdrawal;

import java.util.List;

/** Read model: TimeDeposit is shared and must not change, so its withdrawals are carried alongside it. */
public record TimeDepositWithWithdrawals(TimeDeposit deposit, List<Withdrawal> withdrawals) {

    public TimeDepositWithWithdrawals {
        withdrawals = List.copyOf(withdrawals);
    }
}
