package org.ikigaidigital.infrastructure.persistence;

import jakarta.persistence.EntityManager;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.ikigaidigital.TestDatabase;
import org.ikigaidigital.TestcontainersConfiguration;
import org.ikigaidigital.TimeDeposit;
import org.ikigaidigital.Withdrawal;
import org.ikigaidigital.application.TimeDepositWithWithdrawals;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace.NONE;

/** Runs against real PostgreSQL with the Flyway schema; test data is inserted with plain SQL, independent of the mapping. */
@DataJpaTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@AutoConfigureTestDatabase(replace = NONE)
@Import({TestcontainersConfiguration.class, TimeDepositPersistenceAdapter.class})
class TimeDepositPersistenceAdapterTest {

    @Autowired
    private TimeDepositPersistenceAdapter adapter;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private EntityManager entityManager;

    private TestDatabase db;

    @BeforeEach
    void setUp() {
        db = new TestDatabase(jdbc);
    }

    // Checks the port's unrounded domain values (incl. a fractional balance); the API test sees them only through the
    // web mapper, which rounds to 2 decimals, and uses whole-number balances.
    @Test
    void loadsEveryDepositWithItsOwnWithdrawals() {
        db.insertDeposit(1, "basic", 31, "1000.00");
        db.insertDeposit(2, "student", 400, "250.50");
        db.insertWithdrawal(10, 1, "100.00", "2026-01-15");
        db.insertWithdrawal(11, 1, "50.25", "2026-02-15");

        List<TimeDepositWithWithdrawals> result = adapter.findAllWithWithdrawals();

        assertThat(result)
                .extracting(r -> r.deposit().getId(), r -> r.deposit().getPlanType(),
                        r -> r.deposit().getBalance(), r -> r.deposit().getDays())
                .containsExactly(tuple(1, "basic", 1000.00, 31), tuple(2, "student", 250.50, 400));
        assertThat(result.get(0).withdrawals()).containsExactly(
                new Withdrawal(10, 1, new BigDecimal("100.00"), LocalDate.of(2026, 1, 15)),
                new Withdrawal(11, 1, new BigDecimal("50.25"), LocalDate.of(2026, 2, 15)));
        assertThat(result.get(1).withdrawals()).isEmpty();
    }

    // Deliberately checks the query count: avoiding N+1 loading (one query per deposit) is a design property of this
    // adapter, and the number of statements is its only observable evidence.
    @Test
    void loadsDepositsWithWithdrawalsInTwoQueriesRegardlessOfDepositCount() {
        for (int id = 1; id <= 5; id++) {
            db.insertDeposit(id, "basic", 31, "1000.00");
            db.insertWithdrawal(100 + id, id, "10.00", "2026-01-15");
        }
        Statistics statistics = entityManager.getEntityManagerFactory().unwrap(SessionFactory.class).getStatistics();
        statistics.clear();

        adapter.findAllWithWithdrawals();

        assertThat(statistics.getPrepareStatementCount()).isEqualTo(2);
    }

    @Test
    void persistsUpdatedBalanceRoundedToCentsAndLeavesOtherColumnsUnchanged() {
        db.insertDeposit(1, "basic", 31, "1000.83");
        List<TimeDeposit> deposits = adapter.findAllForUpdate();
        assertThat(deposits).singleElement().extracting(TimeDeposit::getBalance).isEqualTo(1000.83);

        deposits.get(0).setBalance(1001.6600000000001); // double artefact of two monthly updates
        adapter.updateBalances(deposits);
        entityManager.flush();

        Map<String, Object> row = jdbc.queryForMap("select plan_type, days, balance from time_deposits where id = 1");
        assertThat(row).containsEntry("plan_type", "basic").containsEntry("days", 31)
                .containsEntry("balance", new BigDecimal("1001.66"));
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void refusesToRunOutsideATransaction() {
        assertThatThrownBy(() -> adapter.updateBalances(List.of()))
                .isInstanceOf(IllegalTransactionStateException.class);
    }
}
