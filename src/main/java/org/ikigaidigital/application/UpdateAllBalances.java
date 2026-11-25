package org.ikigaidigital.application;

import org.ikigaidigital.TimeDeposit;
import org.ikigaidigital.TimeDepositCalculator;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Applies one month of interest to all time deposits. {@code days} is not advanced: the existing calculator never
 * changes it, so it is assumed to be maintained outside this service.
 */
public class UpdateAllBalances {

    private final TimeDepositRepository repository;
    private final TimeDepositCalculator calculator;

    public UpdateAllBalances(TimeDepositRepository repository, TimeDepositCalculator calculator) {
        this.repository = repository;
        this.calculator = calculator;
    }

    // One transaction: rows are locked on read, so concurrent updates cannot lose a month of interest.
    @Transactional
    public void execute() {
        List<TimeDeposit> deposits = repository.findAllForUpdate();
        calculator.updateBalance(deposits);
        repository.updateBalances(deposits);
    }
}
