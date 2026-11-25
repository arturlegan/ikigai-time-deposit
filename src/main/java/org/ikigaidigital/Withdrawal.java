package org.ikigaidigital;

import java.math.BigDecimal;
import java.time.LocalDate;

/** A withdrawal from a time deposit. New domain type, so money is BigDecimal (unlike the shared TimeDeposit). */
public record Withdrawal(int id, int timeDepositId, BigDecimal amount, LocalDate date) {
}
