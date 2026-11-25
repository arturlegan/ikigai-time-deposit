package org.ikigaidigital.application;

import org.ikigaidigital.TimeDeposit;
import org.ikigaidigital.TimeDepositCalculator;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Applies one month of interest to all time deposits. */
public class UpdateAllBalances {

    private final TimeDepositRepository repository;
    private final TimeDepositCalculator calculator;

    public UpdateAllBalances(TimeDepositRepository repository, TimeDepositCalculator calculator) {
        this.repository = repository;
        this.calculator = calculator;
    }

    // One transaction: balances are read and written atomically.
    @Transactional
    public void execute() {
        List<TimeDeposit> deposits = repository.findAll();
        calculator.updateBalance(deposits);
        repository.updateBalances(deposits);
    }
}
