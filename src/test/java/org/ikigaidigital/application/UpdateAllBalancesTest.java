package org.ikigaidigital.application;

import org.ikigaidigital.TimeDeposit;
import org.ikigaidigital.TimeDepositCalculator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

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

    @Test
    void eachExecutionAppliesAnotherMonthOfInterest() {
        repository.add(new TimeDeposit(1, "basic", 1000.00, 31));

        updateAllBalances.execute();
        updateAllBalances.execute();

        // 1000.00 -> 1000.83 -> 1000.83 + round(1000.83 * 0.01 / 12 = 0.834...) = 1001.66.
        // The double sum is 1001.6600000000001; the real adapter stores NUMERIC(19,2), this fake does not.
        assertThat(repository.balanceOf(1)).isCloseTo(1001.66, within(1e-6));
    }
}
