package org.ikigaidigital.application;

import org.ikigaidigital.TimeDeposit;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Test fake. Stores and returns copies - like a real database - so a use case that mutates
 * deposits but forgets to persist them is caught.
 */
class InMemoryTimeDepositRepository implements TimeDepositRepository {

    private final Map<Integer, TimeDeposit> deposits = new LinkedHashMap<>();

    void add(TimeDeposit deposit) {
        deposits.put(deposit.getId(), copy(deposit));
    }

    double balanceOf(int id) {
        return deposits.get(id).getBalance();
    }

    @Override
    public List<TimeDeposit> findAllForUpdate() {
        return deposits.values().stream().map(InMemoryTimeDepositRepository::copy).toList();
    }

    @Override
    public List<TimeDepositWithWithdrawals> findAllWithWithdrawals() {
        throw new UnsupportedOperationException("not needed by the tests using this fake");
    }

    @Override
    public void updateBalances(List<TimeDeposit> updated) {
        updated.forEach(deposit -> deposits.get(deposit.getId()).setBalance(deposit.getBalance()));
    }

    private static TimeDeposit copy(TimeDeposit deposit) {
        return new TimeDeposit(deposit.getId(), deposit.getPlanType(), deposit.getBalance(), deposit.getDays());
    }
}
