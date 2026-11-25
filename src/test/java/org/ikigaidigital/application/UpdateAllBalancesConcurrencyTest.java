package org.ikigaidigital.application;

import org.ikigaidigital.TestDatabase;
import org.ikigaidigital.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.NONE;

/**
 * Proves the row lock taken by UpdateAllBalances serialises concurrent executions, so no month of interest is lost.
 * Deterministic: instead of sleeping, it asks PostgreSQL whether the second transaction is waiting for a lock.
 */
@SpringBootTest(webEnvironment = NONE)
@Import(TestcontainersConfiguration.class)
class UpdateAllBalancesConcurrencyTest {

    private static final Duration MAX_WAIT = Duration.ofSeconds(10);

    @Autowired
    private UpdateAllBalances updateAllBalances;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void concurrentBalanceUpdatesAreSerialisedSoNoMonthOfInterestIsLost() throws Exception {
        TestDatabase db = new TestDatabase(jdbc);
        db.clear();
        db.insertDeposit(1, "basic", 31, "1000.00");
        AtomicReference<CompletableFuture<Void>> secondUpdate = new AtomicReference<>();

        // The use case's @Transactional joins this outer transaction, so its row lock is held until the outer commit.
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            updateAllBalances.execute();
            secondUpdate.set(CompletableFuture.runAsync(updateAllBalances::execute));
            awaitBlockedOnLock(secondUpdate.get());
        });

        secondUpdate.get().get(MAX_WAIT.toSeconds(), SECONDS);
        // 1000.00 -> 1000.83 (first) -> 1001.66 (second, reading the committed balance); a lost update leaves 1000.83.
        assertThat(jdbc.queryForObject("select balance from time_deposits where id = 1", BigDecimal.class))
                .isEqualByComparingTo("1001.66");
    }

    private void awaitBlockedOnLock(CompletableFuture<Void> secondUpdate) {
        Instant deadline = Instant.now().plus(MAX_WAIT);
        while (Instant.now().isBefore(deadline)) {
            if (secondUpdate.isDone()) {
                secondUpdate.join(); // rethrows if the second update failed instead of waiting
                fail("Second balance update completed without waiting for the row lock");
            }
            Integer waiting = jdbc.queryForObject(
                    "select count(*) from pg_stat_activity where datname = current_database() and wait_event_type = 'Lock'",
                    Integer.class);
            if (waiting != null && waiting > 0) {
                return;
            }
            sleep(Duration.ofMillis(10));
        }
        fail("Second balance update was not blocked by the row lock within " + MAX_WAIT);
    }

    private static void sleep(Duration duration) {
        try {
            Thread.sleep(duration.toMillis()); // Thread.sleep(Duration) needs Java 19+, target is 17
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }
}
