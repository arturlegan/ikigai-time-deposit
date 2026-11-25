package org.ikigaidigital.application;

import org.ikigaidigital.TimeDeposit;

import java.util.List;

/** Outbound port for time deposit persistence; implemented by an infrastructure adapter. */
public interface TimeDepositRepository {

    /** Loads all deposits and locks them until the surrounding transaction ends (requires a transaction). */
    List<TimeDeposit> findAllForUpdate();

    List<TimeDepositWithWithdrawals> findAllWithWithdrawals();

    /** Persists the current balance of each given deposit; other fields are not updated. */
    void updateBalances(List<TimeDeposit> deposits);
}
