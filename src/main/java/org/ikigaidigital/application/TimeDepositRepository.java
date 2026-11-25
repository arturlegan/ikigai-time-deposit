package org.ikigaidigital.application;

import org.ikigaidigital.TimeDeposit;

import java.util.List;

/** Outbound port for time deposit persistence; implemented by an infrastructure adapter. */
public interface TimeDepositRepository {

    List<TimeDeposit> findAll();

    List<TimeDepositWithWithdrawals> findAllWithWithdrawals();

    /** Persists the current balance of each given deposit; other fields are not updated. */
    void updateBalances(List<TimeDeposit> deposits);
}
