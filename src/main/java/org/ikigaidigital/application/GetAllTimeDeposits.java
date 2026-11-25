package org.ikigaidigital.application;

import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Returns all time deposits together with their withdrawals. */
public class GetAllTimeDeposits {

    private final TimeDepositRepository repository;

    public GetAllTimeDeposits(TimeDepositRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<TimeDepositWithWithdrawals> execute() {
        return repository.findAllWithWithdrawals();
    }
}
