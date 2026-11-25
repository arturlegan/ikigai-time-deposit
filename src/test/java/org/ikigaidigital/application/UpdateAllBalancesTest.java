package org.ikigaidigital.application;

import org.ikigaidigital.TimeDeposit;
import org.ikigaidigital.TimeDepositCalculator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UpdateAllBalancesTest {

    private final InMemoryTimeDepositRepository repository = new InMemoryTimeDepositRepository();
    private final UpdateAllBalances updateAllBalances = new UpdateAllBalances(repository, new TimeDepositCalculator());

    @Test
    void persistsOneMonthOfInterestForEveryStoredDeposit() {
        repository.add(new TimeDeposit(1, "basic", 1000.00, 31));
        repository.add(new TimeDeposit(2, "student", 1000.00, 31));

        updateAllBalances.execute();

        assertThat(repository.balanceOf(1)).isEqualTo(1000.83);
        assertThat(repository.balanceOf(2)).isEqualTo(1002.50);
    }
}
