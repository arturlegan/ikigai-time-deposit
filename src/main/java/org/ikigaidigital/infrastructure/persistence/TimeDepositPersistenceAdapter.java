package org.ikigaidigital.infrastructure.persistence;

import org.ikigaidigital.TimeDeposit;
import org.ikigaidigital.Withdrawal;
import org.ikigaidigital.application.TimeDepositRepository;
import org.ikigaidigital.application.TimeDepositWithWithdrawals;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toMap;

// MANDATORY: transactions are owned by the use cases. Without one, the locking query would fail, but updateBalances
// would silently persist nothing (findAllById returns detached entities). Read-only transactions pass this guard:
// PostgreSQL rejects the locking query, but updateBalances alone would still silently persist nothing.
@Component
@Transactional(propagation = Propagation.MANDATORY)
class TimeDepositPersistenceAdapter implements TimeDepositRepository {

    private static final Sort BY_ID = Sort.by("id");

    private final TimeDepositJpaRepository deposits;
    private final WithdrawalJpaRepository withdrawals;

    TimeDepositPersistenceAdapter(TimeDepositJpaRepository deposits, WithdrawalJpaRepository withdrawals) {
        this.deposits = deposits;
        this.withdrawals = withdrawals;
    }

    @Override
    public List<TimeDeposit> findAllForUpdate() {
        return deposits.findAllForUpdate().stream().map(TimeDepositPersistenceAdapter::toDomain).toList();
    }

    // Two queries in total (deposits + all withdrawals grouped in memory) instead of one per deposit.
    @Override
    public List<TimeDepositWithWithdrawals> findAllWithWithdrawals() {
        Map<Integer, List<Withdrawal>> withdrawalsByDeposit = withdrawals.findAll(BY_ID).stream()
                .map(TimeDepositPersistenceAdapter::toDomain)
                .collect(groupingBy(Withdrawal::timeDepositId));
        return deposits.findAll(BY_ID).stream()
                .map(entity -> new TimeDepositWithWithdrawals(
                        toDomain(entity), withdrawalsByDeposit.getOrDefault(entity.getId(), List.of())))
                .toList();
    }

    // Entities are already managed when called in the same transaction as findAllForUpdate;
    // changed balances are written by dirty checking on commit.
    @Override
    public void updateBalances(List<TimeDeposit> updated) {
        Map<Integer, TimeDepositEntity> entities = deposits.findAllById(updated.stream().map(TimeDeposit::getId).toList())
                .stream().collect(toMap(TimeDepositEntity::getId, Function.identity()));
        updated.forEach(deposit -> entities.get(deposit.getId()).setBalance(toMoney(deposit.getBalance())));
    }

    private static TimeDeposit toDomain(TimeDepositEntity entity) {
        return new TimeDeposit(entity.getId(), entity.getPlanType(), entity.getBalance().doubleValue(), entity.getDays());
    }

    private static Withdrawal toDomain(WithdrawalEntity entity) {
        return new Withdrawal(entity.getId(), entity.getTimeDepositId(), entity.getAmount(), entity.getDate());
    }

    // Rounds explicitly to the column's scale (NUMERIC(19,2)) instead of relying on the database's implicit rounding;
    // valueOf uses the double's shortest decimal form (1001.6600000000001 -> 1001.66), unlike new BigDecimal(double).
    private static BigDecimal toMoney(double amount) {
        return BigDecimal.valueOf(amount).setScale(2, RoundingMode.HALF_UP);
    }
}
