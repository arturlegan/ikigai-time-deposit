package org.ikigaidigital;

import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Test data written with plain SQL, independent of the JPA mapping under test. */
public record TestDatabase(JdbcTemplate jdbc) {

    public void clear() {
        jdbc.update("delete from withdrawals");
        jdbc.update("delete from time_deposits");
    }

    public void insertDeposit(int id, String planType, int days, String balance) {
        jdbc.update("insert into time_deposits (id, plan_type, days, balance) values (?, ?, ?, ?)",
                id, planType, days, new BigDecimal(balance));
    }

    public void insertWithdrawal(int id, int timeDepositId, String amount, String date) {
        jdbc.update("insert into withdrawals (id, time_deposit_id, amount, date) values (?, ?, ?, ?)",
                id, timeDepositId, new BigDecimal(amount), LocalDate.parse(date));
    }
}
