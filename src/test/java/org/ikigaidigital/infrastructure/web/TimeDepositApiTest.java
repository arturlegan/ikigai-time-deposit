package org.ikigaidigital.infrastructure.web;

import org.ikigaidigital.TestDatabase;
import org.ikigaidigital.TestcontainersConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

/** End-to-end over real HTTP: controller, use cases, transactions, persistence and PostgreSQL. */
@SpringBootTest(webEnvironment = RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
class TimeDepositApiTest {

    @LocalServerPort
    private int port;

    @Autowired
    private JdbcTemplate jdbc;

    private RestClient client;
    private TestDatabase db;

    @BeforeEach
    void setUp() {
        client = RestClient.create("http://localhost:" + port);
        db = new TestDatabase(jdbc);
        db.clear();
    }

    @Test
    void listsAllDepositsWithTheirWithdrawalsInTheContractShape() throws Exception {
        db.insertDeposit(1, "basic", 45, "1234567.00");
        db.insertDeposit(2, "gold", 100, "750.00");
        db.insertWithdrawal(10, 1, "1000.00", "2026-01-15");
        db.insertWithdrawal(11, 1, "250.50", "2026-02-15");

        String body = client.get().uri("/time-deposits").retrieve().body(String.class);

        JSONAssert.assertEquals("""
                [
                  {"id": 1, "planType": "basic", "balance": 1234567.00, "days": 45,
                   "withdrawals": [{"id": 10, "amount": 1000.00, "date": "2026-01-15"},
                                   {"id": 11, "amount": 250.50,  "date": "2026-02-15"}]},
                  {"id": 2, "planType": "gold", "balance": 750.00, "days": 100, "withdrawals": []}
                ]
                """, body, JSONCompareMode.STRICT);
    }

    @Test
    void listsNoDepositsAsAnEmptyArray() throws Exception {
        String body = client.get().uri("/time-deposits").retrieve().body(String.class);

        JSONAssert.assertEquals("[]", body, JSONCompareMode.STRICT);
    }

    @Test
    void balanceUpdateAppliesOneMonthOfInterestToEveryDepositAndReturnsNoContent() {
        db.insertDeposit(1, "basic", 45, "1234567.00");
        db.insertDeposit(2, "student", 366, "5000.00");
        db.insertDeposit(3, "gold", 100, "750.00");

        ResponseEntity<Void> response = client.post().uri("/time-deposits/balance-updates").retrieve().toBodilessEntity();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(jdbc.queryForList("select balance from time_deposits order by id", BigDecimal.class))
                .containsExactly(new BigDecimal("1235595.81"), new BigDecimal("5000.00"), new BigDecimal("750.00"));
    }
}
